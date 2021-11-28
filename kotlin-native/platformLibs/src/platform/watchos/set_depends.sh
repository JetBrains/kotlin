#!/bin/bash
../../../../dist/bin/run_konan defFileDependencies \
  -target watchos_arm32 \
  -target watchos_x86 \
  -target watchos_arm64 \
  -target watchos_x64 \
  *.def

