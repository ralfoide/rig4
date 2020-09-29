#!/bin/bash
set -e
cd $(dirname "$0")
RIG4VER=$(grep "^version \"" ../build.gradle | cut -d \" -f 2)
RIG4JAR=../build/libs/rig4j-${RIG4VER}-all.jar

if [[ -z "$RIG4VER" ]]; then
    echo "Error: could not extract version from build.gradle"
    exit 1
fi

if [[ -n "$JAVA_HOME" && -d "$JAVA_HOME/bin" ]]; then
    PATH="$JAVA_HOME/bin:$PATH"
fi

BUILD=""
if [[ ! -f "$RIG4JAR" ]]; then
    BUILD="1"
else
    GITREV=$(git rev-parse --short HEAD)
    RIGREV=$(java -jar "$RIG4JAR" "$@" --version || echo "0")
    if [[ ! "$RIGREV" == *"$GITREV"* ]]; then
        BUILD="1"
    fi
fi

if [[ -n "$BUILD" ]]; then
    echo "Building $RIG4JAR ..."
    (
        cd ../..
        pwd
        ./gradlew --no-daemon fatJar
    )
    echo
fi

java -jar "$RIG4JAR" "$@"

