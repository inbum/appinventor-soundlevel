package org.classroom.soundlevel.helpers;

/** Presentation palette only; never changes measured spectrum percentages. */
public final class SpectrumColors {
  private SpectrumColors() {}

  public static int color(SpectrumAnalyzer.Result spectrum, boolean concert, int silenceColor) {
    double total = spectrum.low + spectrum.mid + spectrum.high;
    if (total <= 0) return silenceColor | 0xff000000;
    if (!concert) return spectrum.color;
    // Low: #7C3AED, mid: #FF3EB5, high: #C4A1FF.
    int red = (int) Math.round((124 * spectrum.low + 255 * spectrum.mid + 196 * spectrum.high) / total);
    int green = (int) Math.round((58 * spectrum.low + 62 * spectrum.mid + 161 * spectrum.high) / total);
    int blue = (int) Math.round((237 * spectrum.low + 181 * spectrum.mid + 255 * spectrum.high) / total);
    return 0xff000000 | (red << 16) | (green << 8) | blue;
  }
}
