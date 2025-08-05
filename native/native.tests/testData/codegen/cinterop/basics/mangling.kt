// TARGET_BACKEND: NATIVE
// MODULE: cinterop
// FILE: mangling.def
headers = test.h

// FILE: test.h
// test mangling of special names

enum _Companion {Companion, Any};
extern enum _Companion companion;

// FILE: test.c
#include "test.h"

enum _Companion companion = Companion;

// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
import kotlinx.cinterop.*
import kotlin.test.*
import mangling.*

fun box(): String {
    companion = _Companion.`Companion$`
    assertEquals(_Companion.`Companion$`, companion)

    return "OK"
}

