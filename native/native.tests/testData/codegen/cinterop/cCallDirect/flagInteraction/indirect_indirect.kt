// TARGET_BACKEND: NATIVE

// FILECHECK_STAGE: CStubs
// FREE_CINTEROP_ARGS: -Xccall-mode indirect
// FREE_COMPILER_ARGS: -Xbinary=cCallMode=indirect

// MODULE: cinterop
// FILE: lib.def
headers = lib.h

// FILE: lib.h
int foo(int);
extern int bar;

// FILE: lib.c
#include "lib.h"

int foo(int p) {
    return p + 1;
}

int bar = 2;

// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import lib.*

// CHECK: knifunptr
// CHECK-NOT: call i32 @{{foo|"\\01_foo"}}
// CHECK-NOT: load i32, ptr @{{bar|"\\01_bar"}}
fun box(): String {
    val result = foo(4)
    if (result != 5) return "FAIL 1: $result"

    if (bar != 2) return "FAIL 2: $bar"

    return "OK"
}
