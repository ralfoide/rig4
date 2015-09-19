#!/bin/bash
if [[ -z "$GOPATH" ]]; then . ./_setup.sh; fi
if [[ ! -d "bin" ]]; then mkdir -p bin; fi
go build -v $@ src/rig4.go && mv -v rig4* bin/

