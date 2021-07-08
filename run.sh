#!/bin/bash
set -e
threads=$1
repo="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
jdk=/Library/Java/JavaVirtualMachines/zulu-11.jdk
konanJar="$repo/kotlin-native/dist/konan/lib/kotlin-native.jar"

pids=""

for i in $(seq 1 $threads); do
    KONAN_USE_INTERNAL_SERVER=1 "$jdk/Contents/Home/bin/java" \
        "-Dkonan.home=$repo/kotlin-native/dist" \
        "-XX:TieredStopAtLevel=1" \
        "-Xmx6G" \
        "-ea" \
        "-cp" \
        "$repo/kotlin-native/dist/konan/lib/trove4j.jar:$konanJar" \
        "org.jetbrains.kotlin.cli.utilities.MainKt" \
        "cinterop" \
        "-o" \
        "$repo/out/org.jetbrains.kotlin.native.platform.WebKit${i}.klib" \
        "-target" \
        "ios_arm64" \
        "-def" \
        "$repo/kotlin-native/platformLibs/src/platform/ios/WebKit.def" &

    pids+=" $!"
done

for p in $pids; do
    wait $p
done
