import org.classroom.soundlevel.helpers.SpectrumAnalyzer;
import org.classroom.soundlevel.helpers.SpectrumColors;

public final class SpectrumAnalyzerTest {
  private static void check(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }

  private static void near(double value, double expected, double tolerance, String message) {
    check(!Double.isNaN(value) && Math.abs(value - expected) <= tolerance,
        message + ": expected " + expected + ", got " + value);
  }

  private static short[] tones(int count, double gain, double... amplitudes) {
    double[] frequencies = {125, 1000, 4000};
    short[] samples = new short[count];
    for (int i = 0; i < count; i++) {
      double value = 0;
      for (int band = 0; band < amplitudes.length; band++)
        value += gain * amplitudes[band] * Math.sin(2 * Math.PI * frequencies[band] * i / 16000);
      check(Math.abs(value) <= 32767, "Test fixture clips");
      samples[i] = (short) Math.round(value);
    }
    return samples;
  }

  private static SpectrumAnalyzer.Result analyze(short[] samples) {
    SpectrumAnalyzer analyzer = new SpectrumAnalyzer();
    analyzer.push(samples, samples.length);
    SpectrumAnalyzer.Result result = analyzer.poll(false, 0);
    check(result != null, "Missing spectrum");
    near(result.low + result.mid + result.high, result.color == 0xff000000 ? 0 : 100, 1e-8, "Percentage sum");
    return result;
  }

  private static void valid(SpectrumAnalyzer.Result result) {
    check(result.red >= 0 && result.red <= 255 && result.green >= 0 && result.green <= 255
        && result.blue >= 0 && result.blue <= 255, "Invalid RGB channel");
    check((result.color >>> 24) == 255 && ((result.color >> 16) & 255) == result.red
        && ((result.color >> 8) & 255) == result.green && (result.color & 255) == result.blue,
        "Invalid App Inventor ARGB packing");
  }

