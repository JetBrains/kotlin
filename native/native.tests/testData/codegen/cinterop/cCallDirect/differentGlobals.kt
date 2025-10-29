// TARGET_BACKEND: NATIVE
// FREE_CINTEROP_ARGS: -Xccall-mode direct
// MODULE: cinterop
// FILE: lib.def
headers = lib.h

// FILE: lib.h
#include <stdbool.h>

extern bool differentGlobal1;
extern double differentGlobal2;
extern int* differentGlobal3;
extern enum E { ZERO, ONE, TWO } differentGlobal4;
extern struct S { int value; } differentGlobal5;

bool getDifferentGlobal1(void);
double getDifferentGlobal2(void);
int* getDifferentGlobal3(void);
int getDifferentGlobal4(void);

// FILE: lib.c
#include "lib.h"

bool differentGlobal1 = true;
double differentGlobal2 = 3.0;
int* differentGlobal3 = (int*)0x123;
enum E differentGlobal4 = TWO;
struct S differentGlobal5 = { 5 };

bool getDifferentGlobal1(void) {
    return differentGlobal1;
}

double getDifferentGlobal2(void) {
    return differentGlobal2;
}

int* getDifferentGlobal3(void) {
    return differentGlobal3;
}

int getDifferentGlobal4(void) {
    return differentGlobal4;
}

// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import kotlinx.cinterop.*
import lib.*

fun box(): String {
    if (differentGlobal1 != true) return "FAIL 1: $differentGlobal1"
    if (differentGlobal2 != 3.0) return "FAIL 2: $differentGlobal2"
    if (differentGlobal3.toLong() != 0x123L) return "FAIL 3: $differentGlobal3"
    if (differentGlobal4 != E.TWO) return "FAIL 4: $differentGlobal4"
    if (differentGlobal5.value != 5) return "FAIL 5: $differentGlobal5"

    differentGlobal1 = false
    differentGlobal2 = 2.0
    differentGlobal3 = 0x321L.toCPointer()
    differentGlobal4 = E.ONE

    if (getDifferentGlobal1() != false) return "FAIL 6: ${getDifferentGlobal1()}"
    if (getDifferentGlobal2() != 2.0) return "FAIL 7: ${getDifferentGlobal2()}"
    if (getDifferentGlobal3().toLong() != 0x321L) return "FAIL 8: ${getDifferentGlobal3()}"
    if (getDifferentGlobal4() != 1) return "FAIL 9: ${getDifferentGlobal4()}"

    return "OK"
}
