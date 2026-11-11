#!/usr/bin/env bash
# Fetch official, pinned build dependencies into the ignored local tools folder.
set -euo pipefail
cd "$(dirname "$0")"
template_commit=b3dbcf88a6883cc38fbc8d6d23056522bdffac83
deps_commit=78dc0a61b5f437ff6b54dc3998921fa599bea20f
if [[ ! -d .build-template/.git ]]; then
  git clone https://github.com/mit-cml/extension-template.git .build-template
fi
if [[ "$(git -C .build-template rev-parse HEAD)" != "$template_commit" ]]; then
  git -C .build-template fetch origin "$template_commit"
  git -C .build-template checkout --detach "$template_commit"
fi
git -C .build-template submodule update --init
if [[ "$(git -C .build-template/lib/appinventor rev-parse HEAD)" != "$deps_commit" ]]; then
  echo 'Unexpected App Inventor dependency version' >&2
  exit 1
fi
if [[ ! -f .build-template/lib/ant.jar ]]; then
  curl -L --fail https://repo.maven.apache.org/maven2/org/apache/ant/ant/1.10.15/ant-1.10.15.jar -o .build-template/lib/ant.jar
fi
if [[ ! -f .build-template/lib/ant-launcher.jar ]]; then
  curl -L --fail https://repo.maven.apache.org/maven2/org/apache/ant/ant-launcher/1.10.15/ant-launcher-1.10.15.jar -o .build-template/lib/ant-launcher.jar
fi
shasum -a 256 -c <<'CHECKSUMS'
763acda4a69588c9ea8817a952851ff0c2fc4bffa1d081c2565dc407f29d5794  .build-template/lib/ant.jar
5c8551990307a032336d98ddaed549a39a689f07d4d4c6b950601bf22b3d6a1b  .build-template/lib/ant-launcher.jar
CHECKSUMS
echo 'Build dependencies ready. Run: bash build.sh (JDK 8-17 and Python 3 required)'
