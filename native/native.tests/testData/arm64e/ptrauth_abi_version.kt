// The module is marked with pointer authentication ABI version 0 for user space, as Clang marks its modules. Without
// the mark, a module that doesn't contain the runtime, such as a cache, has no ABI version in its object file, and
// libtool leaves that object file out of static libraries.
// CHECK: !{i32 6, !"ptrauth.abi-version", ![[VERSIONS:[0-9]+]]}
// CHECK: ![[VERSIONS]] = !{![[VERSION:[0-9]+]]}
// CHECK: ![[VERSION]] = !{i32 0, i1 false}

fun foo() = 42
