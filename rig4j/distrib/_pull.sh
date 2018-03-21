#!/bin/bash
set -e
cd $(dirname "$0")/../..
(   cd LibUtils
    echo -n "LibUtils: "
    git pull 
)
echo -n "Rig4j: "
git pull
