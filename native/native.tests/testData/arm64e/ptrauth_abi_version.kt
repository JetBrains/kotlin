// The module is marked with the pointer authentication ABI of user space, version 0, as Clang marks its modules.
// Without it, the object file of a module that doesn't contain the runtime, such as a cache, has no ABI version,
// and libtool leaves it out of static libraries.
// CHECK: !{i32 6, !"ptrauth.abi-version", ![[VERSIONS:[0-9]+]]}
// CHECK: ![[VERSIONS]] = !{![[VERSION:[0-9]+]]}
// CHECK: ![[VERSION]] = !{i32 0, i1 false}

fun foo() = 42
