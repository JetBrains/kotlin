// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_SECOND_STAGE: Native:2.3
// ^^^ KT-81017 is fixed in 2.3.20-Beta1

// TARGET_BACKEND: NATIVE
// FREE_CINTEROP_ARGS: -Xccall-mode direct
// MODULE: cinterop
// FILE: lib.def
headers = lib.h

// FILE: lib.h
int functionWithDollar(void) __asm("function$withDollar");

// FILE: lib.c
#include "lib.h"

int functionWithDollar(void) {
    return 1;
}

// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

fun box(): String {
    val result = lib.functionWithDollar()
    return if (result != 1) "FAIL: $result" else "OK"
}
