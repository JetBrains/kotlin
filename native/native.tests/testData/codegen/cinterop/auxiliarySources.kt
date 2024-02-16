// TARGET_BACKEND: NATIVE
// FREE_CINTEROP_ARGS: -header auxiliaryCppSources.h

// MODULE: cinterop
// FILE: auxiliaryCppSources.def
# The def file is intentionally empty
# `auxiliaryCppSources.h` is meant to be included via `-header` free arg of cinterop tool

// FILE: auxiliaryCppSources.h
#ifdef __cplusplus
extern "C" {
#endif

const char* name();

#ifdef __cplusplus
}
#endif

// FILE: auxiliaryCppSources.cpp
#include <string>
#include "auxiliaryCppSources.h"

static std::string _name = "OK";

extern "C" const char* name() {
    return _name.c_str();
}

// MODULE: main(cinterop)
// FILE: main.kt

@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import auxiliaryCppSources.*
import kotlin.test.*
import kotlinx.cinterop.*

fun box(): String {
    return name()!!.toKString()
}
