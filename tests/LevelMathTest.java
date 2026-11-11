import org.classroom.soundlevel.helpers.LevelMath;

public final class LevelMathTest {
  private static void near(double actual, double expected) {
    if (Math.abs(actual - expected) > 0.02)
      throw new AssertionError("Expected " + expected + ", got " + actual);
  }

  public static void main(String[] args) {
    int[] expectedColors = {0xff0000ff, 0xff00ffff, 0xff00ff00, 0xffffff00, 0xffff0000};
    for (int i = 0; i < expectedColors.length; i++) {
      if (LevelMath.color(i * 25) != expectedColors[i])
        throw new AssertionError("Wrong RGB anchor at " + i * 25);
    }
    if (LevelMath.color(-1) != expectedColors[0] || LevelMath.color(101) != expectedColors[4])
      throw new AssertionError("RGB input was not clamped");
    for (int i = 0; i <= 100; i++) {
      int color = LevelMath.color(i);
      if ((color >>> 24) != 255) throw new AssertionError("Color is not opaque");
      if (i > 0) {
        int previousColor = LevelMath.color(i - 1);
        for (int shift = 0; shift <= 16; shift += 8) {
          if (Math.abs(((color >> shift) & 255) - ((previousColor >> shift) & 255)) > 11)
            throw new AssertionError("Discontinuous RGB transition at " + i);
        }
      }
    }
    near(LevelMath.level(0, 1024, 1), 0);
    near(LevelMath.level(1, 0, 1), 0);
    // Signals at -60, -30 and 0 dBFS map to 0, 50 and 100.
    for (int db = -60; db <= 0; db += 30) {
      double amplitude = 32768 * Math.pow(10, db / 20.0);
      near(LevelMath.level(amplitude * amplitude * 1024, 1024, 1), (db + 60) * 100.0 / 60);
    }
    // Equivalent windows produce the same loudness (independent of event interval).
    near(LevelMath.level(1000000, 100, 1), LevelMath.level(10000000, 1000, 1));
    // A sine wave's RMS is peak/sqrt(2), including negative samples and -32768.
    double squares = 0;
    for (int i = 0; i < 1600; i++) {
      short sample = (short) Math.round(16000 * Math.sin(2 * Math.PI * i / 16));
      squares += (double) sample * sample;
    }
    double expectedDb = 20 * Math.log10(16000 / Math.sqrt(2) / 32768);
    near(LevelMath.level(squares, 1600, 1), (expectedDb + 60) * 100 / 60);
    near(LevelMath.level((double) -32768 * -32768, 1, 1), 100);
    near(LevelMath.level(1, 1, 0.1), 0);
    near(LevelMath.level(32768.0 * 32768, 1, 10), 100);
    double previous = -1;
    for (int amplitude = 0; amplitude <= 32768; amplitude++) {
      double value = LevelMath.level((double) amplitude * amplitude, 1, 1);
      if (Double.isNaN(value) || value < previous || value < 0 || value > 100)
        throw new AssertionError("Invalid loudness at amplitude " + amplitude);
      previous = value;
    }
    System.out.println("PASS: silence, dBFS mapping, window sizes, sine RMS, signed limits, sensitivity, monotonic range, RGB anchors, clamping, opacity and continuity");
  }
}
