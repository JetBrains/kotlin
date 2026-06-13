// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_SECOND_STAGE: Native:2.3
// ^^^ KT-86026 TODO: Rework testdata to move functions/globals definitions from .def/.h into separate source files

// TARGET_BACKEND: NATIVE
// MODULE: cinterop
// FILE: structAnonym.def
---

/*
 *  Test of return/send-by-value for aggregate type (struct or union) with anonymous inner struct or union member.
 *  Specific issues: alignment, packed, nested named and anon struct/union, other anon types (named field  of anon struct type; anon bitfield)
 */

#include <inttypes.h>

#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Winitializer-overrides"

struct StructAnonRecordMember_ExplicitAlignment {
    char a;
    struct {
        __attribute__((aligned(4)))
        char x;
    };
};

static struct StructAnonRecordMember_ExplicitAlignment retByValue_StructAnonRecordMember_ExplicitAlignment() {
    struct StructAnonRecordMember_ExplicitAlignment t = {
        .a = 'a',
        .x = 'x'
    };
    return t;
}
#pragma clang diagnostic pop

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
