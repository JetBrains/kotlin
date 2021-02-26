/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "Memory.h"

#include "KAssert.h"

ALWAYS_INLINE bool isFrozen(const ObjHeader* obj) {
    // TODO: Unimplemented
    return false;
}

extern "C" {

void MutationCheck(ObjHeader* obj) {
    // TODO: Unimplemented
}

void FreezeSubgraph(ObjHeader* obj) {
    // TODO: Unimplemented
}

void EnsureNeverFrozen(ObjHeader* obj) {
    TODO();
}

void Kotlin_native_internal_GC_suspend(ObjHeader*) {
    TODO();
}

void Kotlin_native_internal_GC_resume(ObjHeader*) {
    TODO();
}

void Kotlin_native_internal_GC_stop(ObjHeader*) {
    TODO();
}

void Kotlin_native_internal_GC_start(ObjHeader*) {
    TODO();
}

void Kotlin_native_internal_GC_setTuneThreshold(ObjHeader*, int32_t value) {
    TODO();
}

bool Kotlin_native_internal_GC_getTuneThreshold(ObjHeader*) {
    TODO();
}

bool TryAddHeapRef(const ObjHeader* object) {
    TODO();
}

RUNTIME_NOTHROW void ReleaseHeapRef(const ObjHeader* object) {
    TODO();
}

RUNTIME_NOTHROW void ReleaseHeapRefNoCollect(const ObjHeader* object) {
    TODO();
}

ForeignRefContext InitLocalForeignRef(ObjHeader* object) {
    TODO();
}

} // extern "C"
