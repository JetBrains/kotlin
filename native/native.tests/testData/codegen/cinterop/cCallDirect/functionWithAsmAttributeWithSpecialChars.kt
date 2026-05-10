// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_SECOND_STAGE: Native:2.3
// ^^^ KT-81548 is fixed in 2.3.20-Beta1

// TARGET_BACKEND: NATIVE
// FREE_CINTEROP_ARGS: -Xccall-mode direct
// MODULE: cinterop
// FILE: lib.def
headers = lib.h

// FILE: lib.h
int functionWithSpecialChars(void) __asm("function😅\n\"\\");

// FILE: lib.c
#include "lib.h"

int functionWithSpecialChars(void) {
    return 2;
}

// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

fun box(): String {
    val result = lib.functionWithSpecialChars()
    return if (result != 2) "FAIL: $result" else "OK"
}
