/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
#include "kt-64508_api.h"
#define __ kt_64508_symbols()->
#define T_(x) kt_64508_kref_ ## x

#include <stdio.h>

int main(int argc, char** argv) {
    printf("Ok\n");
}
