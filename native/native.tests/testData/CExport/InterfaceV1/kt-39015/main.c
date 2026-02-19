/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
#include "kt-39015_api.h"
#define __ kt_39015_symbols()->
#define T_(x) kt_39015_kref_ ## x

#include <stdio.h>

int main(int argc, char** argv) {
    T_(ObjectForExample) instance = __ kotlin.root.ObjectForExample._instance ();
    T_(Example)          value =    __ kotlin.root.ObjectForExample.get_example (instance);
    printf("%td\n", value.pinned - NULL);

    // Make sure all printf output is flushed.
    fflush(NULL);
}
