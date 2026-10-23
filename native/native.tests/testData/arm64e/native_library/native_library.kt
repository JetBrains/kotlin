// Bitcode passed with -native-library is linked into an arm64e binary if it is compiled for arm64e.
// CHECK: define {{.*}}i32 @nativeLibraryAnswer()

fun foo() = 42
