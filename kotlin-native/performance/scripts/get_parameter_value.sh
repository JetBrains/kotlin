#!/usr/bin/env bash

ALL_PARAMS="$konanCompilerArgs"
MEMORY_MODEL=$1

if [ $MEMORY_MODEL = "legacy" ]; then
  if [ "$ALL_PARAMS" != "" ]; then
    ALL_PARAMS=" $ALL_PARAMS"
  fi
  ALL_PARAMS="-memory-model strict$ALL_PARAMS"
fi
if [ "$ALL_PARAMS" != "" ]; then
  ALL_PARAMS="-PcompilerArgs=$ALL_PARAMS"
fi

echo "##teamcity[setParameter name='env.konanCompilerArgs' value='$ALL_PARAMS']"
