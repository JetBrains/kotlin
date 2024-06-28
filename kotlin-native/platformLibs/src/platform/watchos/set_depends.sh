#!/bin/bash
../../../../dist/bin/run_konan defFileDependencies \
  -target watchos_arm32 \
  -target watchos_arm64 \
  -target watchos_x64 \
  -target watchos_simulator_arm64 \
  -target watchos_device_arm64 \
  *.def

