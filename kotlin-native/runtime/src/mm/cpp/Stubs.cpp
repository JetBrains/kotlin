/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "Memory.h"

#include "KAssert.h"

extern "C" {

bool TryAddHeapRef(const ObjHeader* object) {
    TODO();
}

RUNTIME_NOTHROW void ReleaseHeapRef(const ObjHeader* object) {
    TODO();
}

RUNTIME_NOTHROW void ReleaseHeapRefNoCollect(const ObjHeader* object) {
    TODO();
}

} // extern "C"
