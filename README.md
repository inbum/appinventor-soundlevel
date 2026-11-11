# SoundLevel — 앱 인벤터 수업용 소리 크기·음악 RGB 확장

배포 파일: `dist/SoundLevel.aix` / 컴포넌트 이름: `SoundLevel`

현재 버전: **4 — Concert 테마·무음 색상 설정 추가**. 기존 `LevelChanged`, `ColorChanged` 블록도 그대로 지원합니다.

## 음악을 RGB로 표현하기 (버전 4)

확장을 업데이트한 다음 **AnalysisMode를 반드시 `Spectrum`으로 설정**하세요. 기존 프로젝트와의 호환성을 위해 기본값은 `Loudness`입니다.

```text
when ButtonStart.Click
  set SoundLevel1.AnalysisMode to "Spectrum"
  set SoundLevel1.ColorTheme to "Concert"
  set SoundLevel1.SilenceColor to make color (make a list 36 16 61)
  set SoundLevel1.Smoothing to 0.4
  call SoundLevel1.Start

when SoundLevel1.SpectrumChanged(low, mid, high, red, green, blue, color)
  set Screen1.BackgroundColor to get color

when ButtonStop.Click
  call SoundLevel1.Stop

when SoundLevel1.Error(message)
  set LabelStatus.Text to get message
```

`get color`는 이벤트 매개변수 `color`에서 꺼내 사용합니다. 위 예제는 블록 연결 안내이며 붙여넣기로 변환되는 코드는 아닙니다.
자세한 설명과 교실 테스트 절차는 [음악 RGB 사용 안내](MUSIC.md)를 참고하세요.

| 이벤트 값 | 범위 | 의미 |
|---|---|---|
| low / mid / high | 각 0~100, 합계 약 100 | 분석 범위 내 저음·중음·고음의 완화된 에너지 비율 |
| red / green / blue | 각 0~255 | 선택한 ColorTheme와 SilenceColor가 적용된 RGB |
| color | 앱 인벤터 색상 값 | 배경색에 바로 연결 |

저음은 80Hz 이상~300Hz 미만, 중음은 300Hz 이상~2,000Hz 미만, 고음은 2,000Hz 이상~8,000Hz 미만입니다.
무음 기준 이하에서는 비율만 모두 0이며, RGB와 color는 SilenceColor를 따릅니다(기본 검정). 실제 비율은 FFT 주파수 구간의 해상도에 따른 근사치입니다.
한 음의 높낮이(기본 주파수)를 검출하는 기능은 아닙니다.

`ColorChanged`도 선택한 모드의 RGB를 전달합니다. 이미 이 이벤트에서 `color`를 배경색에 연결했다면 **AnalysisMode만 바꿔도** 음악 모드로 사용할 수 있습니다.
배경색은 `SpectrumChanged` 또는 `ColorChanged` 한 곳에서만 설정하세요. 음악 모드에서도 `LevelChanged`는 소리 크기를 계속 전달합니다.

## 소리 크기로 화면 색상 바꾸기 (기존 Loudness 모드)

`ColorChanged(red, green, blue, color)` 이벤트에서 값을 받습니다.

- `red`, `green`, `blue`: 각 0~255의 정수. RGB 혼합 활동이나 숫자 표시용입니다.
- `color`: 앱 인벤터에서 바로 사용할 수 있는 불투명 색상 값입니다. `Screen1.BackgroundColor`에 연결하세요.
  Android의 ARGB 정수이므로 숫자로 표시하면 음수일 수 있습니다. 정상이며, RGB 채널은 각각 0~255입니다.

```text
when SoundLevel1.ColorChanged(red, green, blue, color)
  set Screen1.BackgroundColor to get color
```

시작·중지 버튼은 기존과 같이 각각 `SoundLevel1.Start`, `SoundLevel1.Stop`을 호출합니다.
이 절은 AnalysisMode가 `Loudness`일 때의 설명입니다. `ColorChanged`는 측정 중 `LevelChanged` 직후 발생하며 같은 소리 크기 값을 사용합니다.
색상은 소리 크기가 커질수록 다음 순서로 연속적으로 변합니다.

