// TARGET_BACKEND: NATIVE
// MODULE: cinterop
// FILE: cunion.def
---
typedef union {
    short s;
    long long ll;
} BasicUnion;

typedef struct {
    union {
        int i;
        float f;
    } as;
} StructWithUnion;

typedef union {
    unsigned int i : 31;
    unsigned char b : 1;
} Packed;

// MODULE: main(cinterop)
// FILE: main.kt

@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class, kotlinx.cinterop.ExperimentalForeignApi::class)

import cunion.*
import kotlinx.cinterop.*
import kotlin.native.*
import kotlin.test.*

fun box(): String {
    memScoped {
        val basicUnion = alloc<BasicUnion>()
        for (value in Short.MIN_VALUE..Short.MAX_VALUE) {
            basicUnion.ll = value.toLong()
            val expected =  if (Platform.isLittleEndian) {
                value
            } else {
                value.toLong() ushr (Long.SIZE_BITS - Short.SIZE_BITS)
            }
            assertEquals(expected.toShort(), basicUnion.s)
        }
    }
    memScoped {
        val struct = alloc<StructWithUnion>()
        struct.`as`.i = Float.NaN.toRawBits()
        assertEquals(Float.NaN, struct.`as`.f)
    }
    memScoped {
        val union = alloc<Packed>()
        union.b = 1u
        var expected = if (Platform.isLittleEndian) {
            1u
        } else {
            1u shl (Int.SIZE_BITS - Byte.SIZE_BITS)
        }
        assertEquals(expected, union.i)
        union.i = 0u
        assertEquals(0u, union.b)
    }
    return "OK"
}
