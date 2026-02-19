// TARGET_BACKEND: NATIVE
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
