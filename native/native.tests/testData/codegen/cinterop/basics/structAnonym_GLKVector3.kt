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

union _GLKVector3
{
    struct { float x, y, z; };
    struct { float r, g, b; };
    struct { float s, t, p; };
    float v[3];
};

static union _GLKVector3 get_GLKVector3() {
    union _GLKVector3 ret = {{1, 2, 3}};
    return ret;
}

static float hash_GLKVector3(union _GLKVector3 x) {
    union _GLKVector3 ret = {{1, 2, 3}};
    return x.x + 2.0f * x.y + 4.0f * x.z;
}

#pragma clang diagnostic pop

// MODULE: main(cinterop)
// FILE: main.kt

@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)


import kotlinx.cinterop.*
import kotlin.test.*
import structAnonym.*

fun box(): String {
    get_GLKVector3().useContents {
        assertEquals(1.0f, x)
        assertEquals(2.0f, g)
        assertEquals(3.0f, p)
        r = 0.1f
        g = 0.2f
        b = 0.3f
        assertEquals(v[0], r)
        assertEquals(v[1], g)
        assertEquals(v[2], b)

        val ret = hash_GLKVector3(this.readValue())
        assertEquals(s + 2f * t + 4f * p , ret)
    }

    return "OK"
}
