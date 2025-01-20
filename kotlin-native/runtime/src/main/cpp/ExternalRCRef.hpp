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

// Create ExternalRCRef for `obj` with initial refcount=1.
// Can be called only from runnable state.
RawExternalRCRef* createRetainedExternalRCRef(KRef obj) noexcept;

// Create ExternalRCRef for `obj` with initial refcount=0.
// Can be called only from runnable state.
RawExternalRCRef* createUnretainedExternalRCRef(KRef obj) noexcept;

// Retain ExternalRCRef. `ref` must have refcount>0.
// Can be called from any state.
void retainExternalRCRef(RawExternalRCRef* ref) noexcept;

// Release ExternalRCRef. `ref` must have refcount>0.
// Can be called from any state.
void releaseExternalRCRef(RawExternalRCRef* ref) noexcept;

// Dispose ExternalRCRef. `ref` must have refcount=0 and must not be used after that.
// Can be called from any state.
void disposeExternalRCRef(RawExternalRCRef* ref) noexcept;

// Release and dispose ExternalRCRef. `ref` must have refcount=1 and must not be used after that.
// Can be called from any state.
void releaseAndDisposeExternalRCRef(RawExternalRCRef* ref) noexcept;

// Get underlying object from ExternalRCRef. The return value is safe to use only when refcount>0.
// Can be called from any state.
KRef dereferenceExternalRCRef(RawExternalRCRef* ref) noexcept;

// Safely get underlying object from ExternalRCRef.
// If the underlying object is alive, returns it. Otherwise returns nullptr.
// Can be called only from runnable state.
OBJ_GETTER(tryRefExternalRCRef, RawExternalRCRef* ref) noexcept;

}
