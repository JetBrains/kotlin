// TARGET_BACKEND: NATIVE
// FREE_CINTEROP_ARGS: -Xccall-mode direct
// MODULE: cinterop
// FILE: lib.def
headers = lib.h

// FILE: lib.h
int functionWithAsm(void) __asm("functionWithAsm0");

// FILE: lib.c
#include "lib.h"

int functionWithAsm(void) {
    return 222;
}

// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

fun box(): String {
    val result = lib.functionWithAsm()
    return if (result != 222) "FAIL: $result" else "OK"
}
