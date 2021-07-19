/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

#include "testlib_api.h"

#include <stdio.h>
#include <exception>
#include <iostream>

int main(int argc, char** argv) {
    // The reverse interop machinery will catch the exception on the interop border and terminate the program.
    try {
        testlib_symbols()->kotlin.root.setHookAndThrow();
    } catch (...) {
        std::cout << "Should not happen" << std::endl;
    }
}