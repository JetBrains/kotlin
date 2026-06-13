// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_SECOND_STAGE: Native:2.3
// ^^^ KT-86026 TODO: Rework testdata to move functions/globals definitions from .def/.h into separate source files

// TARGET_BACKEND: NATIVE
// MODULE: cinterop
// FILE: cvalues.def
---
_Bool isNullString(const char* str) {
    return str == (const char*)0;
}

typedef const short* LPCWSTR;

_Bool isNullWString(LPCWSTR str) {
    return str == (LPCWSTR)0;
}

// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import kotlinx.cinterop.*
import kotlin.test.*
import cvalues.*

fun box(): String {
    assertTrue(isNullString(null))
    assertTrue(isNullWString(null))
    assertFalse(isNullString("a"))
    assertFalse(isNullWString("b"))

    return "OK"
}
