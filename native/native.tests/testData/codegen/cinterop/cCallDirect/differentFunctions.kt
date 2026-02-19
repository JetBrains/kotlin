// TARGET_BACKEND: NATIVE
// FREE_CINTEROP_ARGS: -Xccall-mode direct
// MODULE: cinterop
// FILE: lib.def
headers = lib.h

// FILE: lib.h
int differentFun1(int);
int differentFun2(int);
long long differentFun3(void);

// FILE: lib.c
#include "lib.h"

int differentFun1(int p) {
    return p - 1;
}

int differentFun2(int p) {
    return p + 1;
}

long long differentFun3(void) {
    return 111;
}

// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

fun box(): String {
    val result1 = lib.differentFun1(1)
    if (result1 != 0) return "FAIL 1: $result1"

    val result2 = lib.differentFun2(1)
    if (result2 != 2) return "FAIL 2: $result2"

    val result3 = lib.differentFun3()
    if (result3 != 111L) return "FAIL 3: $result3"

    return "OK"
}
