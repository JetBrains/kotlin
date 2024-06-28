/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "ExtraObjectDataFactory.hpp"
#include "FinalizerHooks.hpp"
#include "GC.hpp"
#include "GCStatistics.hpp"
#include "SegregatedFinalizerQueue.hpp"

namespace kotlin::alloc {

template <typename Traits>
void SweepExtraObjects(gc::GCHandle handle, typename Traits::ExtraObjectsFactory::Iterable& factoryIter) noexcept {
    auto sweepHandle = handle.sweepExtraObjects();
    factoryIter.ApplyDeletions();
    for (auto it = factoryIter.begin(); it != factoryIter.end();) {
        auto &extraObject = *it;
        if (!extraObject.getFlag(mm::ExtraObjectData::FLAGS_IN_FINALIZER_QUEUE) && !Traits::IsMarkedByExtraObject(extraObject)) {
            extraObject.ClearRegularWeakReferenceImpl();
            if (extraObject.HasAssociatedObject()) {
                extraObject.setFlag(mm::ExtraObjectData::FLAGS_IN_FINALIZER_QUEUE);
                ++it;
                sweepHandle.addKeptObject(sizeof(mm::ExtraObjectData));
            } else {
                extraObject.Uninstall();
                it.EraseAndAdvance();
                sweepHandle.addSweptObject();
            }
        } else {
            ++it;
            sweepHandle.addKeptObject(sizeof(mm::ExtraObjectData));
        }
    }
}

template <typename Traits>
void SweepExtraObjects(gc::GCHandle handle, typename Traits::ExtraObjectsFactory& factory) noexcept {
    auto iter = factory.LockForIter();
    return SweepExtraObjects<Traits>(handle, iter);
}

template <typename Traits>
SegregatedFinalizerQueue<typename Traits::ObjectFactory::FinalizerQueue> Sweep(
        gc::GCHandle handle, typename Traits::ObjectFactory::Iterable& objectFactoryIter) noexcept {
    SegregatedFinalizerQueue<typename Traits::ObjectFactory::FinalizerQueue> finalizerQueue;
    auto sweepHandle = handle.sweep();

    for (auto it = objectFactoryIter.begin(); it != objectFactoryIter.end();) {
        auto* objHeader = it->GetObjHeader();
        if (Traits::TryResetMark(*it)) {
            ++it;
            sweepHandle.addKeptObject(Traits::ObjectFactory::GetAllocatedHeapSize(objHeader));
            continue;
        }
        sweepHandle.addSweptObject();
        if (HasFinalizers(objHeader)) {
            auto* extraObject = mm::ExtraObjectData::Get(objHeader);
            if (compiler::objcDisposeOnMain() && extraObject && extraObject->getFlag(mm::ExtraObjectData::FLAGS_RELEASE_ON_MAIN_QUEUE)) {
                objectFactoryIter.MoveAndAdvance(finalizerQueue.mainThread, it);
            } else {
                objectFactoryIter.MoveAndAdvance(finalizerQueue.regular, it);
            }
        } else {
            objectFactoryIter.EraseAndAdvance(it);
        }
    }

    return finalizerQueue;
}

template <typename Traits>
SegregatedFinalizerQueue<typename Traits::ObjectFactory::FinalizerQueue> Sweep(
        gc::GCHandle handle, typename Traits::ObjectFactory& objectFactory) noexcept {
    auto iter = objectFactory.LockForIter();
    return Sweep<Traits>(handle, iter);
}

template <typename T>
struct DefaultSweepTraits {
    using ObjectFactory = T;
    using ExtraObjectsFactory = ExtraObjectDataFactory;

    static bool IsMarkedByExtraObject(mm::ExtraObjectData& object) noexcept {
        auto* baseObject = object.GetBaseObject();
        if (!baseObject->heap()) return true;
        return gc::isMarked(baseObject);
    }

    static bool TryResetMark(typename ObjectFactory::NodeRef node) noexcept { return gc::tryResetMark(node.ObjectData()); }
};

}
