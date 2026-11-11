# SoundLevel v4 — 음악을 RGB로 표현하기

## 업데이트와 실행

1. 기존 앱 인벤터 프로젝트를 `.aia`로 내보내 백업합니다.
2. 확장 기능 가져오기에서 `dist/SoundLevel.aix`를 가져와 기존 확장을 업데이트합니다.
3. 태블릿에서 MIT AI2 Companion을 완전히 종료하고 연결을 초기화한 뒤 다시 연결합니다.
4. `SoundLevel1`에 `ColorTheme`, `SilenceColor` 속성과 `SpectrumChanged` 이벤트가 있는지 확인합니다.
5. 시작 버튼에서 **AnalysisMode = "Spectrum"**을 설정하고 `Start`를 호출합니다. 기본 모드는 기존의 `Loudness`입니다.
6. `SpectrumChanged` 이벤트의 `color`를 `Screen1.BackgroundColor`에 연결합니다.
7. 처음 사용할 때는 Companion의 마이크 권한을 허용합니다.

```text
when ButtonStart.Click
  set SoundLevel1.AnalysisMode to "Spectrum"
  set SoundLevel1.ColorTheme to "Concert"
  set SoundLevel1.SilenceColor to make color (make a list 36 16 61)
  set SoundLevel1.Interval to 100
  set SoundLevel1.Smoothing to 0.4
  set SoundLevel1.NoiseGate to 20
  call SoundLevel1.Start

when SoundLevel1.SpectrumChanged(low, mid, high, red, green, blue, color)
  set Screen1.BackgroundColor to get color

when ButtonStop.Click
  call SoundLevel1.Stop

when SoundLevel1.Error(message)
  set LabelStatus.Text to get message
```

`SilenceColor`에는 색상 → `make color` 블록에 리스트 → `make a list`로 숫자 36, 16, 61을 연결하세요. 디자이너의 SilenceColor 색상 선택창에서 선택해도 됩니다.

이것은 블록 연결을 글로 표현한 안내입니다. `get color` 등은 이벤트 매개변수에서 꺼냅니다.
화면을 덮는 배치 컴포넌트가 있다면 그 배치의 BackgroundColor를 변경합니다.

이미 `ColorChanged`에서 배경색을 설정했다면 그 블록을 그대로 사용해도 됩니다. 이 이벤트도 선택한 분석 모드의 RGB를 전달합니다.
다만 배경색을 설정하는 이벤트는 한 곳으로 정리하세요. `LevelChanged`의 이전 배경색 설정이 남아 있으면 색상이 번갈아 바뀔 수 있습니다.

## 전달되는 값

`SpectrumChanged(low, mid, high, red, green, blue, color)`

| 값 | 범위와 의미 |
|---|---|
| low | 80~300Hz 저음의 에너지 비율(%) |
| mid | 300~2,000Hz 중음의 에너지 비율(%) |
| high | 2,000~8,000Hz 고음의 에너지 비율(%) |
| red / green / blue | 각 0~255의 정수. 선택한 ColorTheme와 SilenceColor를 적용한 RGB |
| color | 앱 인벤터에서 바로 쓸 수 있는 불투명 색상 값 |

대역의 하한은 포함하고 상한은 제외합니다. FFT 주파수 구간에 따른 근사적인 분석입니다.
비율에는 Smoothing이 적용됩니다. 소리가 있을 때 세 비율의 합계는 부동소수점 오차를 제외하면 100입니다.
무음에서는 비율만 모두 0이고, RGB와 color는 SilenceColor를 따릅니다(기본값은 검정). `color`를 숫자로 표시하면 Android의 색상 표현 때문에 음수가 나올 수 있으며 정상입니다.

**기존 RGB 테마에서는** 가장 강한 대역의 RGB 채널을 255로 맞춰 전체 음악 볼륨이 커도 색상이 한쪽 밝기에 고정되지 않도록 했습니다.
상대적으로 약한 대역도 보이도록 제곱근 보정을 적용하므로, RGB 값은 비율에 단순히 2.55를 곱한 값과 다릅니다.
예를 들어 세 대역의 에너지가 같으면 흰색, 저음만 강하면 빨강, 저음과 고음이 함께 강하면 자홍색에 가까워집니다.
주파수 분포가 비슷한 음악 구간에서는 색상도 비슷하게 유지됩니다.

## Concert 테마: 보라·핑크 콘서트 색감

`ColorTheme`를 `Concert`로 설정하면 다음 세 색상을 대역 비율로 부드럽게 혼합합니다.

| 강한 대역 | 색상 | RGB |
|---|---|---|
| 저음 | 선명한 보라 `#7C3AED` | 124, 58, 237 |
| 중음 | 네온 핑크 `#FF3EB5` | 255, 62, 181 |
| 고음 | 밝은 라벤더 `#C4A1FF` | 196, 161, 255 |
| 무음(추천 설정) | 어두운 보라 `#24103D` | 36, 16, 61 |

