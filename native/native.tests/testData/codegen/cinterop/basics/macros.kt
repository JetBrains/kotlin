/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
// TARGET_BACKEND: NATIVE
// MODULE: cinterop
// FILE: cmacros.def
excludedMacros = EXCLUDED
---
int* ptr_call() {
    return (int*) 1;
}

int int_call() {
    return 42;
}

void void_call() {}

int arg_call(int x) {
    return x;
}

typedef struct {
    int value
} struct_t;

struct_t getStruct() {
    return (struct_t){ 1 };
}

int global_var = 5;

#define TOO_MANY_ERRORS x, x, x, x, x, x, x, x, x, x, x, x, x, x, x, x, x, x, x, x, x, x, x, x

#define LEFT_BRACE {

#define ZERO 0
#define ONE 1

#define RIGHT_BRACE }

#define MAX_LONG 9223372036854775807
#define FOO_STRING "foo"
// This one should be ignored:
#define WIDE_FOO_STRING L"foo"
#define FOURTY_TWO 42
#define SEVENTEEN ((long long) 17)
#define ONE_POINT_ZERO 1.0
#define ONE_POINT_FIVE 1.5f

#define LEFT_PAREN (

#define NULL_PTR ((void*)0)
#define VOID_PTR ((void*)1)
#define INT_PTR ((int*)1)
#define PTR_SUM (INT_PTR + 1)
#define PTR_SUM_EXPECTED (sizeof(int) + 1)

#define RIGHT_PAREN )

enum {
    INT_CALL = 1 // Should be replaced by macro below.
};

#define PTR_CALL ptr_call()
#define INT_CALL int_call()
#define CALL_SUM (int_call() + int_call())
#define GLOBAL_VAR (global_var)

// This one should be excluded:
#define EXCLUDED 42

// These ones should be ignored:
#define VOID_CALL   (void_call())
#define ARG_CALL(x) (arg_call(x))
#define GET_STRUCT  (getStruct())
#define UNDECLARED  (undeclared())

#define BAD1 bar
#define BAD2 5;
#define BAD3 { foo(); }

void increment(int* counter);
#define increment(counter) { (*(counter))++; }

#define DEFAULT_DOUBLE_NAN __builtin_nan("")
#define DEFAULT_FLOAT_NAN __builtin_nanf("")
#define OTHER_DOUBLE_NAN __builtin_nan("0x123456789ab")
#define OTHER_FLOAT_NAN __builtin_nanf("0x12345")

// MODULE: main(cinterop)
// FILE: main.kt

@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class, kotlinx.cinterop.ExperimentalForeignApi::class)

import kotlin.test.*
import cmacros.*
import kotlinx.cinterop.*

fun box(): String {
    assertEquals("foo", FOO_STRING)
    assertEquals(0, ZERO)
    assertEquals(1, ONE)
    assertEquals(Long.MAX_VALUE, MAX_LONG)
    assertEquals(42, FOURTY_TWO)

    val seventeen: Long = SEVENTEEN
    assertEquals(17L, seventeen)

    val onePointFive: Float = ONE_POINT_FIVE
    val onePointZero: Double = ONE_POINT_ZERO

    assertEquals(1.5f, onePointFive)
    assertEquals(1.0, onePointZero)

    val nullPtr: COpaquePointer? = NULL_PTR
    val voidPtr: COpaquePointer? = VOID_PTR
    val intPtr: CPointer<IntVar>? = INT_PTR
    val ptrSum: CPointer<IntVar>?  = PTR_SUM
    val ptrCall: CPointer<IntVar>? = PTR_CALL

    assertEquals(null, nullPtr)
    assertEquals(1L, voidPtr.rawValue.toLong())
    assertEquals(1L, intPtr.rawValue.toLong())
    assertEquals(PTR_SUM_EXPECTED.toLong(), ptrSum.rawValue.toLong())
    assertEquals(1L, ptrCall.rawValue.toLong())

    assertEquals(42, INT_CALL)
    assertEquals(84, CALL_SUM)
    assertEquals(5, GLOBAL_VAR)

    memScoped {
        val counter = alloc<IntVar>()
        counter.value = 42
        increment(counter.ptr)
        assertEquals(43, counter.value)
    }


    val floatNanBase = Float.NaN.toRawBits()
    assertEquals(floatNanBase, 0x7fc00000)
    val doubleNanBase = Double.NaN.toRawBits()
    assertEquals(doubleNanBase, 0x7ff8000000000000L)
    assertEquals(floatNanBase, DEFAULT_FLOAT_NAN.toRawBits())
    assertEquals(doubleNanBase, DEFAULT_DOUBLE_NAN.toRawBits())
    assertEquals(floatNanBase, OTHER_FLOAT_NAN.toRawBits())
    assertEquals(doubleNanBase, OTHER_DOUBLE_NAN.toRawBits())

    return "OK"
}
