/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
#include "testlib_api.h"
#define __ testlib_symbols()->
#define T_(x) testlib_kref_ ## x

#include <stdio.h>

int main(int argc, char** argv) {
    T_(ObjectForExample) instance = __ kotlin.root.ObjectForExample._instance ();
    T_(Example)          value =    __ kotlin.root.ObjectForExample.get_example (instance);
    printf("%x\n", value.pinned);
}
