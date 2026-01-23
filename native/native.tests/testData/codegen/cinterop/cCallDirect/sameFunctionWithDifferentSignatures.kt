// TARGET_BACKEND: NATIVE
// FREE_CINTEROP_ARGS: -Xccall-mode direct
// MODULE: cinterop1
// FILE: lib1.def
headers = lib1.h

// FILE: lib1.h
int sameFun(int);

// FILE: lib.c
#include "lib1.h"

int sameFun(int p) {
    return p + 2;
}

// MODULE: cinterop2
// FILE: lib2.def
headers = lib2.h

// FILE: lib2.h
unsigned sameFun(unsigned);

// MODULE: main(cinterop1, cinterop2)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

fun box(): String {
    val result1 = lib1.sameFun(5)
    if (result1 != 7) return "FAIL 1: $result1"

    val result2 = lib2.sameFun(6u)
    if (result2 != 8u) return "FAIL 2: $result2"

    return "OK"
}
