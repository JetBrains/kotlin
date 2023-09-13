#!/bin/bash

kotlin-native/tools/scripts/update_apple_frameworks.sh watchos "$(realpath kotlin-native)"
kotlin-native/tools/scripts/update_apple_frameworks.sh osx "$(realpath kotlin-native)"
kotlin-native/tools/scripts/update_apple_frameworks.sh tvos "$(realpath kotlin-native)"
kotlin-native/tools/scripts/update_apple_frameworks.sh ios "$(realpath kotlin-native)"
