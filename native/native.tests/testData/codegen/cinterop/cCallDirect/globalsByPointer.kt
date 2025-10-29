// TARGET_BACKEND: NATIVE
// FREE_CINTEROP_ARGS: -Xccall-mode direct
// MODULE: cinterop
// FILE: lib.def
headers = lib.h

// FILE: lib.h
extern int intArray[10];
extern struct S { char c; } structArray[5];

int* getIntArrayPtr(void);
struct S* getStructArrayPtr(void);

// FILE: lib.c
#include "lib.h"

int intArray[10] = { 9, 8, -1, 6, 5, 4, 3, 2, 1, 0 };
struct S structArray[5] = {{ 'H' }, { 'e' }, { 'l' }, { 'l' }, { 'l' }};

int* getIntArrayPtr(void) {
    return &intArray;
}

struct S* getStructArrayPtr(void) {
    return &structArray;
}

// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import kotlinx.cinterop.*
import lib.*

fun box(): String {
    if (intArray != getIntArrayPtr()) return "FAIL 1"
    if (structArray != getStructArrayPtr()) return "FAIL 2"

    intArray[2] = 7
    val result1 = (0..<10).map { intArray[it] }
    if (result1 != (9 downTo 0).toList()) return "FAIL 3: $result1"

    structArray[4].c = 'o'.code.toByte()
    val result2 = (0..<5).map { structArray[it].c.toInt().toChar() }.joinToString("")
    if (result2 != "Hello") return "FAIL 4: $result2"

    return "OK"
}
