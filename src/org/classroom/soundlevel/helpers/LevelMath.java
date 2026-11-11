package org.classroom.soundlevel.helpers;

/** Relative loudness, not calibrated sound pressure level (dB SPL). */
public final class LevelMath {
  private LevelMath() {}

  /** Opaque App Inventor/Android color: blue -> cyan -> green -> yellow -> red. */
  public static int color(int level) {
    int bounded = Math.max(0, Math.min(100, level));
    int red = 0;
    int green = 0;
    int blue = 0;
    if (bounded <= 25) {
      green = Math.round(bounded * 255f / 25);
      blue = 255;
    } else if (bounded <= 50) {
      green = 255;
      blue = Math.round((50 - bounded) * 255f / 25);
    } else if (bounded <= 75) {
      red = Math.round((bounded - 50) * 255f / 25);
      green = 255;
    } else {
      red = 255;
      green = Math.round((100 - bounded) * 255f / 25);
    }
    return 0xff000000 | (red << 16) | (green << 8) | blue;
  }

  public static double level(double sumSquares, long count, double sensitivity) {
    if (count <= 0 || sumSquares <= 0) return 0;
    double rms = Math.sqrt(sumSquares / count) / 32768.0;
    double dbfs = 20.0 * Math.log10(Math.max(1e-10, rms * sensitivity));
    return Math.max(0, Math.min(100, (dbfs + 60.0) * 100.0 / 60.0));
  }
}
