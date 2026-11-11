package org.classroom.soundlevel.helpers;

/** Streaming 16 kHz PCM -> overlapping Hann-windowed FFT -> three band energies. */
public final class SpectrumAnalyzer {
  private static final int SIZE = 2048;
  private static final int HOP = 1024;
  private final double[] ring = new double[SIZE];
  private final double[] real = new double[SIZE];
  private final double[] imaginary = new double[SIZE];
  private final double[] window = new double[SIZE];
  private final double[] cosine = new double[SIZE / 2];
  private final double[] sine = new double[SIZE / 2];
  private final double[] energies = new double[3];
  private final double[] smoothed = new double[3];
  private int write;
  private int filled;
  private int sinceFrame;
  private int frames;
  private boolean first = true;

  public static final class Result {
    public final double low;
    public final double mid;
    public final double high;
    public final int red;
    public final int green;
    public final int blue;
    public final int color;

    private Result(double low, double mid, double high, int red, int green, int blue) {
      this.low = low;
      this.mid = mid;
      this.high = high;
      this.red = red;
      this.green = green;
      this.blue = blue;
      this.color = 0xff000000 | (red << 16) | (green << 8) | blue;
    }
  }

  public SpectrumAnalyzer() {
    for (int i = 0; i < SIZE; i++) window[i] = 0.5 - 0.5 * Math.cos(2 * Math.PI * i / (SIZE - 1));
    for (int i = 0; i < SIZE / 2; i++) {
      cosine[i] = Math.cos(-2 * Math.PI * i / SIZE);
      sine[i] = Math.sin(-2 * Math.PI * i / SIZE);
    }
  }

  public void reset() {
    write = filled = sinceFrame = frames = 0;
    first = true;
    for (int i = 0; i < 3; i++) energies[i] = smoothed[i] = 0;
  }

  public void push(short[] samples, int length) {
    for (int i = 0; i < length; i++) {
      ring[write] = samples[i];
      write = (write + 1) & (SIZE - 1);
      if (filled < SIZE) {
        filled++;
        if (filled == SIZE) { transform(); sinceFrame = 0; }
      } else if (++sinceFrame == HOP) {
        transform();
        sinceFrame = 0;
      }
    }
  }

  private void transform() {
    double mean = 0;
    for (int i = 0; i < SIZE; i++) mean += ring[i];
    mean /= SIZE;
    for (int i = 0; i < SIZE; i++) {
      real[i] = (ring[(write + i) & (SIZE - 1)] - mean) * window[i];
      imaginary[i] = 0;
    }
    // Iterative radix-2 Cooley-Tukey FFT, with bit-reversed input permutation.
    for (int i = 1, j = 0; i < SIZE; i++) {
      int bit = SIZE >> 1;
      while ((j & bit) != 0) { j ^= bit; bit >>= 1; }
      j ^= bit;
      if (i < j) {
        double temporary = real[i]; real[i] = real[j]; real[j] = temporary;
      }
    }
    for (int length = 2; length <= SIZE; length <<= 1) {
      int half = length / 2;
      int step = SIZE / length;
      for (int start = 0; start < SIZE; start += length) {
        for (int j = 0; j < half; j++) {
          int a = start + j;
          int b = a + half;
          int twiddle = j * step;
          double r = cosine[twiddle] * real[b] - sine[twiddle] * imaginary[b];
          double im = sine[twiddle] * real[b] + cosine[twiddle] * imaginary[b];
          real[b] = real[a] - r;
          imaginary[b] = imaginary[a] - im;
          real[a] += r;
          imaginary[a] += im;
        }
      }
    }
    for (int bin = 1; bin < SIZE / 2; bin++) {
      double hz = bin * 16000.0 / SIZE;
      if (hz < 80) continue;
      int band = hz < 300 ? 0 : hz < 2000 ? 1 : 2;
      energies[band] += real[bin] * real[bin] + imaginary[bin] * imaginary[bin];
    }
    frames++;
  }

  /** Returns null until a full FFT window is available. Silence is explicitly black. */
  public Result poll(boolean silent, double retention) {
    if (silent) {
      // Discard the ring too: do not let pre-silence audio color the next onset.
      reset();
      return new Result(0, 0, 0, 0, 0, 0);
    }
    if (frames == 0) return null;
    double total = energies[0] + energies[1] + energies[2];
    if (total < 1e-12) {
      reset();
      return new Result(0, 0, 0, 0, 0, 0);
    }
    for (int band = 0; band < 3; band++) {
      double ratio = energies[band] / total;
      smoothed[band] = first ? ratio : retention * smoothed[band] + (1 - retention) * ratio;
      energies[band] = 0;
    }
    first = false;
    frames = 0;
    double sum = smoothed[0] + smoothed[1] + smoothed[2];
    double max = Math.max(smoothed[0], Math.max(smoothed[1], smoothed[2]));
    // Square-root display gamma makes weaker bands visible, independent of volume.
    int red = (int) Math.round(255 * Math.sqrt(smoothed[0] / max));
    int green = (int) Math.round(255 * Math.sqrt(smoothed[1] / max));
    int blue = (int) Math.round(255 * Math.sqrt(smoothed[2] / max));
    return new Result(100 * smoothed[0] / sum, 100 * smoothed[1] / sum,
        100 * smoothed[2] / sum, red, green, blue);
  }
}
