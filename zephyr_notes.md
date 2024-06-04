# Things you need to install (please add to the list if you encounter any missing dependencies)
* brew install llvm

# How to get things building

NOTE - all the steps below assumes you're in the root dir of `kn_exp`

1. By default, Kotlin uses a prebuilt image of Kotlin compiler downloaded from JetBrain's server to compile everything. However, that image does not contain the Zephyr target. So we need to build a new Kotlin compiler and tell `gradle` to use it instead.
2. create a file called `local.properties` - this allows you to override various behaviors of `gradle`
3. add `kotlin.native.enabled=true` to `local.properties` to enable KN
4. run `./gradlew publish`, it will take a while
5. now add `bootstrap.local=true` to `local.properties`, which will tell `gradle` to use the compiler we just built instead.
6. run `./gradlew :kotlin-native:zephyr_m55crossDist`, which will build the `zephyr_m55` related binaries.
7. when you re run step 6, it sometimes complain about `trove4j.jar` permission denied, simply run `chmod +w kotlin-native/dist/konan/lib/trove4j.jar` once to reset the permission. This is a known issue that has been resolved in mainline.

# How to test the kotlin native compiler

run `./zephyr_prj/scripts/kn_compile.sh`
This command invokes the compiler you just built from previous step against a very simple kotlin source code (`~/kn_exp/zephyr_proj/kotlin/src/nativeMain/kotlin/lib.kt`) and produces a couple files under `~/kn_exp/zephyr_proj/out/kn`
* `api.bc` - this contains the LLVM bitcode for `lib.kt` mentioned above
* `api.cpp` - not sure why this one is produced, but it's basically the c++ form of the `api.bc` file
* `api.ll` - this is the result of running `llvm-dis` on `api.bc` to turn that into human readable llvm instructions
* `out.bc` - this is the bitcode for KN runtime
* `out.Codegen.ll` - this is the decoded llvm code after KN runs code-gen
* `out.ll` - this is result after we run `llvm-dis` on `out.bc`