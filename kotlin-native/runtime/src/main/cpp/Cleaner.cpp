/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "Cleaner.h"

#include <exception>

// Defined in Cleaner.kt
extern "C" void Kotlin_native_ref_CleanerImpl_finalize(KRef thiz);

RUNTIME_NOTHROW void DisposeCleaner(KRef thiz) {
    try {
        Kotlin_native_ref_CleanerImpl_finalize(thiz);
    } catch (...) {
        // Ignore exceptions
    }
}