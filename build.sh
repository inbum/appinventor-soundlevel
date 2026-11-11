#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
if [[ -x /opt/homebrew/opt/openjdk@17/bin/java ]]; then
  soundlevel_java=/opt/homebrew/opt/openjdk@17/bin/java
elif [[ -n "${JAVA_HOME:-}" ]]; then
  soundlevel_java="$JAVA_HOME/bin/java"
else
  soundlevel_java=java
fi
"$soundlevel_java" -cp '.build-template/lib/ant.jar:.build-template/lib/ant-launcher.jar' org.apache.tools.ant.Main extensions
mkdir -p dist
cp out/org.classroom.soundlevel.aix dist/SoundLevel.aix
python3 tests/verify_aix.py
"${soundlevel_java%java}javac" -d build/tests src/org/classroom/soundlevel/helpers/LevelMath.java src/org/classroom/soundlevel/helpers/SpectrumAnalyzer.java src/org/classroom/soundlevel/helpers/SpectrumColors.java tests/LevelMathTest.java tests/SpectrumAnalyzerTest.java
"$soundlevel_java" -cp build/tests LevelMathTest
"$soundlevel_java" -cp build/tests SpectrumAnalyzerTest
