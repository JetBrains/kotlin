/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_GC_COMMON_MARK_AND_SWEEP_UTILS_H
#define RUNTIME_GC_COMMON_MARK_AND_SWEEP_UTILS_H

#include "ExtraObjectData.hpp"
#include "FinalizerHooks.hpp"
#include "Memory.h"
#include "ObjectTraversal.hpp"
#include "Runtime.h"
#include "Types.h"

namespace kotlin {
namespace gc {

// TODO: Because of `graySet` this implementation may allocate heap memory during GC.
template <typename Traits>
void Mark(KStdVector<ObjHeader*> graySet) noexcept {
    while (!graySet.empty()) {
        ObjHeader* top = graySet.back();
        graySet.pop_back();

        RuntimeAssert(!isNullOrMarker(top), "Got invalid reference %p in gray set", top);
        RuntimeAssert(!top->local(), "TODO: Stack objects are not supported yet, top=%p", top);

        if (top->heap()) {
            if (!Traits::TryMark(top)) {
                continue;
            }
        }

        if (!top->permanent()) {
            traverseReferredObjects(top, [&graySet](ObjHeader* field) noexcept {
                if (!isNullOrMarker(field) && !field->permanent() && !Traits::IsMarked(field)) {
                    graySet.push_back(field);
                }
            });
        }

        if (auto* extraObjectData = mm::ExtraObjectData::Get(top)) {
            auto* weakCounter = *extraObjectData->GetWeakCounterLocation();
            if (!isNullOrMarker(weakCounter)) {
                graySet.push_back(weakCounter);
            }
        }
    }
}

template <typename Traits>
typename Traits::ObjectFactory::FinalizerQueue Sweep(typename Traits::ObjectFactory& objectFactory) noexcept {
    typename Traits::ObjectFactory::FinalizerQueue finalizerQueue;

    auto iter = objectFactory.Iter();
    for (auto it = iter.begin(); it != iter.end();) {
        if (Traits::TryResetMark(*it)) {
            ++it;
            continue;
        }
        auto* objHeader = it->IsArray() ? it->GetArrayHeader()->obj() : it->GetObjHeader();
        if (auto* extraObject = mm::ExtraObjectData::Get(objHeader)) {
            extraObject->ClearWeakReferenceCounter();
        }
        if (HasFinalizers(objHeader)) {
            iter.MoveAndAdvance(finalizerQueue, it);
        } else {
            iter.EraseAndAdvance(it);
        }
    }

    return finalizerQueue;
}

} // namespace gc
} // namespace kotlin

#endif // RUNTIME_GC_COMMON_MARK_AND_SWEEP_UTILS_H
