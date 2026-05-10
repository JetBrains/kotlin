// TARGET_BACKEND: NATIVE
// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_SECOND_STAGE: Native:2.3
// ^^^ Native backend v.2.3.0 has issue KT-81538, fixed only in 2.3.20-Beta1. So, a test `current frontend and cinterop + 2.3.0 backend` expectedly fails
// MODULE: cinterop
// FILE: lib.def
headers = lib.h

// FILE: lib.h
int foo$bar(void);

// FILE: lib.c
#include "lib.h"

int foo$bar(void) {
    return 1;
}

// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

fun box(): String {
    val result = lib.`foo$bar`()
    return if (result != 1) "FAIL: $result" else "OK"
}
