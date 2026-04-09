# Build native image

From repo root:
```
./native-image/buildNativeImage.sh

Usage: ./native-image/buildNativeImage.sh [--graalHome=PATH] [--updateReachabilityMetadata]
```

**This script requires GraalVM with Crema installed. Crema is available in the [early access builds of GraalVM](https://github.com/graalvm/oracle-graalvm-ea-builds).**
Tests were conducted using `graalvm-jdk-25e1-25.0.2-ea.20`.

1. Builds dist
2. Builds kotlin-compiler-embeddable
3. Runs kotlin-compiler-embeddable on hello world to collect reachability-metadata
4. Re-builds kotlin-compiler-embeddable with new reachability-medatata
5. Builds native image

# Run native image with kotlin-serialization-compiler-plugin

```
./kotlinc-native -Djava.home=$JAVA_HOME -Xplugin=dist/kotlinc/lib/kotlin-serialization-compiler-plugin.jar A.kt
```

This currently fails with
```
Fatal error: Method pointer invoked on a method that was not compiled because it was not seen as invoked by the static analysis nor was it directly registered for compilation
```

# Collect reachability and trace data

Collect reachability for command-line compiler
`JAVA_OPTS=-agentlib:native-image-agent=caller-filter-file=${KOTLIN_REPO:-.}/native-image/caller-filter.json,config-output-dir=./cfg ${KOTLIN_REPO:-.}/dist/kotlinc/bin/kotlinc A.kt -kotlin-home ${KOTLIN_REPO:-.}/dist/kotlinc/`
Collect trace for command-line compiler:
`JAVA_OPTS=-agentlib:native-image-agent=trace-output=./cfg/trace.json ${KOTLIN_REPO:-.}/dist/kotlinc/bin/kotlinc A.kt -kotlin-home ${KOTLIN_REPO:-.}/dist/kotlinc/`
