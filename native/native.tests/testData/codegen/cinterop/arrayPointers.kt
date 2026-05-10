// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_SECOND_STAGE: Native:2.3
// ^^^ KT-79742 is fixed in 2.3.20-Beta1

// TARGET_BACKEND: NATIVE
// WITH_PLATFORM_LIBS
// MODULE: cinterop
// FILE: carrayPointers.def
---
int (*arrayPointer)[1];

int globalArray[3] = {1, 2, 3};

struct StructWithArrayPtr {
    int (*arrayPointer)[1];
};

// MODULE: main(cinterop)
// FILE: main.kt

@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import carrayPointers.*
import kotlin.test.*
import kotlinx.cinterop.*

fun box(): String {
    arrayPointer = globalArray
    assertEquals(globalArray[0], arrayPointer!![0])
    arrayPointer!![0] = 15
    assertEquals(15, globalArray[0])

    memScoped {
        val struct = alloc<StructWithArrayPtr>()
        struct.arrayPointer = globalArray
        assertEquals(globalArray[0], struct.arrayPointer!![0])
    }
    return "OK"
}
