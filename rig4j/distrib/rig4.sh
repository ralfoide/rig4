#!/bin/bash
set -e
cd $(dirname "$0")
RIG4JAR=../build/libs/rig4j-1.0-SNAPSHOT-all.jar

if [[ -n "$JAVA_HOME" && -d "$JAVA_HOME/bin" ]]; then
    PATH="$JAVA_HOME/bin:$PATH"
fi

BUILD=""
if [[ ! -f "$RIG4JAR" ]]; then
    BUILD="1"
else
    GITREV=$(git rev-parse --short HEAD)
    RIGREV=$(java -jar "$RIG4JAR" --version || echo "0")
    if [[ ! "$RIGREV" == *"$GITREV"* ]]; then
        BUILD="1"
    fi
fi

if [[ -n "$BUILD" ]]; then
    echo "Building $RIG4JAR ..."
    (
        cd ..
        ./gradlew --no-daemon fatJar
    )
    echo
fi

java -jar "$RIG4JAR" "$@"