  public static void main(String[] args) {
    for (int band = 0; band < 3; band++) {
      double[] amplitudes = {0, 0, 0}; amplitudes[band] = 1;
      SpectrumAnalyzer.Result result = analyze(tones(16000, 10000, amplitudes));
      double[] percentages = {result.low, result.mid, result.high};
      near(percentages[band], 100, 0.01, "Single tone band " + band);
      check((band == 0 ? result.red : band == 1 ? result.green : result.blue) == 255,
          "Dominant channel brightness");
      valid(result);
    }
    SpectrumAnalyzer.Result mixed = analyze(tones(16000, 5000, 2, 1, 1));
    near(mixed.low, 100 * 4.0 / 6, 0.05, "Mixture energy, bass");
    near(mixed.mid, 100 / 6.0, 0.05, "Mixture energy, mid");
    near(mixed.high, 100 / 6.0, 0.05, "Mixture energy, high");
    check(mixed.red == 255 && Math.abs(mixed.green - 128) <= 1 && Math.abs(mixed.blue - 128) <= 1,
        "Square-root gamma and brightness normalization");
    SpectrumAnalyzer.Result quiet = analyze(tones(16000, 200, 2, 1, 1));
    near(quiet.low, mixed.low, 0.1, "Volume independence, bass");
    near(quiet.mid, mixed.mid, 0.1, "Volume independence, mid");
    near(quiet.high, mixed.high, 0.1, "Volume independence, high");
    SpectrumAnalyzer.Result equal = analyze(tones(16000, 5000, 1, 1, 1));
    check(equal.red == 255 && equal.green == 255 && equal.blue == 255, "Equal mixture should be white");
    near(equal.low, 100 / 3.0, 0.05, "Equal-energy ratio");
    valid(equal);

    // The presentation palette changes colors, never the measured spectrum.
    int[] anchors = {0xff7c3aed, 0xffff3eb5, 0xffc4a1ff};
    for (int band = 0; band < 3; band++) {
      double[] amplitudes = {0, 0, 0}; amplitudes[band] = 1;
      SpectrumAnalyzer.Result result = analyze(tones(4096, 10000, amplitudes));
      check(SpectrumColors.color(result, true, 0xff24103d) == anchors[band], "Concert anchor " + band);
      check(SpectrumColors.color(result, false, 0xff24103d) == result.color, "RGB compatibility");
    }
    check(SpectrumColors.color(equal, true, 0xff24103d) == 0xffc05ee0, "Concert equal mixture");
    for (int bass = 0; bass <= 2; bass++) {
      for (int mid = 0; mid <= 2; mid++) {
        for (int treble = 0; treble <= 2; treble++) {
          if (bass + mid + treble == 0) continue;
          SpectrumAnalyzer.Result result = analyze(tones(4096, 3000, bass, mid, treble));
          double lowBefore = result.low, midBefore = result.mid, highBefore = result.high;
          int color = SpectrumColors.color(result, true, 0xff24103d);
          int r = (color >> 16) & 255, g = (color >> 8) & 255, b = color & 255;
          check((color >>> 24) == 255 && r >= 124 && g >= 58 && g <= 161 && b >= 181,
              "Concert mixture escaped purple-pink palette");
          check(r > g && b > g, "Concert mixture became green-dominant");
          check(result.low == lowBefore && result.mid == midBefore && result.high == highBefore,
              "Theme changed measurements");
          check(SpectrumColors.color(result, false, 0xff24103d) == result.color, "Switch back to RGB");
        }
      }
    }

    // DC offsets do not become bass; silence must have finite, zero percentages.
    short[] dc = new short[4096]; java.util.Arrays.fill(dc, (short) 12000);
    SpectrumAnalyzer.Result zero = analyze(dc);
    check(zero.color == 0xff000000 && zero.low == 0 && zero.mid == 0 && zero.high == 0, "DC rejection");
    valid(analyze(new short[4096]));

    // Arbitrary AudioRecord chunk boundaries must not affect a streaming FFT.
    short[] source = tones(17017, 4000, 2, 3, 1);
    SpectrumAnalyzer chunked = new SpectrumAnalyzer();
    for (int offset = 0; offset < source.length;) {
      int size = Math.min(317, source.length - offset);
      short[] chunk = new short[size]; System.arraycopy(source, offset, chunk, 0, size);
      chunked.push(chunk, size); offset += size;
    }
    SpectrumAnalyzer.Result chunks = chunked.poll(false, 0);
    SpectrumAnalyzer.Result whole = analyze(source);
    near(chunks.low, whole.low, 1e-9, "Chunk invariant bass");
    near(chunks.mid, whole.mid, 1e-9, "Chunk invariant mid");
    near(chunks.high, whole.high, 1e-9, "Chunk invariant high");

    SpectrumAnalyzer state = new SpectrumAnalyzer();
    check(state.poll(false, 0.6) == null, "No result without a full window");
    state.push(tones(2048, 10000, 1, 0, 0), 2048);
    state.poll(false, 0.6);
    // Gate must bypass smoothing and remove history immediately.
    SpectrumAnalyzer.Result gated = state.poll(true, 0.95);
    check(gated.color == 0xff000000 && gated.low + gated.mid + gated.high == 0, "Noise gate");
    check(SpectrumColors.color(gated, true, 0xff24103d) == 0xff24103d, "Concert silence purple");
    check(SpectrumColors.color(gated, false, 0xffff3eb5) == 0xffff3eb5, "Custom RGB silence");
    check(SpectrumColors.color(gated, true, 0x0024103d) == 0xff24103d, "Opaque silence color");
    check(SpectrumColors.color(gated, false, 0xff000000) == 0xff000000, "Default silence compatibility");
    state.push(tones(2048, 10000, 0, 0, 1), 2048);
    near(state.poll(false, 0.95).high, 100, 0.01, "No stale bass after gate reset");
    check(state.poll(false, 0) == null, "Draining consumes only completed frames");

    // Smoothing retains some old color, without changing normalized totals.
    SpectrumAnalyzer smooth = new SpectrumAnalyzer();
    smooth.push(tones(4096, 10000, 1, 0, 0), 4096); smooth.poll(false, 0.6);
    smooth.push(tones(8192, 10000, 0, 0, 1), 8192);
    SpectrumAnalyzer.Result transition = smooth.poll(false, 0.6);
    check(transition.low > 50 && transition.high > 20 && transition.high < 50, "Temporal smoothing");
    near(transition.low + transition.mid + transition.high, 100, 1e-8, "Smoothed sum");

    // Independent direct DFT checks FFT binning using a broadband deterministic signal.
    short[] arbitrary = new short[2048];
    java.util.Random random = new java.util.Random(37);
    double mean = 0;
    for (int i = 0; i < arbitrary.length; i++) { arbitrary[i] = (short) (random.nextInt(20001) - 10000); mean += arbitrary[i]; }
    mean /= arbitrary.length;
    double[] expected = new double[3];
    for (int bin = 1; bin < 1024; bin++) {
      double hz = bin * 16000.0 / 2048;
      if (hz < 80) continue;
      double real = 0, imaginary = 0;
      for (int i = 0; i < 2048; i++) {
        double sample = (arbitrary[i] - mean) * (0.5 - 0.5 * Math.cos(2 * Math.PI * i / 2047));
        real += sample * Math.cos(-2 * Math.PI * bin * i / 2048);
        imaginary += sample * Math.sin(-2 * Math.PI * bin * i / 2048);
      }
      expected[hz < 300 ? 0 : hz < 2000 ? 1 : 2] += real * real + imaginary * imaginary;
    }
    double total = expected[0] + expected[1] + expected[2];
    SpectrumAnalyzer.Result fft = analyze(arbitrary);
    near(fft.low, 100 * expected[0] / total, 1e-8, "FFT vs direct DFT, low");
    near(fft.mid, 100 * expected[1] / total, 1e-8, "FFT vs direct DFT, mid");
    near(fft.high, 100 * expected[2] / total, 1e-8, "FFT vs direct DFT, high");

    long started = System.nanoTime();
    analyze(tones(16000 * 30, 3000, 2, 3, 1));
    System.out.println("PASS: spectrum tones, mixtures, gain invariance, gamma, DC, streaming chunks, silence gate, smoothing, independent DFT, Concert palette, custom silence, RGB compatibility; 30 s synthetic audio processed in "
        + (System.nanoTime() - started) / 1000000 + " ms on host (not a tablet benchmark)");
  }
}
