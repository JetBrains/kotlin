// TARGET_BACKEND: NATIVE
// FREE_CINTEROP_ARGS: -Xccall-mode direct
// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_FIRST_STAGE: Native:2.3
// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_SECOND_STAGE: Native:2.3
// ^^^ KT-79742 is fixed in 2.3.20-Beta1

// MODULE: cinterop
// FILE: lib.def
headers = lib.h

// FILE: lib.h
extern char globalWithSpecialChars __asm("global🤷‍♂️$\n\"\\");

// FILE: lib.c
#include "lib.h"

char globalWithSpecialChars = '`';

// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import lib.*

fun box(): String {
    return if (globalWithSpecialChars != '`'.code.toByte()) "FAIL: $globalWithSpecialChars" else "OK"
}
