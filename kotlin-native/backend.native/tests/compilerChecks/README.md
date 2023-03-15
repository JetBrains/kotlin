These tests aren't invoked automatically so far because there is no mechanism of checking compilation errors yet.
To run tests manually, 
- build platformLibs: `./gradlew :kotlin-native:platformLibs:macos_arm64Install` or `./gradlew :kotlin-native:platformLibs:macos_x64Install`
- run `runtests.sh` as described below. 

Known issues: missing expected error for tests:
K1: `kotlin-native/backend.native/tests/compilerChecks/runtests.sh -language-version 1.9`
- t51.kt: missing error
- t63.kt: missing error
- t64.kt: missing error

K2: `kotlin-native/backend.native/tests/compilerChecks/runtests.sh -language-version 2.0`
- t36.kt: missing `error: native interop types constructors must not be called directly` 
          with literal `42`:  wrong `error: symbol @GCUnsafeCall(...) fun malloc(size: Long, align: Int): NativePtr is invisible`, since "KT-56583 K1: Implement opt-in for integer cinterop conversions" is not implemented in K2
          with literal `42u`: wrong `error: type kotlinx.cinterop.CPointer<out kotlinx.cinterop.CPointed>?  of return value is not supported here: doesn't correspond to any C type`
- t51.kt: missing error. K1 has no error as well
- t54.kt: missing `error: no spread elements allowed here`
- t55.kt: missing `error: all elements of binary blob must be constants`
- t56.kt: missing `error: incorrect value for binary data: 1000`
- t57.kt: missing `error: expected at least one element`
- t63.kt: missing error. K1 has no error as well
- t64.kt: missing error. K1 has no error as well
