#!/bin/sh
set -eu
if command -v mvn >/dev/null 2>&1; then exec mvn "$@"; fi
DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
MVN_DIR="$DIR/.mvn/apache-maven-3.9.9"
if [ ! -x "$MVN_DIR/bin/mvn" ]; then
  command -v curl >/dev/null 2>&1 || { echo 'Install Maven or curl to bootstrap Maven.' >&2; exit 1; }
  ZIP="$DIR/.mvn/maven.zip"
  curl -fsSL --retry 3 https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip -o "$ZIP"
  (cd "$DIR/.mvn" && unzip -q "$ZIP" && rm "$ZIP")
fi
exec "$MVN_DIR/bin/mvn" "$@"
