package org.classroom.soundlevel;

import android.Manifest;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.runtime.*;
import org.classroom.soundlevel.helpers.LevelMath;
import org.classroom.soundlevel.helpers.SpectrumAnalyzer;
import org.classroom.soundlevel.helpers.SpectrumColors;

@DesignerComponent(version = 4, description = "Microphone loudness and music spectrum colors with RGB and purple-pink Concert themes. No audio files are saved. Android only.",
    category = ComponentCategory.EXTENSION, nonVisible = true, iconName = "")
@SimpleObject(external = true)
@UsesPermissions(permissionNames = "android.permission.RECORD_AUDIO")
public final class SoundLevel extends AndroidNonvisibleComponent
    implements OnPauseListener, OnStopListener, OnDestroyListener, OnClearListener, Deleteable {
  private final Handler ui = new Handler(Looper.getMainLooper());
  // Lifecycle and permission state belongs exclusively to the UI thread.
  private Session session;
  private boolean desired;
  private boolean permissionPending;
  private boolean disposed;
  private volatile int interval = 100;
  private volatile double smoothing = 0.6;
  private volatile double sensitivity = 1.0;
  private int level;
  private volatile int noiseGate = 20;
  private volatile AnalysisConfig analysis = new AnalysisConfig(false);
  private boolean concertTheme;
  private int silenceColor = 0xff000000;

  private static final class AnalysisConfig {
    final boolean spectrum;
    AnalysisConfig(boolean spectrum) { this.spectrum = spectrum; }
  }

  private static final class Session {
    volatile boolean cancelled;
    AudioRecord recorder; // guarded by this Session's monitor
  }

  public SoundLevel(ComponentContainer container) {
    super(container.$form());
    form.registerForOnPause(this);
    form.registerForOnStop(this);
    form.registerForOnDestroy(this);
    form.registerForOnClear(this);
  }

  @SimpleFunction(description = "Request microphone permission, then start measuring. Use a button click.")
  public void Start() {
    if (disposed) return;
    desired = true;
    if (session != null || permissionPending) return;
    permissionPending = true;
    form.askPermission(Manifest.permission.RECORD_AUDIO, new PermissionResultHandler() {
      @Override public void HandlePermissionResponse(String permission, boolean granted) {
        permissionPending = false;
        if (disposed || !desired) return;
        if (!granted) {
          desired = false;
          Error("Microphone permission denied. Allow Microphone for MIT AI2 Companion in Android Settings, then press Start.");
          return;
        }
        begin();
      }
    });
  }

  private void begin() {
    if (disposed || !desired || session != null) return;
    final Session current = new Session();
    session = current;
    new Thread(new Runnable() {
      @Override public void run() { measure(current); }
    }, "SoundLevel-Microphone").start();
  }

  @SimpleFunction(description = "Stop measuring and release the microphone. Also cancels a pending Start.")
  public void Stop() {
    desired = false;
    level = 0;
    Session current = session;
    if (current != null) {
      current.cancelled = true;
      // stop() unblocks a blocking read; release() belongs to the worker's finally block.
      synchronized (current) {
        if (current.recorder != null) {
          try { current.recorder.stop(); } catch (RuntimeException ignored) { }
        }
      }
    }
  }

  private void measure(final Session current) {
    AudioRecord audio = null;
    String failure = null;
    try {
      int minimum = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO,
          AudioFormat.ENCODING_PCM_16BIT);
      if (minimum <= 0) throw new IllegalStateException("16 kHz microphone input is unavailable on this device.");
      synchronized (current) {
        if (current.cancelled) return;
        audio = new AudioRecord(MediaRecorder.AudioSource.MIC, 16000,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, Math.max(minimum, 8192));
        current.recorder = audio;
        if (audio.getState() != AudioRecord.STATE_INITIALIZED)
          throw new IllegalStateException("Could not initialize microphone. Close other apps using it.");
        audio.startRecording();
        if (audio.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING)
          throw new IllegalStateException("Microphone did not start recording.");
      }
      ui.post(new Runnable() {
        @Override public void run() {
          if (!disposed && session == current && !current.cancelled) Started();
        }
      });
      short[] samples = new short[1024];
      double squares = 0;
      long count = 0;
      long last = SystemClock.elapsedRealtime();
      double filtered = 0;
      boolean first = true;
      final SpectrumAnalyzer spectrum = new SpectrumAnalyzer();
      AnalysisConfig activeAnalysis = analysis;
      while (!current.cancelled) {
        int size = audio.read(samples, 0, samples.length);
        if (current.cancelled) break;
        if (size <= 0) throw new IllegalStateException("Microphone read failed (" + size + ").");
        if (activeAnalysis != analysis) {
          activeAnalysis = analysis;
          spectrum.reset();
          squares = 0;
          count = 0;
          filtered = 0;
          first = true;
          last = SystemClock.elapsedRealtime();
        }
        if (activeAnalysis.spectrum) spectrum.push(samples, size);
        for (int i = 0; i < size; i++) squares += (double) samples[i] * samples[i];
        count += size;
        long now = SystemClock.elapsedRealtime();
        if (now - last >= interval) {
          double raw = LevelMath.level(squares, count, sensitivity);
          double retention = smoothing;
          filtered = first ? raw : retention * filtered + (1 - retention) * raw;
          first = false;
          final int value = (int) Math.round(filtered);
          final AnalysisConfig emittedAnalysis = activeAnalysis;
          final SpectrumAnalyzer.Result bands = activeAnalysis.spectrum
              ? spectrum.poll(raw <= noiseGate, retention) : null;
          ui.post(new Runnable() {
            @Override public void run() {
              if (!disposed && session == current && !current.cancelled && analysis == emittedAnalysis) {
                level = value;
                LevelChanged(value);
                // A LevelChanged handler may stop or remove this component.
                if (!disposed && session == current && !current.cancelled && analysis == emittedAnalysis) {
                  if (emittedAnalysis.spectrum) {
                    if (bands != null) {
                      int color = SpectrumColors.color(bands, concertTheme, silenceColor);
                      int red = (color >> 16) & 255;
                      int green = (color >> 8) & 255;
                      int blue = color & 255;
                      SpectrumChanged(bands.low, bands.mid, bands.high, red, green, blue, color);
                      if (!disposed && session == current && !current.cancelled && analysis == emittedAnalysis)
                        ColorChanged(red, green, blue, color);
                    }
                  } else {
                    int color = LevelMath.color(value);
                    ColorChanged((color >> 16) & 255, (color >> 8) & 255, color & 255, color);
                  }
                }
              }
            }
          });
          squares = 0;
          count = 0;
          last = now;
        }
      }
    } catch (RuntimeException error) {
      if (!current.cancelled) failure = error.getClass().getSimpleName() + ": " + error.getMessage();
    } finally {
      synchronized (current) {
        if (audio != null) {
          try { audio.stop(); } catch (RuntimeException ignored) { }
          audio.release();
        }
        current.recorder = null;
      }
      final String message = failure;
      ui.post(new Runnable() {
        @Override public void run() {
          if (session != current) return;
          session = null;
          level = 0;
          if (disposed) return;
          // Permit Start immediately after Stop, but never overlap two recorders.
          boolean restart = desired && current.cancelled;
          if (message != null) desired = false;
          Stopped();
          if (message != null) Error(message);
          if (restart && desired && !disposed) Start();
        }
      });
    }
  }

  @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Latest relative loudness, 0-100. This is not calibrated dB SPL.")
  public int Level() { return level; }

  @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Loudness (default) or Spectrum. Spectrum maps 80-300 Hz to red, 300-2000 Hz to green and 2000-8000 Hz to blue.")
  public String AnalysisMode() { return analysis.spectrum ? "Spectrum" : "Loudness"; }
  @DesignerProperty(editorType = "string", defaultValue = "Loudness")
  @SimpleProperty(category = PropertyCategory.BEHAVIOR) public void AnalysisMode(String value) {
    String mode = value == null ? "" : value.trim();
    if (!"Loudness".equalsIgnoreCase(mode) && !"Spectrum".equalsIgnoreCase(mode)) {
      Error("AnalysisMode must be Loudness or Spectrum.");
      return;
    }
    boolean spectrum = "Spectrum".equalsIgnoreCase(mode);
    if (analysis.spectrum != spectrum) analysis = new AnalysisConfig(spectrum);
  }

  @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Spectrum silence threshold in relative Level units (0-100). Raw level at or below this value produces SilenceColor. Raise to reject room noise.")
  public int NoiseGate() { return noiseGate; }
  @DesignerProperty(editorType = "integer", defaultValue = "20")
  @SimpleProperty(category = PropertyCategory.BEHAVIOR) public void NoiseGate(int value) {
    noiseGate = Math.max(0, Math.min(100, value));
  }

  @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Spectrum palette: RGB (default) or Concert (purple, pink, lavender). Does not change low/mid/high measurements. Loudness mode is unaffected.")
  public String ColorTheme() { return concertTheme ? "Concert" : "RGB"; }
  @DesignerProperty(editorType = "string", defaultValue = "RGB")
  @SimpleProperty(category = PropertyCategory.BEHAVIOR) public void ColorTheme(String value) {
    String theme = value == null ? "" : value.trim();
    if (!"RGB".equalsIgnoreCase(theme) && !"Concert".equalsIgnoreCase(theme)) {
      Error("ColorTheme must be RGB or Concert.");
      return;
    }
    concertTheme = "Concert".equalsIgnoreCase(theme);
  }

  @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Spectrum mode color below NoiseGate or with no in-band energy. Default black. Set a dark purple for Concert. Always opaque; low/mid/high remain zero during silence.")
  @IsColor public int SilenceColor() { return silenceColor; }
  @DesignerProperty(editorType = "color", defaultValue = "&HFF000000")
  @SimpleProperty(category = PropertyCategory.BEHAVIOR) public void SilenceColor(@IsColor int value) {
    silenceColor = value | 0xff000000;
  }

  @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Whether microphone capture has started.")
  public boolean IsListening() {
    Session current = session;
    if (current == null || current.cancelled) return false;
    synchronized (current) {
      return current.recorder != null
          && current.recorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING;
    }
  }

  @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Event interval in milliseconds (50-2000). Actual timing depends on the device.")
  public int Interval() { return interval; }
  @DesignerProperty(editorType = "integer", defaultValue = "100")
  @SimpleProperty(category = PropertyCategory.BEHAVIOR) public void Interval(int value) { interval = Math.max(50, Math.min(2000, value)); }

  @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Smoothing 0-0.95. Higher values reduce flicker and respond more slowly.")
  public double Smoothing() { return smoothing; }
  @DesignerProperty(editorType = "float", defaultValue = "0.6")
  @SimpleProperty(category = PropertyCategory.BEHAVIOR) public void Smoothing(double value) {
    if (!Double.isNaN(value)) smoothing = Math.max(0, Math.min(0.95, value));
  }

  @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Input gain 0.1-10. Increase if the reported level is too low.")
  public double Sensitivity() { return sensitivity; }
  @DesignerProperty(editorType = "float", defaultValue = "1.0")
  @SimpleProperty(category = PropertyCategory.BEHAVIOR) public void Sensitivity(double value) {
    if (!Double.isNaN(value)) sensitivity = Math.max(0.1, Math.min(10, value));
  }

  @SimpleEvent(description = "Relative loudness, 0-100, emitted approximately every Interval milliseconds.")
  public void LevelChanged(int level) { EventDispatcher.dispatchEvent(this, "LevelChanged", level); }
  @SimpleEvent(description = "Color for the selected AnalysisMode and ColorTheme. Spectrum uses SilenceColor during silence; Loudness uses blue-cyan-green-yellow-red. Channels are 0-255. Use color directly for Screen.BackgroundColor.")
  public void ColorChanged(int red, int green, int blue, @IsColor int color) {
    EventDispatcher.dispatchEvent(this, "ColorChanged", red, green, blue, color);
  }
  @SimpleEvent(description = "Spectrum mode only: smoothed low/mid/high energy percentages sum to about 100 (all zero during silence). RGB channels (0-255) and color follow ColorTheme and SilenceColor. This is not pitch detection.")
  public void SpectrumChanged(double low, double mid, double high, int red, int green, int blue, @IsColor int color) {
    EventDispatcher.dispatchEvent(this, "SpectrumChanged", low, mid, high, red, green, blue, color);
  }
  @SimpleEvent(description = "Microphone measurement started.")
  public void Started() { EventDispatcher.dispatchEvent(this, "Started"); }
  @SimpleEvent(description = "Microphone released after stopping or an error.")
  public void Stopped() { EventDispatcher.dispatchEvent(this, "Stopped"); }
  @SimpleEvent(description = "Permission or microphone error. Display message in a Label or Notifier.")
  public void Error(String message) { EventDispatcher.dispatchEvent(this, "Error", message); }

  @Override public void onPause() { if (session != null) Stop(); }
  @Override public void onStop() { Stop(); }
  @Override public void onDestroy() { dispose(); }
  @Override public void onClear() { dispose(); }
  @Override public void onDelete() { dispose(); }
  private void dispose() { disposed = true; Stop(); }
}
