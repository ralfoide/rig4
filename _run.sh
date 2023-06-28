#!/usr/bin/bash
if [[ -z "$1" ]]; then
  ./gradlew run --console=plain
else
  ./gradlew fatJar --console=plain && java -jar build/libs/rig4-0.1-SNAPSHOT-all.jar $@
fi
