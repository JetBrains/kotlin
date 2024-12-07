/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

// TARGET_BACKEND: NATIVE
// OUTPUT_DATA_FILE: funptr.out
// MODULE: cinterop
// FILE: cfunptr.def
headerFilter = NOTHING
---
#include <stdio.h>
#include <stdlib.h>

typedef int (*atoiPtrType)(const char*);

static atoiPtrType getAtoiPtr() {
    return &atoi;
}

static void __printInt(int x) {
    printf("%d\n", x);
}

static void* __getPrintIntPtr() {
    return &__printInt;
}

typedef void* (*getPrintIntPtrPtrType)(void);

static getPrintIntPtrPtrType getGetPrintIntPtrPtr() {
    return &__getPrintIntPtr;
}

static double __add(double x, double y) {
    return x + y;
}

typedef double (*addPtrType)(double, double);

static addPtrType getAddPtr() {
    return &__add;
}

static int __doubleToInt(double x) {
    return (int) x;
}

typedef int (*doubleToIntPtrType)(double);

static doubleToIntPtrType getDoubleToIntPtr() {
    return &__doubleToInt;
}

static _Bool __isIntPositive(int x) {
    return x > 0;
}

typedef _Bool (*isIntPositivePtrType)(int);

static isIntPositivePtrType getIsIntPositivePtr() {
    return &__isIntPositive;
}

static unsigned int getMaxUInt(void) {
    return 0xffffffff;
}

static typeof(&getMaxUInt) getMaxUIntGetter() {
    return &getMaxUInt;
}

typedef int (*longSignatureFunctionPtrType)(
int, int, int, int, int, int, int, int, int, int, int, int,
int, int, int, int, int, int, int, int, int, int, int
);

typedef int (*notSoLongSignatureFunctionPtrType)(
int, int, int, int, int, int, int, int, int, int, int,
int, int, int, int, int, int, int, int, int, int, int
);

static int longSignatureFunction(
int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, int p9, int p10, int p11, int p12,
int p13, int p14, int p15, int p16, int p17, int p18, int p19, int p20, int p21, int p22, int p23
) {
    return 42;
}

static int notSoLongSignatureFunction(
int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, int p9, int p10, int p11, int p12,
int p13, int p14, int p15, int p16, int p17, int p18, int p19, int p20, int p21, int p22
) {
    return 42;
}

static longSignatureFunctionPtrType getLongSignatureFunctionPtr() {
    return &longSignatureFunction;
}

static notSoLongSignatureFunctionPtrType getNotSoLongSignatureFunctionPtr() {
    return &notSoLongSignatureFunction;
}

// MODULE: main(cinterop)
// FILE: main.kt

@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import kotlinx.cinterop.*
import cfunptr.*
import kotlin.test.*

typealias NotSoLongSignatureFunction = (
    Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int,
    Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int
) -> Int

fun main() {
    val atoiPtr = getAtoiPtr()!!

    val getPrintIntPtrPtr = getGetPrintIntPtrPtr()!!
    val printIntPtr = getPrintIntPtrPtr()!!.reinterpret<CFunction<(Int) -> Unit>>()

    val fortyTwo = memScoped {
        atoiPtr("42".cstr.getPointer(memScope))
    }

    printIntPtr(fortyTwo)

    printIntPtr(
            getDoubleToIntPtr()!!(
                    getAddPtr()!!(5.1, 12.2)
            )
    )

    val isIntPositivePtr = getIsIntPositivePtr()!!

    printIntPtr(isIntPositivePtr(42).ifThenOneElseZero())
    printIntPtr(isIntPositivePtr(-42).ifThenOneElseZero())

    assertEquals(getMaxUIntGetter()!!(), UInt.MAX_VALUE)

    val longSignaturePtr: COpaquePointer? = getLongSignatureFunctionPtr()
    val notSoLongSignaturePtr: CPointer<CFunction<NotSoLongSignatureFunction>>? = getNotSoLongSignatureFunctionPtr()
    printIntPtr(notSoLongSignaturePtr!!.invoke(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0))
    printIntPtr(notSoLongSignatureFunction(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0))
}

fun Boolean.ifThenOneElseZero() = if (this) 1 else 0
