/*
 * Copyright 2010-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

// TARGET_BACKEND: NATIVE
// WITH_PLATFORM_LIBS
// MODULE: cinterop
// FILE: rttiMapRuntimeType.def
---
struct Data {
    int value;
};

// MODULE: main(cinterop)
// FILE: main.kt

@file:OptIn(
    kotlinx.cinterop.ExperimentalForeignApi::class,
    kotlin.experimental.ExperimentalNativeApi::class,
    kotlin.native.runtime.NativeRuntimeApi::class
)

import rttiMapRuntimeType.*
import kotlinx.cinterop.*
import kotlin.native.runtime.GC

// Class with both CPointer (raw pointer -> RT_NATIVE_PTR) and Kotlin object (-> RT_OBJECT)
// Both are `ptr` in LLVM - tests that isObjectType distinguishes them correctly
class MixedPointerHolder(
    val rawPointer: CPointer<Data>?,    // RT_NATIVE_PTR (8)
    val kotlinObject: Any?               // RT_OBJECT (1)
)

// Multiple interleaved fields to stress-test field ordering
class InterleavedHolder(
    val obj1: String?,
    val ptr1: CPointer<Data>?,
    val obj2: Any?,
    val ptr2: CPointer<Data>?,
    val obj3: List<Int>?
)

// Array holder - tests array element type identification
class ArrayHolder(
    val kotlinArray: Array<String>?,     // Object array elements -> RT_OBJECT
    val nativePointer: CPointer<Data>?   // RT_NATIVE_PTR
)

fun box(): String {
    memScoped {
        val data = alloc<Data>()
        data.value = 42

        // Test 1: MixedPointerHolder
        val holder1 = MixedPointerHolder(data.ptr, "test object")
        if (holder1.rawPointer?.pointed?.value != 42) return "FAIL: rawPointer value"
        if (holder1.kotlinObject != "test object") return "FAIL: kotlinObject value"

        // Test 2: InterleavedHolder - multiple fields
        val holder2 = InterleavedHolder(
            "first",
            data.ptr,
            listOf(1, 2, 3),
            data.ptr,
            listOf(4, 5, 6)
        )
        if (holder2.obj1 != "first") return "FAIL: obj1"
        if (holder2.ptr1?.pointed?.value != 42) return "FAIL: ptr1"

        // Test 3: ArrayHolder
        val holder3 = ArrayHolder(arrayOf("a", "b"), data.ptr)
        if (holder3.kotlinArray?.size != 2) return "FAIL: array size"

        // Force GC - if types are wrong, this could crash or corrupt data
        GC.collect()

        // Verify values survive GC (would fail if object refs weren't traced)
        if (holder1.kotlinObject != "test object") return "FAIL: kotlinObject after GC"
        if (holder2.obj1 != "first") return "FAIL: obj1 after GC"
        if ((holder2.obj2 as? List<*>)?.size != 3) return "FAIL: obj2 after GC"
    }

    return "OK"
}
