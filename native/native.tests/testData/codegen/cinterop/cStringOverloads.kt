// TARGET_BACKEND: NATIVE
// MODULE: cinterop
// FILE: cStringOverloads.def
headers = cStringOverloads.h

// FILE: cStringOverloads.h
const char* cstr_identity(const char* s);
int cstr_len(const char* s);


// FILE: cStringOverloads.c
#include <string.h>
#include "cStringOverloads.h"

const char* cstr_identity(const char* s) {
    return s;
}

int cstr_len(const char* s) {
    return s ? (int)strlen(s) : -1;
}

// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import cStringOverloads.*
import kotlinx.cinterop.*
import kotlin.test.*

fun box(): String {
    // String overload
    assertEquals(5, cstr_len("hello"))
    assertEquals(0, cstr_len(""))

    // CValuesRef overload
    val s = "hello world"
    val roundTripped = memScoped {
        val inPtr = s.cstr.ptr
        val outPtr = cstr_identity(inPtr)!!
        outPtr.toKString()
    }
    assertEquals(s, roundTripped)

    assertEquals(s.length, cstr_len(s.cstr))

    // `null` matches both overloads. `@LowPriorityInOverloadResolution` on the `CValuesRef`
    // variant resolves the ambiguity by choosing the `String?` overload, preserving the
    // pre-dual-overload behavior.
    assertEquals(-1, cstr_len(null))

    // `::cstr_len` must resolve to the `String?` overload to preserve
    // the behavior that existed before the dual-overload feature: calling the reference
    // with a Kotlin String literal must compile. Requires `@LowPriorityInOverloadResolution`
    // on the `CValuesRef` overload, not on the `String?` one.
    val ref = ::cstr_len
    assertEquals(5, ref("hello"))
    val ref2: (CValuesRef<ByteVar>?) -> Int = ::cstr_len
    assertEquals(5, ref2("hello".cstr))

    return "OK"
}
