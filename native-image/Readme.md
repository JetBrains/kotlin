From repo root:
```
./native-image/buildNativeImage.sh

Usage: ./native-image/buildNativeImage.sh [--graalHome=PATH] [--updateReachabilityMetadata]
```

1. Builds dist
2. Builds kotlin-compiler-embeddable
3. Runs kotlin-compiler-embeddable on hello world to collect reachability-metadata
4. Re-builds kotlin-compiler-embeddable with new reachability-medatata
5. Builds native image


Collect reachability for command-line compiler
`JAVA_OPTS=-agentlib:native-image-agent=caller-filter-file=${KOTLIN_REPO:-.}/native-image/caller-filter.json,config-output-dir=./cfg ${KOTLIN_REPO:-.}/dist/kotlinc/bin/kotlinc A.kt -kotlin-home ${KOTLIN_REPO:-.}/dist/kotlinc/`
Collect trace for command-line compiler:
`JAVA_OPTS=-agentlib:native-image-agent=trace-output=./cfg/trace.json ${KOTLIN_REPO:-.}/dist/kotlinc/bin/kotlinc A.kt -kotlin-home ${KOTLIN_REPO:-.}/dist/kotlinc/`
