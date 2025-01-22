/*
 * Copyright 2010-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "Types.h"

namespace kotlin::mm {

// TODO(KT-67741): Unify different SpecialRefs

// `RawExternalRCRef*` is kotlin.native.internal.ref.ExternalRCRef
struct RawExternalRCRef;

// Object if the given ExternalRCRef is permanent object, nullptr otherwise.
KRef externalRCRefAsPermanentObject(RawExternalRCRef* ref) noexcept;

// ExternalRCRef for the given permanent object.
RawExternalRCRef* permanentObjectAsExternalRCRef(KRef obj) noexcept;

// TODO: Do we really need to keep this distinction for performance reasons?
// Like `RawExternalRCRef`, but guaranteed to not point to a permanent object.
struct RawExternalRCRefNonPermanent;

inline RawExternalRCRef* externalRCRef(RawExternalRCRefNonPermanent* ref) noexcept {
    return reinterpret_cast<RawExternalRCRef*>(ref);
}

inline RawExternalRCRefNonPermanent* externalRCRefNonPermanent(RawExternalRCRef* ref) noexcept {
    RuntimeAssert(externalRCRefAsPermanentObject(ref) == nullptr, "Creating ExternalRCRefNonPermanent from permanent ExternalRCRef");
    return reinterpret_cast<RawExternalRCRefNonPermanent*>(ref);
}

// Create ExternalRCRef for `obj` with initial refcount=1.
// Can be called from any state.
RawExternalRCRef* createRetainedExternalRCRef(KRef obj) noexcept;

// Create ExternalRCRef for `obj` with initial refcount=0.
// Can be called from any state.
RawExternalRCRef* createUnretainedExternalRCRef(KRef obj) noexcept;

// Retain ExternalRCRef. `ref` must have refcount>0.
// Can be called from any state.
void retainExternalRCRef(RawExternalRCRefNonPermanent* ref) noexcept;

// Retain ExternalRCRef. `ref` must have refcount>0.
// Can be called from any state.
inline void retainExternalRCRef(RawExternalRCRef* ref) noexcept {
    if (ref == nullptr || externalRCRefAsPermanentObject(ref))
        return;
    retainExternalRCRef(externalRCRefNonPermanent(ref));
}

// Release ExternalRCRef. `ref` must have refcount>0.
// Can be called from any state.
void releaseExternalRCRef(RawExternalRCRefNonPermanent* ref) noexcept;

// Release ExternalRCRef. `ref` must have refcount>0.
// Can be called from any state.
inline void releaseExternalRCRef(RawExternalRCRef* ref) noexcept {
    if (ref == nullptr || externalRCRefAsPermanentObject(ref))
        return;
    releaseExternalRCRef(externalRCRefNonPermanent(ref));
}

// Dispose ExternalRCRef. `ref` must have refcount=0 and must not be used after that.
// Can be called from any state.
void disposeExternalRCRef(RawExternalRCRefNonPermanent* ref) noexcept;

// Dispose ExternalRCRef. `ref` must have refcount=0 and must not be used after that.
// Can be called from any state.
inline void disposeExternalRCRef(RawExternalRCRef* ref) noexcept {
    if (ref == nullptr || externalRCRefAsPermanentObject(ref))
        return;
    disposeExternalRCRef(externalRCRefNonPermanent(ref));
}

// Release and dispose ExternalRCRef. `ref` must have refcount=1 and must not be used after that.
// Can be called from any state.
inline void releaseAndDisposeExternalRCRef(RawExternalRCRef* ref) noexcept {
    if (ref == nullptr || externalRCRefAsPermanentObject(ref))
        return;
    auto* nonPermanentRef = externalRCRefNonPermanent(ref);
    releaseExternalRCRef(nonPermanentRef);
    disposeExternalRCRef(nonPermanentRef);
}

// Get underlying object from ExternalRCRef. The return value is safe to use only when refcount>0.
// Can be called from any state.
KRef dereferenceExternalRCRef(RawExternalRCRefNonPermanent* ref) noexcept;

// Get underlying object from ExternalRCRef. The return value is safe to use only when refcount>0.
// Can be called from any state.
inline KRef dereferenceExternalRCRef(RawExternalRCRef* ref) noexcept {
    if (ref == nullptr)
        return nullptr;
    if (auto* obj = externalRCRefAsPermanentObject(ref))
        return obj;
    return dereferenceExternalRCRef(externalRCRefNonPermanent(ref));
}

// Safely get underlying object from ExternalRCRef.
// If the underlying object is alive, returns it. Otherwise returns nullptr.
// Can be called only from runnable state.
OBJ_GETTER(tryRefExternalRCRef, RawExternalRCRefNonPermanent* ref) noexcept;

// Safely get underlying object from ExternalRCRef.
// If the underlying object is alive, returns it. Otherwise returns nullptr.
// Can be called only from runnable state.
inline OBJ_GETTER(tryRefExternalRCRef, RawExternalRCRef* ref) noexcept {
    AssertThreadState(ThreadState::kRunnable);
    if (ref == nullptr)
        RETURN_OBJ(nullptr);
    if (auto obj = mm::externalRCRefAsPermanentObject(ref)) {
        RETURN_OBJ(obj);
    }
    RETURN_RESULT_OF(tryRefExternalRCRef, externalRCRefNonPermanent(ref))
}

}

extern "C" {

RUNTIME_NOTHROW kotlin::mm::RawExternalRCRef* Kotlin_mm_createRetainedExternalRCRef(KRef obj);
RUNTIME_NOTHROW void Kotlin_mm_releaseAndDisposeExternalRCRef(kotlin::mm::RawExternalRCRef* ref);

}
