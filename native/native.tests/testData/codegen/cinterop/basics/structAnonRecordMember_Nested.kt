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

// Deep nesting
struct StructAnonRecordMember_Nested {
    int x;
    union { // implicitly aligned to 8 bytes due to int64, or 4 bytes at 32-bit arch
        int a[2];
        struct {
            int64_t b;
        };
    };
    char z;
    double y;
};

static struct StructAnonRecordMember_Nested retByValue_StructAnonRecordMember_Nested() {
    struct StructAnonRecordMember_Nested c = {
        .x = 37,
        .b = 42,
        .z = 'z',
        .y = 3.14
    };
    return c;
}

static int sendByValue_StructAnonRecordMember_Nested(struct StructAnonRecordMember_Nested c) {
    return c.a[0] + 2 * c.a[1];
}
#pragma clang diagnostic pop

// MODULE: main(cinterop)
// FILE: main.kt

@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)


import kotlinx.cinterop.*
import kotlin.test.*
import structAnonym.*

fun box(): String {
    retByValue_StructAnonRecordMember_Nested()
        .useContents {
            assertEquals(37, x)
            assertEquals(42, b)
            assertEquals('z', z.toInt().toChar())
            assertEquals(3.14, y)

            a[0] = 3
            a[1] = 5
            assertEquals(3 + 2*5, sendByValue_StructAnonRecordMember_Nested(this.readValue()))
        }

    return "OK"
}
