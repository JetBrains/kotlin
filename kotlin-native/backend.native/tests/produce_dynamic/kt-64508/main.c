/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
#include "testlib_api.h"
#define __ testlib_symbols()->
#define T_(x) testlib_kref_ ## x

#include <stdio.h>

int main(int argc, char** argv) {
    printf("Ok\n");
}
