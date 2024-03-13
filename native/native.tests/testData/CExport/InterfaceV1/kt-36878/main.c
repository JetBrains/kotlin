/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
#include "kt-36878_api.h"
#define __ kt_36878_symbols()->
#define T_(x) kt_36878_kref_ ## x

int main(int argc, char** argv) {
    T_(ObjectForExample)   instance = __ kotlin.root.ObjectForExample._instance ();
    T_(ValueClass)            value = __ kotlin.root.ObjectForExample.get_nullableVal (instance);
    T_(UsualClass)            usual = __ kotlin.root.ObjectForExample.get_usual (instance);
    T_(kotlin_ByteArray) uByteArray = __ kotlin.root.ObjectForExample.get_uByteArray (instance);
    T_(kotlin_IntArray)   uIntArray = __ kotlin.root.ObjectForExample.get_uIntArray (instance);
    T_(Foo)           fooValueClass = __ kotlin.root.ObjectForExample.get_fooValueClass (instance);
    T_(Foo)           fooUsualClass = __ kotlin.root.ObjectForExample.get_fooValueClass (instance);
    __ kotlin.root.ObjectForExample.fooValue             (instance, value);
    __ kotlin.root.ObjectForExample.fooUsual             (instance, usual);
    __ kotlin.root.ObjectForExample.fooUByteArray        (instance, uByteArray);
    __ kotlin.root.ObjectForExample.fooUIntArrayNullable (instance, uIntArray);
    __ kotlin.root.ObjectForExample.fooFooValue          (instance, fooValueClass);
    __ kotlin.root.ObjectForExample.fooFooUsual          (instance, fooUsualClass);
}