| 소리 크기 | R | G | B | 색상 |
|---:|---:|---:|---:|---|
| 0 | 0 | 0 | 255 | 파랑 |
| 25 | 0 | 255 | 255 | 청록 |
| 50 | 0 | 255 | 0 | 초록 |
| 75 | 255 | 255 | 0 | 노랑 |
| 100 | 255 | 0 | 0 | 빨강 |

사이 값은 인접 색상 사이에서 보간합니다. 무음은 파랑으로 표현하며, 중지할 때 화면의 마지막 색상은 유지됩니다.
중지 시 흰색 등으로 되돌리고 싶다면 `Stopped` 이벤트에서 배경색을 지정하세요.
이 기능은 **소리 크기**를 RGB로 표현합니다. 음의 높낮이나 저음·중음·고음의 비율은 분석하지 않습니다.

기존의 `LevelChanged` 이벤트에서도 배경색을 설정하고 있다면, 해당 배경색 설정을 제거하고 위 이벤트에서 설정하세요.
두 이벤트에서 배경색을 설정하면 나중에 실행되는 `ColorChanged`의 색상이 적용됩니다.

### 기존 확장 업데이트

프로젝트를 `.aia`로 내보내 백업한 뒤 새 `SoundLevel.aix`를 가져오고, 같은 확장을 업데이트하는 안내가 나오면 진행합니다.
업데이트 후에는 Companion을 완전히 종료하고 연결을 초기화한 뒤 다시 연결합니다.
컴포넌트 버전이 4인지, `ColorTheme`, `SilenceColor` 속성이 나타나는지 확인하세요.

Android 태블릿에서 MIT AI2 Companion으로 소리 크기에 따라 화면 색상을 바꾸는 활동을 위한 확장입니다.
소리는 메모리에서만 처리하며 녹음 파일을 저장하거나 서버로 전송하지 않습니다.
확장 자체가 요청하는 권한은 마이크(`android.permission.RECORD_AUDIO`) 하나입니다.

## 1. 확장 추가

1. 컴퓨터의 앱 인벤터 프로젝트에서 디자이너 → 팔레트 → **확장 기능(Extension)** → **확장 기능 가져오기(Import extension)**를 엽니다.
2. `SoundLevel.aix`를 업로드합니다.
3. 추가된 `SoundLevel`을 뷰어로 끌어다 놓습니다. 보이지 않는 컴포넌트 `SoundLevel1`이 생깁니다.
4. 버튼 두 개(`ButtonStart`, `ButtonStop`)와 레이블 두 개(`LabelLevel`, `LabelStatus`)를 추가합니다.
5. 버튼의 Text를 각각 `측정 시작`, `측정 중지`로 설정합니다.
6. 태블릿의 MIT AI2 Companion을 가능한 최신 버전으로 업데이트한 뒤 연결합니다.
7. 확장 추가·교체 후 블록이 인식되지 않으면 Companion 연결을 초기화하고 다시 연결합니다.

## 2. 블록 연결

아래는 블록을 글로 표현한 연결 안내입니다. 텍스트를 붙여넣어 블록으로 변환하는 기능은 아닙니다.

```text
when ButtonStart.Click
  set LabelStatus.Text to "마이크 권한 확인 중…"
  call SoundLevel1.Start

when SoundLevel1.Started
  set LabelStatus.Text to "측정 중"

when ButtonStop.Click
  call SoundLevel1.Stop
  set LabelStatus.Text to "중지 중…"

when SoundLevel1.Stopped
  set LabelStatus.Text to "측정 중지"
  set LabelLevel.Text to 0

when SoundLevel1.Error(message)
  set LabelStatus.Text to get message

when SoundLevel1.LevelChanged(level)
  set LabelLevel.Text to get level
  if get level < 30
    set Screen1.BackgroundColor to 파랑
  else if get level < 60
    set Screen1.BackgroundColor to 초록
  else
    set Screen1.BackgroundColor to 빨강
```

