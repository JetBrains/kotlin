
```
./native-image/buildNativeImage.sh \
    $GRAAL_HOME \
    ./kotlin/native-image/reachability/reachability-metadata-new.json \
    ./kotlin/native-image/reachability/reachability-metadata-new.json
```

1. Splits the reachability metadata into separate json configs in `native-image/current-config`
2. Builds kotlin-compiler-embeddable.jar
3. Builds native image