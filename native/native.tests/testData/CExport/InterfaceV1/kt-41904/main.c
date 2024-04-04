/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
#include "kt-41904_api.h"
#define __ kt_41904_symbols()->
#define T_(x) kt_41904_kref_ ## x

#include <stdio.h>

int main(int argc, char** argv) {
    T_(TestInterface) tiFromInt = __ kotlin.root.TestInterface_ (153);
    printf("TestInterface constructor(Int): %.1f\n", __ kotlin.root.TestInterface.doSomething(tiFromInt));
    T_(TestInterface) tiFromDouble = __ kotlin.root.TestInterface__ (42.0);
    printf("TestInterface constructor(Double): %.1f\n", __ kotlin.root.TestInterface.doSomething(tiFromDouble));

    T_(ContainingClass) containingClassInstance = __ kotlin.root.ContainingClass.ContainingClass();
    T_(ContainingClass_InnerInterface) iiFromInt = __ kotlin.root.ContainingClass.InnerInterface_ (containingClassInstance, 153);
    printf("InnerInterface constructor(Int): %.1f\n", __ kotlin.root.ContainingClass.InnerInterface.doSomething(iiFromInt));
    T_(ContainingClass_InnerInterface) iiFromDouble = __ kotlin.root.ContainingClass.InnerInterface__ (containingClassInstance, 42.0);
    printf("InnerInterface constructor(Double): %.1f\n", __ kotlin.root.ContainingClass.InnerInterface.doSomething(iiFromDouble));

    T_(TestInterface) tiFromInnerPackage = __ kotlin.root.TestPackage_ (__ kotlin.root.TestPackage.identity(19.0));
    printf("TestInterface initialized from inner package: %.1f\n", __ kotlin.root.TestInterface.doSomething(tiFromInnerPackage));

    // Make sure all printf output is flushed.
    fflush(NULL);
}
