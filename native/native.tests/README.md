## Running tests

* To run all tests, use `./gradlew :native:native.tests:test`. Please note, this Gradle task is available only in development environment and it not available at CI server.
* To execute certain tests only, use the appropriate Gradle tasks. Example: `./gradlew :native:native.tests:codegenBoxTest`
* To execute InteropIndexer tests for all targets, use:
```bash
for TARGET in android_x64 android_x86 android_arm32 android_arm64 \
              ios_arm32 ios_arm64 ios_x64 ios_simulator_arm64 \
              linux_x64 linux_arm64 linux_arm32_hfp linux_mips32 linux_mipsel32 \
              macos_x64 macos_arm64 \
              mingw_x86 mingw_x64 \
              tvos_arm64 tvos_x64 tvos_simulator_arm64 \
              wasm32 \
              watchos_arm32 watchos_arm64 watchos_x86 watchos_x64 watchos_simulator_arm64 watchos_device_arm64
do
  echo $TARGET             
  ./gradlew :native:native.tests:interopIndexerTest -Pkotlin.internal.native.test.target=$TARGET
done
```
* To re-generate tests, use `./gradlew :native:native.tests:generateTests`

For more details see [Testing](../../kotlin-native/HACKING.md#Testing).