Concert 테마는 실제 대역 비율을 바꾸지 않고 표현 색상만 변경합니다. RGB 테마의 채널별 최대 밝기 보정 대신 세 대표색의 가중 평균을 사용하여 초록이나 흰색으로 쉽게 바뀌지 않도록 했습니다.
세 대역의 에너지가 같으면 연보라 `#C05EE0`에 가까워집니다. 이 색들은 참고 이미지의 분위기에 맞춘 제안이며 이미지에서 정확히 추출한 값은 아닙니다.

`ColorTheme`의 기본값은 `RGB`, `SilenceColor`의 기본값은 검정으로 기존 동작을 유지합니다. 새 분위기를 사용하려면 둘 다 설정하세요.
`SilenceColor`는 어느 테마에서도 지정할 수 있으며 항상 불투명 색상으로 처리합니다.
`ColorTheme`, `SilenceColor`를 실행 중 바꾸면 다음 색상 이벤트부터 반영됩니다. Loudness 모드에는 영향을 주지 않습니다.

## 교실 환경에 맞추기

- **색상이 너무 늦게 바뀜**: Smoothing을 0.2~0.4로 낮춥니다. 0이면 완화하지 않습니다.
- **너무 빠르게 깜빡임**: Smoothing을 0.6~0.8로 높입니다. 반응이 그만큼 느려집니다.
- **음악을 꺼도 무음색으로 바뀌지 않음**: `LevelChanged`로 실내 소리 크기를 확인하고, NoiseGate를 그 값보다 조금 높입니다.
- **작은 음악에서 계속 무음색임**: NoiseGate를 낮추거나 음악 볼륨을 조금 높입니다.
- **한 색에 고정됨**: low/mid/high를 레이블에 표시해 실제 비율을 확인합니다. 마이크가 스피커에 너무 가까우면 멀리하고 볼륨을 낮춥니다. 마이크가 과입력되면 주파수 분포가 왜곡될 수 있습니다.

NoiseGate는 완화 전의 Level과 비교합니다. 기본값은 20이고 이 값 이하에서 SilenceColor로 전환합니다.
무음에서는 과거 대역 비율과 분석 버퍼도 초기화하여 다음 음악에 이전 색상이 섞이지 않게 합니다.
Sensitivity는 Level과 무음 판정에 영향을 주며, 대역 비율 자체를 바꾸지는 않습니다.

분석은 16kHz 입력, 2,048개 샘플(128ms) 단위 FFT, 50% 겹침, Hann 윈도우를 사용합니다.
최초 음악 분석에는 약 128ms 이상의 입력이 필요하며, 이벤트 간격과 기기 성능에 따라 지연이 더해집니다.
Interval을 50으로 설정해도 더 많은 주파수 정보를 얻는 것은 아니며, SpectrumChanged는 새 분석 결과가 있을 때만 발생합니다.
기본 Interval=100은 대략적인 요청값이고 정확한 주기 보장은 아닙니다.

## 수업 전 실기기 확인

다른 기기나 외부 스피커에서 음악을 틀고 태블릿 마이크로 입력받는 방식으로 먼저 확인하세요. 내부 오디오를 직접 읽는 기능은 아닙니다.

1. 외부 기기에서 125Hz, 1,000Hz, 4,000Hz 테스트 음을 적당한 음량으로 차례로 재생합니다. Concert 테마에서는 각각 보라, 핑크, 라벤더에 가까워지는지 확인합니다. RGB 테마에서는 각각 빨강, 초록, 파랑입니다. 작은 스피커는 125Hz를 충분히 재생하지 못할 수 있습니다.
2. 음악을 재생하면서 low/mid/high 비율과 배경색의 변화를 확인합니다.
3. 같은 음악의 볼륨을 바꿔 비율이 소리 크기만큼 크게 변하지 않는지 확인합니다. 실제 스피커·마이크의 특성으로 차이가 생길 수 있습니다.
4. 음악을 끄고 NoiseGate를 실내 잡음에 맞춰 지정한 SilenceColor로 전환되는지 확인합니다.
5. 중지→다시 시작, 홈 화면으로 이동→복귀 후 다시 시작, Companion 재연결을 확인합니다.

중지 후에는 마지막 화면 색상이 유지됩니다. 중지 시 검정으로 바꾸려면 `Stopped`에서 배경색을 검정으로 설정하세요.

빌드, 파일 구조, 블록 정보와 합성 신호를 이용한 DSP·테마 색상·사용자 지정 무음색·RGB 호환성 검증은 완료했습니다. 실제 Android 태블릿의 마이크와 AI Companion에서의 실행은 아직 확인하지 않았습니다.