`level`과 `message`는 각 이벤트 블록의 매개변수에 마우스를 올려 나타나는 `get` 블록을 꺼내 사용합니다.
`else if`는 제어의 `if` 블록 톱니바퀴에서 추가합니다. 색상은 기본 제공 색상 블록을 사용합니다.
화면을 덮는 배치 컴포넌트가 있다면 해당 배치의 BackgroundColor를 바꾸세요.

## 3. 태블릿에서 첫 실행

1. `측정 시작`을 누릅니다.
2. Android 권한 창에서 마이크 사용을 **앱 사용 중에만 허용** 또는 **이번만 허용**합니다(기기마다 문구가 다름).
3. 조용히 있기 → 말하기 → 손뼉 치기로 숫자와 배경색이 달라지는지 확인합니다.
4. `측정 중지`를 누른 뒤 다시 시작할 수 있는지 확인합니다.
5. 홈 화면으로 나가면 측정을 중지합니다. Companion으로 돌아온 후 `측정 시작`을 다시 누릅니다.

권한은 Companion 실습에서는 **MIT AI2 Companion 앱**에 부여합니다. 이후 APK로 빌드한다면 설치한 앱의 권한을 따로 허용해야 합니다.
별도의 `Screen1.AskForPermission` 블록은 필요하지 않습니다. `Start`가 권한 승인 후 측정을 시작합니다.

## 4. 교실 환경 조정

| 속성 | 기본값 | 의미 |
|---|---:|---|
| Interval | 100 | 이벤트 간격(ms), 범위 50~2000. 실제 주기는 기기에 따라 달라집니다. |
| Smoothing | 0.6 | 값 완화 정도, 범위 0~0.95. 높을수록 덜 흔들리고 반응이 느립니다. |
| Sensitivity | 1.0 | 감도 배수, 범위 0.1~10. 값이 작게 나오면 높입니다. |
| AnalysisMode | Loudness | `Loudness`: 소리 크기, `Spectrum`: 음악 대역 비율로 색상 변환. 실행 중 전환 가능. |
| NoiseGate | 20 | 음악 모드에서 원래 소리 크기가 이 값 이하이면 SilenceColor 적용. 범위 0~100. |
| ColorTheme | RGB | 음악 모드의 색상 테마: `RGB` 또는 `Concert`. |
| SilenceColor | 검정 | 음악 모드에서 무음일 때의 색상. 추천: RGB(36, 16, 61), 어두운 보라. |
| Level | 읽기 전용 | 최근 소리 크기(0~100). 중지 후에는 0입니다. |
| IsListening | 읽기 전용 | 마이크 측정을 시작했는지 여부입니다. |

먼저 조용할 때와 말할 때 `LabelLevel` 숫자를 관찰한 뒤 30, 60 기준을 조정하세요.
예를 들어 조용할 때 45, 말할 때 65라면 기준을 50, 70으로 바꿔 봅니다.
임계값 주변에서 여전히 색이 깜빡이면 `Smoothing`을 0.8로 올리거나, 심화 활동으로 진입·이탈 기준을 다르게 하는 히스테리시스를 적용할 수 있습니다.

0~100은 RMS 진폭의 로그값(-60~0 dBFS)을 환산한 **상대 크기**입니다. 실제 소음계의 dB SPL 값이 아니며, 태블릿별 마이크·자동 이득 조절에 따라 차이가 납니다.
절대 소음 측정이나 여러 기기의 숫자를 직접 비교하는 용도로 사용하지 마세요.

## 5. 문제 해결

