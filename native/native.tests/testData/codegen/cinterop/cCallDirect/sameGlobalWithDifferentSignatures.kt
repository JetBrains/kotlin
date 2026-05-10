// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_FIRST_STAGE: Native:2.3
// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_SECOND_STAGE: Native:2.3
// ^^^ KT-79742 is fixed in 2.3.20-Beta1

// TARGET_BACKEND: NATIVE
// FREE_CINTEROP_ARGS: -Xccall-mode direct
// MODULE: cinterop1
// FILE: lib1.def
headers = lib1.h

// FILE: lib1.h
extern int sameGlobal;

// FILE: lib.c
#include "lib1.h"

int sameGlobal = 101;

// MODULE: cinterop2
// FILE: lib2.def
headers = lib2.h

// FILE: lib2.h
extern unsigned sameGlobal;

// MODULE: main(cinterop1, cinterop2)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

fun box(): String {
    val result1 = lib1.sameGlobal
    if (result1 != 101) return "FAIL 1: $result1"

    val result2 = lib2.sameGlobal
    if (result2 != 101u) return "FAIL 2: $result2"

    lib2.sameGlobal = 5u
    val result3 = lib1.sameGlobal
    if (result3 != 5) return "FAIL 3: $result3"

    lib1.sameGlobal = 6
    val result4 = lib2.sameGlobal
    if (result4 != 6u) return "FAIL 4: $result4"

    return "OK"
}
