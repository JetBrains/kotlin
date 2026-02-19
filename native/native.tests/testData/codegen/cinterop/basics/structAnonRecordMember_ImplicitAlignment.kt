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

// trivial alignment: member is already aligned, but this implies implicit larger alignment of the root struct
struct StructAnonRecordMember_ImplicitAlignment {
    int32_t a[4];
    struct {
        int b  __attribute__((aligned(16)));
    };
};

static struct StructAnonRecordMember_ImplicitAlignment retByValue_StructAnonRecordMember_ImplicitAlignment() {
    struct StructAnonRecordMember_ImplicitAlignment t = {
        .a = {1,2,3,4},
        .b = 42
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
    retByValue_StructAnonRecordMember_ImplicitAlignment()
        .useContents {
            assertEquals(1, a[0])
            assertEquals(4, a[3])
            assertEquals(42, b)
        }

    return "OK"
}
