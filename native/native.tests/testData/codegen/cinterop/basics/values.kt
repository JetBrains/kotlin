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
