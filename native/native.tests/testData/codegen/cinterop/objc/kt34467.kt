// TARGET_BACKEND: NATIVE
// DISABLE_NATIVE: isAppleTarget=false

// FREE_CINTEROP_ARGS: -compiler-option -fmodule-map-file=$generatedSourcesDir/cinterop/module_library.modulemap
// MODULE: cinterop
// FILE: module_library.def
language = Objective-C
modules = module_library

// FILE: module_library.modulemap
module module_library {
    umbrella header "module_library_umbrella.h"

    export *
    module * { export * }
}

// FILE: module_library_umbrella.h
#import <foo.h>

// FILE: foo.h
#define ANSWER 42

// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
import module_library.*

fun box(): String {
    val answer = ANSWER
    if (answer != 42)
        return "FAIL: $answer"
    return "OK"
}
