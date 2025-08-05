// TARGET_BACKEND: NATIVE
// MODULE: cinterop
// FILE: structAnonym.def
headers = test.h

// FILE: test.h
/*
 *  Test of return/send-by-value for aggregate type (struct or union) with anonymous inner struct or union member.
 *  Specific issues: alignment, packed, nested named and anon struct/union, other anon types (named field  of anon struct type; anon bitfield)
 */

struct StructAnonRecordMember_ExplicitAlignment {
    char a;
    struct {
        __attribute__((aligned(4)))
        char x;
    };
};

struct StructAnonRecordMember_ExplicitAlignment retByValue_StructAnonRecordMember_ExplicitAlignment();

// FILE: test.c
#include "test.h"

struct StructAnonRecordMember_ExplicitAlignment retByValue_StructAnonRecordMember_ExplicitAlignment() {
    struct StructAnonRecordMember_ExplicitAlignment t = {
        .a = 'a',
        .x = 'x'
    };
    return t;
}

// MODULE: main(cinterop)
// FILE: main.kt

@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)


import kotlinx.cinterop.*
import kotlin.test.*
import structAnonym.*

fun box(): String {
    retByValue_StructAnonRecordMember_ExplicitAlignment()
        .useContents {
            assertEquals('a', a.toInt().toChar())
            assertEquals('x', x.toInt().toChar())
        }

    return "OK"
}
