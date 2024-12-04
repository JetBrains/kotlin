/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
#include "kt-42830_api.h"
#define __ kt_42830_symbols()->
#define T_(x) kt_42830_kref_ ## x

#include <stdio.h>
#include <stdbool.h>
#include <stdint.h>
#include <float.h>

#define TEST_TYPE_VAL(TYPE_C, TYPE, VAL) \
    TYPE_C primitive##TYPE = VAL; \
    __ kotlin.root.print##TYPE   (primitive##TYPE); \
    T_(kotlin_##TYPE) nullable##TYPE = __ createNullable##TYPE (primitive##TYPE); \
    T_(kotlin_Any) any##TYPE = { .pinned = nullable##TYPE.pinned }; \
    __ kotlin.root.printAny (any##TYPE); \
    TYPE_C unboxed##TYPE = __ getNonNullValueOf##TYPE (nullable##TYPE); \
    __ kotlin.root.print##TYPE   (unboxed##TYPE);


int main(int argc, char** argv) {
    TEST_TYPE_VAL(bool, Boolean, true);
    TEST_TYPE_VAL(uint16_t, Char, UINT16_MAX);
    TEST_TYPE_VAL(int8_t, Byte, INT8_MIN);
    TEST_TYPE_VAL(int16_t, Short, INT16_MIN);
    TEST_TYPE_VAL(int32_t, Int, INT32_MIN);
    TEST_TYPE_VAL(int64_t, Long, INT64_MIN);
    TEST_TYPE_VAL(float, Float, FLT_MAX);
    TEST_TYPE_VAL(double, Double, DBL_MAX);
    TEST_TYPE_VAL(uint8_t, UByte, UINT8_MAX);
    TEST_TYPE_VAL(uint16_t, UShort, UINT16_MAX);
    TEST_TYPE_VAL(uint32_t, UInt, UINT32_MAX);
    TEST_TYPE_VAL(uint64_t, ULong, UINT64_MAX);
}
