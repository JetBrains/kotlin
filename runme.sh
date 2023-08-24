#!/bin/bash

set -xueo pipefail

kotlin-native/dist/bin/generate-platform -t macos_arm64 -k dynamic_cache -c kotlin-native/dist/klib/cache/macos_arm64-gDYNAMIC

exit 0

kotlin-native/dist/bin/generate-platform -t macos_x64 -k dynamic_cache -c kotlin-native/dist/klib/cache/macos_x64-gDYNAMIC
kotlin-native/dist/bin/generate-platform -t macos_arm64 -k dynamic_cache -c kotlin-native/dist/klib/cache/macos_arm64-gDYNAMIC
#kotlin-native/dist/bin/generate-platform -t ios_arm32 -k dynamic_cache -c kotlin-native/dist/klib/cache/ios_arm32-gDYNAMIC
kotlin-native/dist/bin/generate-platform -t ios_arm64 -k dynamic_cache -c kotlin-native/dist/klib/cache/ios_arm64-gDYNAMIC
kotlin-native/dist/bin/generate-platform -t ios_x64 -k dynamic_cache -c kotlin-native/dist/klib/cache/ios_x64-gDYNAMIC
kotlin-native/dist/bin/generate-platform -t ios_simulator_arm64 -k dynamic_cache -c kotlin-native/dist/klib/cache/ios_simulator_arm64-gDYNAMIC
#kotlin-native/dist/bin/generate-platform -t watchos_arm32 -k dynamic_cache -c kotlin-native/dist/klib/cache/watchos_arm32-gDYNAMIC
kotlin-native/dist/bin/generate-platform -t watchos_arm64 -k dynamic_cache -c kotlin-native/dist/klib/cache/watchos_arm64-gDYNAMIC
#kotlin-native/dist/bin/generate-platform -t watchos_x86 -k dynamic_cache -c kotlin-native/dist/klib/cache/watchos_x86-gDYNAMIC
kotlin-native/dist/bin/generate-platform -t watchos_x64 -k dynamic_cache -c kotlin-native/dist/klib/cache/watchos_x64-gDYNAMIC
kotlin-native/dist/bin/generate-platform -t watchos_simulator_arm64 -k dynamic_cache -c kotlin-native/dist/klib/cache/watchos_simulator_arm64-gDYNAMIC
kotlin-native/dist/bin/generate-platform -t watchos_device_arm64 -k dynamic_cache -c kotlin-native/dist/klib/cache/watchos_device_arm64-gDYNAMIC
kotlin-native/dist/bin/generate-platform -t tvos_arm64 -k dynamic_cache -c kotlin-native/dist/klib/cache/tvos_arm64-gDYNAMIC
kotlin-native/dist/bin/generate-platform -t tvos_x64 -k dynamic_cache -c kotlin-native/dist/klib/cache/tvos_x64-gDYNAMIC
kotlin-native/dist/bin/generate-platform -t tvos_simulator_arm64 -k dynamic_cache -c kotlin-native/dist/klib/cache/tvos_simulator_arm64-gDYNAMIC
