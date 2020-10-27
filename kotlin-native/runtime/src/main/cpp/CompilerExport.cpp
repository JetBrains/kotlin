/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "Memory.h"

namespace {

template <typename T>
T defaultValue() {
    return T();
}

template <typename Ret, typename... Args>
void ensureUsed(Ret (*f)(Args...)) {
    f(defaultValue<Args>()...);
}

} // namespace

// This is a hack to force clang to emit possibly unused declarations.
// TODO: Make sure this function gets DCE'd in the final binary.
// TODO: Should be done with some sort of annotation on the declaration.
void EnsureDeclarationsEmitted() {
    ensureUsed(AllocInstance);
    ensureUsed(AllocArrayInstance);
    ensureUsed(InitInstance);
    ensureUsed(InitSharedInstance);
    ensureUsed(UpdateHeapRef);
    ensureUsed(UpdateStackRef);
    ensureUsed(UpdateReturnRef);
    ensureUsed(ZeroHeapRef);
    ensureUsed(ZeroArrayRefs);
    ensureUsed(EnterFrame);
    ensureUsed(LeaveFrame);
    ensureUsed(AddTLSRecord);
    ensureUsed(ClearTLSRecord);
    ensureUsed(LookupTLS);
    ensureUsed(MutationCheck);
    ensureUsed(CheckLifetimesConstraint);
    ensureUsed(FreezeSubgraph);
    ensureUsed(FreezeSubgraph);
}
