#!/bin/bash
if [[ -z "$GOPATH" ]]; then . ./_setup.sh; fi
set -e
for dir in config rig4 utils; do
    echo
    echo "[[[ $dir ]]]"
    echo
    go test -v src/$dir/*.go
    echo
done
