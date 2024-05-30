Currently working on getting llvm's clang++ working with the toolchain provided by zephyr.

After initial `publish` and `:kotlin-native:dist` 
* we need to build `:kotlin-native:zephyr_m55crossDist`. 
* Delete `kotlin-native/runtime/build/nativeStdlib/default/manifest`
* run the `:kotlin-native:dist` again.