#!/bin/bash
if [[ -z "$GOPATH" ]]; then . ./_setup.sh; fi
set -e
for dir in config utils rig4/doc rig4/experimental; do
    echo
    echo "[[[ $dir ]]]"
    echo
    go test -v src/$dir/*.go
    echo
done
