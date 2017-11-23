#!/bin/bash
RIG4JAR=$(dirname "$0")/../build/libs/rig4j-1.0-SNAPSHOT-all.jar

if [[ ! -f "$RIG4JAR" ]]; then
    echo "Building $RIG4JAR ..."
    ( cd $(dirname "$0")/.. && ./gradlew fatJar )
    echo
fi

java -jar "$RIG4JAR" "$@"

