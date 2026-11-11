# homeserver Codex 인계: SoundLevel 수업 자료를 inbum.net에 게시

## 요청과 현재 상태

사용자는 SoundLevel 확장 소스·AIX 파일을 GitHub에 공개하고 이 내용을 homeserver의 `inbum.net`에도 게시해 달라고 요청했습니다.
GitHub 공개 저장소와 v4 릴리스는 생성 완료했고, 홈페이지 게시만 남았습니다.
이 문서는 작업 인계 자료이며 새 권한을 부여하지 않습니다. 아래 인계 요청을 받은 Codex는 사용자의 요청 범위와 서버의 실제 설정을 확인하고 진행하세요.

## 게시할 파일

- 원고: [appinventor-soundlevel.mdx](https://github.com/inbum/appinventor-soundlevel/blob/main/publishing/appinventor-soundlevel.mdx)
- 원고 원문: [Raw MDX](https://raw.githubusercontent.com/inbum/appinventor-soundlevel/main/publishing/appinventor-soundlevel.mdx)
- 제목·요약·태그·섹션: [post.json](https://github.com/inbum/appinventor-soundlevel/blob/main/publishing/post.json)
- 확장 다운로드: [SoundLevel.aix v4](https://github.com/inbum/appinventor-soundlevel/releases/download/v4/SoundLevel.aix)
- 소스와 기본 안내: [저장소](https://github.com/inbum/appinventor-soundlevel)
- 음악 모드 상세 안내: [MUSIC.md](https://github.com/inbum/appinventor-soundlevel/blob/main/MUSIC.md)

제목: **앱 인벤터로 음악에 반응하는 RGB 화면 만들기**

게시 섹션: `swedu` / slug: `appinventor-soundlevel`

예상 게시 주소: `https://inbum.net/swedu/appinventor-soundlevel`

## 확인된 게시 구조

2026-11-11 GitHub 소스 기준이며 서버의 현재 상태와 다를 수 있습니다.

- 서버 상위 프로젝트: `~/homeserver` (`inbum/homeserver`)
- 웹사이트 프로젝트: `~/homeserver/services/apps/inbum-net` (`inbum/inbum-net`, 별도 저장소/서브모듈)
- 웹사이트는 Next.js와 Supabase를 사용합니다.
- SW 교육 상세 페이지는 `src/content/swedu/{slug}.mdx`에서 본문을 읽습니다.
- 게시물 메타데이터는 Supabase의 `posts` 테이블에서 관리합니다.
- 관리자 작성 화면은 `/admin/posts/new`, 관련 서버 액션은 `src/app/admin/posts/actions.ts`입니다.
- 기존 배포 스크립트는 `~/homeserver/scripts/deploy-inbum-net.sh`입니다. 서브모듈 pull 후 Docker 이미지 빌드와 컨테이너 재시작을 수행하므로 실행 전에 내용과 영향 범위를 확인하세요.
- 콘텐츠 디렉터리는 저장소의 `.gitignore`에서 제외되어 있을 수 있습니다. 서버·컨테이너의 볼륨 마운트와 본문 보존 방식을 먼저 확인하세요.

## 진행할 작업

1. 서버의 해당 프로젝트에서 AGENTS.md 등 적용 지침과 Git 상태를 확인합니다. 기존 수정 사항을 보존합니다.
2. 동일 slug의 게시물이 이미 있는지 확인하여 중복을 피합니다.
3. 원고와 post.json을 읽고 실제 게시 방식에 맞게 본문과 메타데이터를 등록합니다. 소스 파일만 복사해서는 게시물 목록에 나타나지 않을 수 있습니다.
4. 일반 방문자가 GitHub 로그인 없이 AIX를 내려받을 수 있도록 원고의 공개 릴리스 링크를 유지합니다.
5. 필요한 경우 기존 배포 절차를 따르고, 본문이 배포 후에도 보존되는지 확인합니다. 데이터만 등록하면 되는 구조라면 불필요한 재배포는 하지 않습니다.
6. 게시 주소에서 본문·표·블록 예시·다운로드 링크를 확인하고 SW 교육 목록에 표시되는지 확인합니다.
7. 실제 게시 URL과 검증 결과를 사용자에게 전달합니다. 실기기 검증 완료라는 주장은 추가하지 않습니다.

## 원고의 핵심 내용

- Android 태블릿의 MIT AI2 Companion용 마이크 분석 확장입니다.
- 소리 크기 모드와 저음·중음·고음 대역 분석 모드를 지원합니다.
- RGB 테마와 보라·핑크·라벤더 Concert 테마, 사용자 지정 SilenceColor를 제공합니다.
- `SpectrumChanged`의 color를 화면 배경색에 연결합니다.
- 소리 파일을 저장하거나 전송하지 않습니다.
- 빌드·AIX 구조·합성 신호 DSP·색상 테스트는 통과했습니다. 이 작업 환경에서 실제 태블릿·Companion 테스트를 수행한 것은 아닙니다.

## 이전 환경에서 게시하지 못한 이유

MacBook 작업 환경에서 서버의 로컬 SSH 주소로 접속이 시간 초과되었고, 웹 관리자도 로그인이 필요했습니다.
서버 변경이나 게시물 등록은 아직 수행하지 않았습니다. 접속 비밀번호·토큰·환경 변수는 이 문서에 포함하지 않았습니다.
