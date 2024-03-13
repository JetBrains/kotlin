/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

#include "unhandledExceptionThroughBridge_api.h"

#include <stdio.h>
#include <exception>
#include <iostream>

void throwException() {
    throw std::runtime_error("Cpp exception");
}

int main(int argc, char** argv) {
    // The reverse interop machinery will catch the exception on the interop border and terminate the program.
    try {
        unhandledExceptionThroughBridge_symbols()->kotlin.root.callback((void*)throwException);
    } catch (...) {
        std::cout << "Should not happen" << std::endl;
    }
}