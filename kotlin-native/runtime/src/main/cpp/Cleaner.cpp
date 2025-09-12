/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "Cleaner.h"

#include "ExternalRCRef.hpp"
#include "Memory.h"

using namespace kotlin;

// Defined in RuntimeUtils.kt
extern "C" void Kotlin_internal_executeAndRelease(mm::RawExternalRCRef* cleanerAction);

namespace {

struct CleanerImpl {
    ObjHeader header;
    mm::RawExternalRCRef* cleanerStablePtr;
};

} // namespace

RUNTIME_NOTHROW void DisposeCleaner(KRef thiz) {
    try {
        mm::RawExternalRCRef* cleanerAction = reinterpret_cast<CleanerImpl*>(thiz)->cleanerStablePtr;
        Kotlin_internal_executeAndRelease(cleanerAction);
    } catch (...) {
        // A trick to terminate with unhandled exception. This will print a stack trace
        // and write to iOS crash log.
        std::terminate();
    }
}
