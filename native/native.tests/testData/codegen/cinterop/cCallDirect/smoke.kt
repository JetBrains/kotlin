// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_FIRST_STAGE: Native:2.3
// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_SECOND_STAGE: Native:2.3
// ^^^ KT-79742 is fixed in 2.3.20-Beta1

// TARGET_BACKEND: NATIVE
// FREE_CINTEROP_ARGS: -Xccall-mode direct
// MODULE: cinterop
// FILE: lib.def
headers = lib.h

// FILE: lib.h
int foo(int);
extern short bar;
short getBar(void);

// FILE: lib.c
#include "lib.h"

int foo(int p) {
    return p + 1;
}

short bar = 42;
short getBar(void) {
    return bar;
}

// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import lib.*

fun box(): String {
    val result1 = foo(4)
    if (result1 != 5) return "FAIL 1: $result1"

    val result2 = bar
    if (result2.toInt() != 42) return "FAIL 2: $result2"

    bar = 7
    val result3 = getBar()
    if (result3.toInt() != 7) return "FAIL 3: $result3"

    val result4 = bar
    if (result4.toInt() != 7) return "FAIL 4: $result4"

    return "OK"
}
