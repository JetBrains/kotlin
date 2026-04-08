// TARGET_BACKEND: NATIVE
// MODULE: cinterop
// FILE: cstructs.def
nonStrictEnums = NonStrict
---
typedef struct {
    int i;
} Trivial;

enum E {
    R, G, B
};

enum NonStrict {
    N, S, K
};

struct Complex {
    unsigned int ui;
    Trivial t;
    struct Complex* next;
    enum E e;
    enum NonStrict nonStrict;
    int arr[2];
    _Bool b;
};

struct __attribute__((packed)) Packed {
    int i : 1;
    enum E e : 2;
};

struct Complex produceComplex() {
    struct Complex complex = {
        .ui = 128,
        .t = {1},
        .next = 0,
        .e = R,
        .nonStrict = K,
        .arr = {-51, -19},
        .b = 1
    };
    return complex;
};

struct WithFlexibleArray {
    int size;
    int data[];
};

struct WithFlexibleArrayWithPadding {
    int size;
    char c;
    long long data[];
};

void fillArray(struct WithFlexibleArrayWithPadding *flex, int count) {
    flex->size = count;
    flex->c = '!';
    for (int i = 0; i < count; i++) {
        flex->data[i] = (((long long)i) << 32) | (i * 100);
    }
}

struct WithZeroSizedArray {
    int size;
    int data[0];
};

// MODULE: main(cinterop)
// FILE: main.kt

@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import cstructs.*
import kotlinx.cinterop.*
import kotlin.test.*

fun box(): String {
    produceComplex().useContents {
        assertEquals(ui, 128u)
        ui = 333u
        assertEquals(ui, 333u)

        assertEquals(t.i, 1)
        t.i += 15
        assertEquals(t.i, 16)

        assertEquals(next, null)
        next = this.ptr
        assertEquals(next, this.ptr)
        // Check null pointer because it has Nothing? type.
        next = null
        assertEquals(next, null)

        assertEquals(e, E.R)
        e = E.G
        assertEquals(e, E.G)

        assertEquals(K, nonStrict)
        nonStrict = S
        assertEquals(S, nonStrict)

        assertEquals(arr[0], -51)
        assertEquals(arr[1], -19)
        arr[0] = 51
        arr[1] = 19
        assertEquals(arr[0], 51)
        assertEquals(arr[1], 19)

        assertEquals(true, b)
        b = false
        assertEquals(false, b)

        // Check that subtyping via Nothing-returning functions does not break compiler.
        assertFailsWith<NotImplementedError> {
            ui = TODO()
            t.i = TODO()
            next = TODO()
            e = TODO()
            nonStrict = TODO()
            b = TODO()
        }
    }
    memScoped {
        val packed = alloc<Packed>()
        packed.i = -1
        assertEquals(-1, packed.i)
        packed.e = E.R
        assertEquals(E.R, packed.e)
        // Check that subtyping via Nothing-returning functions does not break compiler.
        assertFailsWith<NotImplementedError> {
            packed.i = TODO()
            packed.e = TODO()
        }
    }
    // Check that generics doesn't break anything.
    checkEnumSubTyping(E.R)
    checkIntSubTyping(630090)

    memScoped {
        val SIZE = 10
        val flex = alloc(sizeOf<WithFlexibleArray>() + sizeOf<IntVar>() * SIZE, alignOf<WithFlexibleArray>()).reinterpret<WithFlexibleArray>()
        flex.size = SIZE
        for (i in 0 until SIZE) {
            flex.data[i] = i
        }
        assertEquals(SIZE, flex.size)
        for (i in 0 until SIZE) {
            assertEquals(i, flex.data[i])
        }
    }

    memScoped {
        val SIZE = 10
        val flex = alloc(sizeOf<WithZeroSizedArray>() + sizeOf<IntVar>() * SIZE, alignOf<WithZeroSizedArray>()).reinterpret<WithZeroSizedArray>()
        assertEquals(4, sizeOf<WithZeroSizedArray>())
        assertEquals(4, alignOf<WithZeroSizedArray>())
        flex.size = SIZE
        for (i in 0 until SIZE) {
            flex.data[i] = i
        }
        assertEquals(SIZE, flex.size)
        for (i in 0 until SIZE) {
            assertEquals(i, flex.data[i])
        }
    }

    memScoped {
        val SIZE = 10
        assertEquals(8, sizeOf<WithFlexibleArrayWithPadding>())
        assertEquals(8, alignOf<WithFlexibleArrayWithPadding>())
        val flex = alloc(sizeOf<WithFlexibleArrayWithPadding>() + sizeOf<LongVar>() * SIZE, alignOf<WithFlexibleArrayWithPadding>())
                .reinterpret<WithFlexibleArrayWithPadding>()
        fillArray(flex.ptr, SIZE)
        assertEquals(SIZE, flex.size)
        assertEquals('!'.code.toByte(), flex.c);
        for (i in 0 until SIZE) {
            assertEquals((i.toLong() shl 32) or (i.toLong() * 100), flex.data[i])
        }
    }
    return "OK"
}

fun <T : E> checkEnumSubTyping(e: T) = memScoped {
    val s = alloc<Complex>()
    s.e = e
}

fun <T : Int> checkIntSubTyping(x: T) = memScoped {
    val s = alloc<Trivial>()
    s.i = x
}
