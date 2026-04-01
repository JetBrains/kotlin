From repo root:
```
./native-image/buildNativeImage.sh \
    $GRAAL_HOME \
    native-image/reachability/reachability-metadata-new.json \
    native-image/reachability/reachability-metadata-cli.json
```

1. Splits the reachability metadata into separate json configs in `native-image/current-config`
2. Builds kotlin-compiler-embeddable.jar
3. Builds native image


Collect reachability for command-line compiler
`JAVA_OPTS=-agentlib:native-image-agent=caller-filter-file=${KOTLIN_REPO:-.}/native-image/caller-filter.json,config-output-dir=./cfg ${KOTLIN_REPO:-.}/dist/kotlinc/bin/kotlinc A.kt -kotlin-home ${KOTLIN_REPO:-.}/dist/kotlinc/`
Collect trace for command-line compiler:
`JAVA_OPTS=-agentlib:native-image-agent=trace-output=./cfg/trace.json ${KOTLIN_REPO:-.}/dist/kotlinc/bin/kotlinc A.kt -kotlin-home ${KOTLIN_REPO:-.}/dist/kotlinc/`
