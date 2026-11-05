/*
 * Copyright 2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "CommonLauncher.hpp"

#include <cstdlib>

#include "Memory.h"
#include "Natives.h"
#include "Runtime.h"
#include "KString.h"
#include "Types.h"

using namespace kotlin;

namespace {

/**
 * Returns a Kotlin's Array of Strings parsed from the given arguments.
 */
OBJ_GETTER(parseArgumentsToKotlinStringArray, int argc, const char** argv) {
    if (argc > 0 && argv[0][0] != '\0') {
        // Don't set the programName to an empty string (by checking argv[0][0] != '\0') to make all platforms behave the same:
        // Linux would set argv[0] to "" in case no programName is passed, whereas Windows & macOS would set argc to 0.
        kotlin::programName = argv[0];
    }

    // The count is one less, because we skip argv[0] which is the binary name.
    ObjHeader* result = AllocArrayInstance(theArrayTypeInfo, std::max(0, argc - 1), OBJ_RESULT);
    ArrayHeader* array = result->array();
    for (int index = 1; index < argc; index++) {
        ObjHolder result;
        CreateStringFromCString(argv[index], result.slot());
        UpdateHeapRef(ArrayAddressOfElementAt(array, index - 1), result.obj());
    }

    return result;
}
} // namespace

namespace kotlin {

int executableEntryPoint(int argc, const char** argv, KInt (*entryPoint)(const ObjHeader*)) {
    Kotlin_initRuntimeIfNeeded();
    Kotlin_mm_switchThreadStateRunnable();

    ObjHolder args;
    parseArgumentsToKotlinStringArray(argc, argv, args.slot());

    KInt exitStatus = entryPoint(args.obj());

    Kotlin_shutdownRuntime();

    kotlin::programName = nullptr; // argv[0] might not be valid after this point

    return exitStatus;
}

} // namespace kotlin
