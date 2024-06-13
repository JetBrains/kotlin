/*
 * Copyright 2010-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ExternalRCRef.hpp"

#include "Memory.h"
#include "ObjCBackRef.hpp"
#include "PointerBits.h"

using namespace kotlin;

// Implementation of kotlin.native.internal.ref.ExternalRCRef

namespace {

constexpr unsigned kPermanentTag = 1;

}

RUNTIME_NOTHROW extern "C" void* Kotlin_native_internal_ref_createRetainedExternalRCRef(KRef obj) {
    AssertThreadState(ThreadState::kRunnable);
    RuntimeAssert(obj != nullptr, "Cannot handle nullptr");

    if (obj->permanent()) {
        return mm::permanentObjectAsExternalRCRef(obj);
    }
    return static_cast<mm::RawSpecialRef*>(mm::ObjCBackRef::create(obj));
}

RUNTIME_NOTHROW extern "C" void Kotlin_native_internal_ref_disposeExternalRCRef(void* ref) {
    RuntimeAssert(ref != nullptr, "Cannot handle nullptr");
    if (auto obj = mm::externalRCRefAsPermanentObject(ref)) {
        // Nothing to do.
        return;
    }
    mm::ObjCBackRef(static_cast<mm::RawSpecialRef*>(ref)).dispose();
}

RUNTIME_NOTHROW extern "C" OBJ_GETTER(Kotlin_native_internal_ref_dereferenceExternalRCRef, void* ref) {
    AssertThreadState(ThreadState::kRunnable);
    RuntimeAssert(ref != nullptr, "Cannot handle nullptr");
    if (auto obj = mm::externalRCRefAsPermanentObject(ref)) {
        RETURN_OBJ(obj);
    }
    RETURN_OBJ(*mm::ObjCBackRef(static_cast<mm::RawSpecialRef*>(ref)));
}

RUNTIME_NOTHROW extern "C" void Kotlin_native_internal_ref_retainExternalRCRef(void* ref) {
    RuntimeAssert(ref != nullptr, "Cannot handle nullptr");
    if (auto obj = mm::externalRCRefAsPermanentObject(ref)) {
        // Nothing to do.
        return;
    }
    mm::ObjCBackRef(static_cast<mm::RawSpecialRef*>(ref)).retain();
}

RUNTIME_NOTHROW extern "C" void Kotlin_native_internal_ref_releaseExternalRCRef(void* ref) {
    RuntimeAssert(ref != nullptr, "Cannot handle nullptr");
    if (auto obj = mm::externalRCRefAsPermanentObject(ref)) {
        // Nothing to do.
        return;
    }
    mm::ObjCBackRef(static_cast<mm::RawSpecialRef*>(ref)).release();
}

RUNTIME_NOTHROW extern "C" bool Kotlin_native_internal_ref_tryRetainExternalRCRef(void* ref) {
    AssertThreadState(ThreadState::kRunnable);
    RuntimeAssert(ref != nullptr, "Cannot handle nullptr");
    if (auto obj = mm::externalRCRefAsPermanentObject(ref)) {
        return true;
    }
    return mm::ObjCBackRef(static_cast<mm::RawSpecialRef*>(ref)).tryRetain();
}

KRef kotlin::mm::externalRCRefAsPermanentObject(void* ref) noexcept {
    RuntimeAssert(ref != nullptr, "Cannot handle nullptr");
    if (hasPointerBits(ref, kPermanentTag)) {
        auto obj = clearPointerBits(static_cast<KRef>(ref), kPermanentTag);
        RuntimeAssert(obj->permanent(), "Permanent ExternalRCRef for non-permanent object %p", obj);
        return obj;
    }
    return nullptr;
}

void* kotlin::mm::permanentObjectAsExternalRCRef(KRef obj) noexcept {
    RuntimeAssert(obj != nullptr, "Cannot handle nullptr");
    RuntimeAssert(obj->permanent(), "Object %p must be permanent", obj);
    return setPointerBits(obj, kPermanentTag);
}
