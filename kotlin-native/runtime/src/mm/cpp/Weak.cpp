/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "Weak.hpp"

#include "ExternalRCRef.hpp"
#include "ExtraObjectData.hpp"
#include "ThreadState.hpp"
#include "Types.h"

using namespace kotlin;

extern "C" {
OBJ_GETTER(makeRegularWeakReferenceImpl, KRef, void*);
}

namespace {

struct RegularWeakReferenceImpl {
    ObjHeader header;
    mm::RawExternalRCRef* weakRef;
    void* referred;
};

RegularWeakReferenceImpl* asRegularWeakReferenceImpl(ObjHeader* weakRef) noexcept {
    return reinterpret_cast<RegularWeakReferenceImpl*>(weakRef);
}

} // namespace

OBJ_GETTER(mm::createRegularWeakReferenceImpl, ObjHeader* object) noexcept {
    auto* thread = mm::ThreadRegistry::Instance().CurrentThreadData();
    AssertThreadState(thread, ThreadState::kRunnable);

    auto& extraObject = mm::ExtraObjectData::GetOrInstall(object);
    if (auto* weakRef = extraObject.GetRegularWeakReferenceImpl()) {
        RETURN_OBJ(weakRef);
    }
    ObjHolder holder;
    auto* weakRef = makeRegularWeakReferenceImpl(object, object, holder.slot());
    auto* setWeakRef = extraObject.GetOrSetRegularWeakReferenceImpl(object, weakRef);
    RETURN_OBJ(setWeakRef);
}

void mm::disposeRegularWeakReferenceImpl(ObjHeader* weakRef) noexcept {
    mm::disposeExternalRCRef(asRegularWeakReferenceImpl(weakRef)->weakRef);
}

ObjHeader* mm::regularWeakReferenceImplBaseObjectUnsafe(ObjHeader* weakRef) noexcept {
    return static_cast<ObjHeader*>(asRegularWeakReferenceImpl(weakRef)->referred);
}
