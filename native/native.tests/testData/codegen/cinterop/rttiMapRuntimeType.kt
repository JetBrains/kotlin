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
    kotlin.experimental.ExperimentalNativeApi::class
)

import rttiMapRuntimeType.*
import kotlinx.cinterop.*
import kotlin.native.internal.GCUnsafeCall
import kotlin.native.internal.NativePtr

// RT_OBJECT = 1 in Konan_RuntimeType
const val RT_OBJECT = 1
// RT_NATIVE_PTR = 8 in Konan_RuntimeType
const val RT_NATIVE_PTR = 8

// Class with both CPointer (raw pointer -> RT_NATIVE_PTR) and Kotlin object (-> RT_OBJECT).
// Both are `ptr` in LLVM IR — mapRuntimeType must use isObjectType to distinguish them.
class MixedPointerHolder(
    val rawPointer: CPointer<Data>?,    // should be RT_NATIVE_PTR (8)
    val kotlinObject: Any?               // should be RT_OBJECT (1)
)

// Multiple interleaved fields to stress-test field ordering
class InterleavedHolder(
    val obj1: String?,                   // should be RT_OBJECT (1)
    val ptr1: CPointer<Data>?,           // should be RT_NATIVE_PTR (8)
    val obj2: Any?,                      // should be RT_OBJECT (1)
    val ptr2: CPointer<Data>?,           // should be RT_NATIVE_PTR (8)
    val obj3: List<Int>?                 // should be RT_OBJECT (1)
)

// Direct calls to Kotlin/Native runtime debug functions.
// These read the extended type info (field types populated by RTTIGenerator.mapRuntimeType).
// Using @GCUnsafeCall keeps the thread in RUNNABLE state, avoiding thread state assertion.
@GCUnsafeCall("Konan_DebugGetFieldCount")
external fun debugGetFieldCount(obj: Any): Int

@GCUnsafeCall("Konan_DebugGetFieldType")
external fun debugGetFieldType(obj: Any, index: Int): Int

@GCUnsafeCall("Konan_DebugGetFieldName")
external fun debugGetFieldName(obj: Any, index: Int): NativePtr

// Inspect the extended RTTI field types using runtime debug functions.
// This directly reads the fieldTypes_ array populated by RTTIGenerator.mapRuntimeType.
fun inspectFieldTypes(obj: Any): Map<String, Int> {
    val count = debugGetFieldCount(obj)
    val result = mutableMapOf<String, Int>()
    for (i in 0 until count) {
        val namePtr = debugGetFieldName(obj, i)
        val name = interpretCPointer<ByteVar>(namePtr)?.toKString() ?: "?"
        val type = debugGetFieldType(obj, i)
        result[name] = type
    }
    return result
}

fun box(): String {
    memScoped {
        val data = alloc<Data>()
        data.value = 42

        // --- Test 1: MixedPointerHolder ---
        // Verify RTTI field types directly (the values populated by mapRuntimeType)
        val holder1 = MixedPointerHolder(data.ptr, "test object")
        val types1 = inspectFieldTypes(holder1)

        if (types1.size != 2)
            return "FAIL: MixedPointerHolder field count = ${types1.size}, expected 2"
        if (types1["rawPointer"] != RT_NATIVE_PTR)
            return "FAIL: rawPointer has runtime type ${types1["rawPointer"]}, expected RT_NATIVE_PTR ($RT_NATIVE_PTR)"
        if (types1["kotlinObject"] != RT_OBJECT)
            return "FAIL: kotlinObject has runtime type ${types1["kotlinObject"]}, expected RT_OBJECT ($RT_OBJECT)"

        // --- Test 2: InterleavedHolder ---
        // Multiple interleaved object/pointer fields
        val holder2 = InterleavedHolder("first", data.ptr, listOf(1, 2, 3), data.ptr, listOf(4, 5, 6))
        val types2 = inspectFieldTypes(holder2)

        if (types2.size != 5)
            return "FAIL: InterleavedHolder field count = ${types2.size}, expected 5"

        for (name in listOf("obj1", "obj2", "obj3")) {
            if (types2[name] != RT_OBJECT)
                return "FAIL: $name has runtime type ${types2[name]}, expected RT_OBJECT ($RT_OBJECT)"
        }
        for (name in listOf("ptr1", "ptr2")) {
            if (types2[name] != RT_NATIVE_PTR)
                return "FAIL: $name has runtime type ${types2[name]}, expected RT_NATIVE_PTR ($RT_NATIVE_PTR)"
        }
    }

    return "OK"
}