- **권한 거부 오류**: Android 설정 → 앱 → MIT AI2 Companion → 권한 → 마이크를 허용한 뒤 시작합니다. 계속 차단되면 앱을 완전히 종료하고 다시 연결합니다.
- **계속 0이 표시됨**: 기기의 전체 마이크 차단 스위치/개인정보 설정을 확인합니다. 다른 녹음 앱으로 마이크가 동작하는지도 확인합니다.
- **마이크 초기화·읽기 오류**: 카메라·녹음기·음성 인식 등 다른 마이크 사용 기능을 종료한 뒤 다시 시작합니다. 이 확장과 SoundRecorder/SpeechRecognizer를 동시에 사용하지 않습니다.
- **권한 허용 후 반응이 없음**: 앱을 벗어나면 대기 중인 시작 요청도 취소될 수 있습니다. Companion으로 돌아와 시작 버튼을 다시 누릅니다.
- **중지 이벤트가 발생하지 않음**: 실제 마이크 세션이 없던 상태(예: 권한 대기 중 취소)에서는 `Stopped`가 발생하지 않습니다.
- **확장을 수정해 다시 가져왔는데 이전 동작이 남음**: Companion을 완전히 종료하고 연결을 초기화합니다.

## 6. 검증 상태

이 프로젝트의 자동 검증은 Java 컴파일, Android DEX 생성, AIX 내부 구조·권한·블록 정보, 소리 크기 계산의 정확성과 경계값을 확인합니다.
음악 분석은 저음·중음·고음 단일 신호, 혼합 신호의 에너지 비율, 볼륨 변화, RGB 보정, 무음 처리, 값 완화, 입력 청크 분할을 검증합니다.
FFT 결과를 별도로 계산한 직접 DFT 결과와 비교해 대역별 계산도 확인합니다. Concert 테마의 대표색·혼합색, 무음 색상, 기존 RGB 호환성도 검증합니다.
실제 Android 태블릿과 AI Companion에서는 아직 실행하지 않았습니다. 위 첫 실행 절차로 수업 전 확인이 필요합니다.
권한 팝업, 마이크 장치 접근, 화면 전환 중 해제, Companion 재연결은 실기기에서 확인할 항목입니다.

## 7. 소스 및 재빌드

GitHub에서 내려받은 새 환경에서는 먼저 공식 빌드 의존성을 준비합니다. JDK 8~17, Git, curl, Python 3이 필요합니다.

```sh
bash setup-build.sh
bash build.sh
```

준비 스크립트는 버전을 고정한 MIT 공식 템플릿·라이브러리와 Apache Ant를 내려받습니다. Ant 파일의 SHA-256도 확인합니다.
결과는 `dist/SoundLevel.aix`에 생성됩니다. 내려받은 빌드 의존성은 Git에 포함하지 않습니다.

- 코드: `src/org/classroom/soundlevel/SoundLevel.java`
- 수치 계산: `src/org/classroom/soundlevel/helpers/LevelMath.java`
- 음악 분석: `src/org/classroom/soundlevel/helpers/SpectrumAnalyzer.java`
- 색상 테마: `src/org/classroom/soundlevel/helpers/SpectrumColors.java`
- 빌드: `bash build.sh` (JDK 8~17 중 Java 7 대상으로 컴파일 가능한 JDK 필요, 이 환경에서는 JDK 17 사용)
- 검증: `tests/LevelMathTest.java`, `tests/SpectrumAnalyzerTest.java`, `tests/verify_aix.py`

빌드 도구는 MIT 공식 [extension-template](https://github.com/mit-cml/extension-template)을 기반으로 합니다.
`build.xml`은 해당 템플릿에서 라이브러리 경로를 이 프로젝트 구조에 맞게 변경했습니다.
`.build-template/lib/appinventor`는 MIT 공식 [extension-deps](https://github.com/mit-cml/extension-deps)의 커밋 `78dc0a61b5f437ff6b54dc3998921fa599bea20f`(v194)입니다.
기존 런타임 API만 사용하며, 최신 Companion에서의 호환성은 실기기 검증이 필요합니다.
Apache Ant 1.10.15의 `ant.jar`, `ant-launcher.jar`는 `.build-template/lib`에 있습니다.
배포한 `.aix`를 가져와 사용하는 데는 Java나 빌드 도구 설치가 필요 없습니다.
