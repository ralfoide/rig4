#!/bin/bash
if [[ -z "$GOPATH" ]]; then . ./_setup.sh; fi
if [[ ! -d "bin" ]]; then mkdir -p bin; fi
function _mv() { for i in $* ; do if [[ -f "$i" ]]; then mv -vf "$i" bin/ ; fi; done; }
go build -v $@ src/rig4.go && _mv rig4 rig4.exe

