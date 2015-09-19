#!/bin/bash
if [[ -z "$GOPATH" ]]; then . ./_setup.sh; fi
go test -v src/rig4/*.go
