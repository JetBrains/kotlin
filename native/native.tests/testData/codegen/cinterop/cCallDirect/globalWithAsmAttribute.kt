// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_FIRST_STAGE: Native:2.3
// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_SECOND_STAGE: Native:2.3
// ^^^ KT-79742 is fixed in 2.3.20-Beta1

// TARGET_BACKEND: NATIVE
// FREE_CINTEROP_ARGS: -Xccall-mode direct
// MODULE: cinterop
// FILE: lib.def
headers = lib.h

// FILE: lib.h
extern unsigned long long globalWithAsm __asm("globalWithAsm0");

// FILE: lib.c
#include "lib.h"

unsigned long long globalWithAsm = 333ULL;

// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import lib.*

fun box(): String {
    return if (globalWithAsm != 333uL) "FAIL: $globalWithAsm" else "OK"
}
