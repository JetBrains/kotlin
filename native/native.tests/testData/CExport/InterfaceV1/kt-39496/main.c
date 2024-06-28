/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
#include "kt-39496_api.h"
#define __ kt_39496_symbols()->
#define T_(x) kt_39496_kref_ ## x

#include <stdio.h>

int main(int argc, char** argv) {
    __ kotlin.root.helloChar   ((T_(kotlin_Char))   {NULL});
    __ kotlin.root.helloByte   ((T_(kotlin_Byte))   {NULL});
    __ kotlin.root.helloShort  ((T_(kotlin_Short))  {NULL});
    __ kotlin.root.helloInt    ((T_(kotlin_Int))    {NULL});
    __ kotlin.root.helloLong   ((T_(kotlin_Long))   {NULL});
    __ kotlin.root.helloUByte  ((T_(kotlin_UByte))  {NULL});
    __ kotlin.root.helloUShort ((T_(kotlin_UShort)) {NULL});
    __ kotlin.root.helloUInt   ((T_(kotlin_UInt))   {NULL});
    __ kotlin.root.helloULong  ((T_(kotlin_ULong))  {NULL});
    __ kotlin.root.helloFloat  ((T_(kotlin_Float))  {NULL});
    __ kotlin.root.helloDouble ((T_(kotlin_Double)) {NULL});
}
