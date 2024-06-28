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

// Nested struct may be packed too
#pragma pack(2)
struct StructAnonRecordMember_Packed2 {
    char first;
    union {
        int a[2];
        union { char c1; int c2; };
        struct { char b1; int64_t b2; };
    };
    char second;
    struct {
        char x;
        struct { int64_t b11, b12; } Y2;
        int32_t f;
    } __attribute__((aligned(16)));
    char last;
} __attribute__ ((packed));
#pragma pack()

#define INIT(T, x) 	struct T x = \
{ \
    .first = 'a', \
    .b1 = 'b', \
    .b2 = 42, \
    .second = 's', \
    .last = 'z', \
    .f = 314, \
    .Y2 = {11, 12} \
}

static struct StructAnonRecordMember_Packed2 retByValue_StructAnonRecordMember_Packed2() {
    INIT(StructAnonRecordMember_Packed2, c);
    return c;
}

#pragma clang diagnostic pop

// MODULE: main(cinterop)
// FILE: main.kt

@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)


import kotlinx.cinterop.*
import kotlin.test.*
import structAnonym.*

fun box(): String {
    retByValue_StructAnonRecordMember_Packed2()
        .useContents{
            assertEquals('a', first.toInt().toChar())
            assertEquals('s', second.toInt().toChar())
            assertEquals('z', last.toInt().toChar())
            assertEquals('b', b1.toInt().toChar())
            assertEquals(42L, b2)
            assertEquals(314, f)
            assertEquals(11L, Y2.b11)
        }

    return "OK"
}
