/*
 * Copyright 2010-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ExternalRCRef.hpp"

#include "Memory.h"
#include "PointerBits.h"
#include "SpecialRefRegistry.hpp"
#include "ThreadData.hpp"
#include "ThreadRegistry.hpp"

using namespace kotlin;

// Implementation of kotlin.native.internal.ref.ExternalRCRef

namespace {

constexpr unsigned kPermanentTag = 1;

}

RUNTIME_NOTHROW extern "C" mm::RawExternalRCRef* Kotlin_native_internal_ref_createRetainedExternalRCRef(KRef obj) {
    return mm::createRetainedExternalRCRef(obj);
}

RUNTIME_NOTHROW extern "C" mm::RawExternalRCRef* Kotlin_native_internal_ref_createUnretainedExternalRCRef(KRef obj) {
    return mm::createUnretainedExternalRCRef(obj);
}

RUNTIME_NOTHROW extern "C" void Kotlin_native_internal_ref_disposeExternalRCRef(mm::RawExternalRCRef* ref) {
    mm::disposeExternalRCRef(ref);
}

RUNTIME_NOTHROW extern "C" OBJ_GETTER(Kotlin_native_internal_ref_dereferenceExternalRCRef, mm::RawExternalRCRef* ref) {
    AssertThreadState(ThreadState::kRunnable);
    RETURN_OBJ(mm::dereferenceExternalRCRef(ref));
}

RUNTIME_NOTHROW extern "C" void Kotlin_native_internal_ref_retainExternalRCRef(mm::RawExternalRCRef* ref) {
    mm::retainExternalRCRef(ref);
}

RUNTIME_NOTHROW extern "C" void Kotlin_native_internal_ref_releaseExternalRCRef(mm::RawExternalRCRef* ref) {
    mm::releaseExternalRCRef(ref);
}

RUNTIME_NOTHROW extern "C" bool Kotlin_native_internal_ref_tryRetainExternalRCRef(mm::RawExternalRCRef* ref) {
    AssertThreadState(ThreadState::kRunnable);
    ObjHolder holder;
    if (mm::tryRefExternalRCRef(ref, holder.slot())) {
        mm::retainExternalRCRef(ref);
        return true;
    }
    return false;
}

RUNTIME_NOTHROW extern "C" OBJ_GETTER(Kotlin_native_internal_ref_tryDereferenceExternalRCRef, mm::RawExternalRCRef* ref) {
    AssertThreadState(ThreadState::kRunnable);
    RETURN_RESULT_OF(mm::tryRefExternalRCRef, ref);
}

KRef kotlin::mm::externalRCRefAsPermanentObject(mm::RawExternalRCRef* ref) noexcept {
    RuntimeAssert(ref != nullptr, "Cannot handle nullptr");
    if (hasPointerBits(ref, kPermanentTag)) {
        auto obj = reinterpret_cast<KRef>(clearPointerBits(ref, kPermanentTag));
        RuntimeAssert(obj == nullptr || obj->permanent(), "Permanent ExternalRCRef for non-permanent object %p", obj);
        return obj;
    }
    return nullptr;
}

mm::RawExternalRCRef* kotlin::mm::permanentObjectAsExternalRCRef(KRef obj) noexcept {
    RuntimeAssert(obj != nullptr, "Cannot handle nullptr");
    RuntimeAssert(obj->permanent(), "Object %p must be permanent", obj);
    return setPointerBits(reinterpret_cast<mm::RawExternalRCRef*>(obj), kPermanentTag);
}

mm::RawExternalRCRef* kotlin::mm::createRetainedExternalRCRef(KRef obj) noexcept {
    AssertThreadState(ThreadState::kRunnable);

    if (obj == nullptr)
        return nullptr;

    if (obj->permanent()) {
        return mm::permanentObjectAsExternalRCRef(obj);
    }

    return externalRCRef(mm::ThreadRegistry::Instance().CurrentThreadData()->specialRefRegistry().createRef(obj, 1));
}

mm::RawExternalRCRef* kotlin::mm::createUnretainedExternalRCRef(KRef obj) noexcept {
    AssertThreadState(ThreadState::kRunnable);

    if (obj == nullptr)
        return nullptr;

    if (obj->permanent()) {
        return mm::permanentObjectAsExternalRCRef(obj);
    }

    return externalRCRef(mm::ThreadRegistry::Instance().CurrentThreadData()->specialRefRegistry().createRef(obj, 0));
}

void kotlin::mm::retainExternalRCRef(mm::RawExternalRCRefNonPermanent* ref) noexcept {
    if (auto* node = mm::SpecialRefRegistry::Node::fromRaw(ref)) {
        node->retainRef();
    }
}

void kotlin::mm::releaseExternalRCRef(mm::RawExternalRCRefNonPermanent* ref) noexcept {
    if (auto* node = mm::SpecialRefRegistry::Node::fromRaw(ref)) {
        node->releaseRef();
    }
}

void kotlin::mm::disposeExternalRCRef(mm::RawExternalRCRefNonPermanent* ref) noexcept {
    if (auto* node = mm::SpecialRefRegistry::Node::fromRaw(ref)) {
        node->dispose();
    }
}

KRef kotlin::mm::dereferenceExternalRCRef(mm::RawExternalRCRefNonPermanent* ref) noexcept {
    if (auto* node = mm::SpecialRefRegistry::Node::fromRaw(ref)) {
        return node->ref();
    }
    return nullptr;
}

OBJ_GETTER(kotlin::mm::tryRefExternalRCRef, RawExternalRCRefNonPermanent* ref) noexcept {
    AssertThreadState(ThreadState::kRunnable);
    if (auto* node = mm::SpecialRefRegistry::Node::fromRaw(ref)) {
        RETURN_RESULT_OF0(node->tryRef);
    }
    RETURN_OBJ(nullptr);
}
