// TARGET_BACKEND: NATIVE

// FILECHECK_STAGE: CStubs
// FREE_CINTEROP_ARGS: -Xccall-mode both
// FREE_COMPILER_ARGS: -Xbinary=cCallMode=indirect

// MODULE: cinterop
// FILE: lib.def
headers = lib.h

// FILE: lib.h
int foo(int);

// FILE: lib.c
#include "lib.h"

int foo(int p) {
    return p + 1;
}

// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

// CHECK: knifunptr
// CHECK-NOT: call i32 @{{foo|"\\01_foo"}}
fun box(): String {
    val result = lib.foo(4)
    return if (result != 5) "FAIL: $result" else "OK"
}
