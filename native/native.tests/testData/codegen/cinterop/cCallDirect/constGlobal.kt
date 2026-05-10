// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_FIRST_STAGE: Native:2.3
// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_SECOND_STAGE: Native:2.3
// ^^^ KT-79742 is fixed in 2.3.20-Beta1

// TARGET_BACKEND: NATIVE
// FREE_CINTEROP_ARGS: -Xccall-mode direct
// MODULE: cinterop
// FILE: lib.def
headers = lib.h

// FILE: lib.h
extern const long long constGlobal;

// FILE: lib.c
#include "lib.h"

const long long constGlobal = 0x100000000L;

// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

fun box(): String {
    val result = lib.constGlobal
    if (result != UInt.MAX_VALUE.toLong() + 1) return "FAIL: $result"

    return "OK"
}
