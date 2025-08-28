// TARGET_BACKEND: NATIVE

// IGNORE_BACKEND: NATIVE
// FREE_CINTEROP_ARGS: -Xccall-mode indirect
// FREE_COMPILER_ARGS: -Xbinary=cCallMode=direct

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

fun box(): String {
    val result = lib.foo(4)
    return if (result != 5) "FAIL: $result" else "OK"
}
