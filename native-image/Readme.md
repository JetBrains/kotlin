# Build native image

From repo root:
```
./native-image/buildNativeImage.sh

Usage: ./native-image/buildNativeImage.sh [--graalHome=PATH] [--updateReachabilityMetadata]
```

1. Builds dist
2. Builds kotlin-compiler-embeddable
3. Runs kotlin-compiler-embeddable on hello world to collect reachability-metadata if `--updateReachabilityMetadata` is specified
4. Re-builds kotlin-compiler-embeddable with new reachability-medatata
5. Builds native image

By default, assumes that `JAVA_HOME` is set to GraalVM installation. Tested on GraalVM 25.0.2.

The binary uses reachability metadata from [resources/META-INF/](../resources/META-INF/native-image/org/jetbrains/kotlin/kotlin-compiler-embeddable/reachability-metadata.json).
There are also several samples of reachability metadata collected on different test setups in [native-image/reachability](../native-image/reachability).

The build script will produce a `kotlinc-native` binary. The binary works just the same as a regular `kotlinc`, however,
it always requires `-Djava.home` property to be set when running. Example command:
```
./kotlinc-native -Djava.home=$JAVA_HOME A.kt
```

# Reachability

Collect reachability for command-line compiler
`JAVA_OPTS=-agentlib:native-image-agent=caller-filter-file=${KOTLIN_REPO:-.}/native-image/caller-filter.json,config-output-dir=./cfg ${KOTLIN_REPO:-.}/dist/kotlinc/bin/kotlinc A.kt -kotlin-home ${KOTLIN_REPO:-.}/dist/kotlinc/`

Collect trace for command-line compiler:
`JAVA_OPTS=-agentlib:native-image-agent=trace-output=./cfg/trace.json ${KOTLIN_REPO:-.}/dist/kotlinc/bin/kotlinc A.kt -kotlin-home ${KOTLIN_REPO:-.}/dist/kotlinc/`
