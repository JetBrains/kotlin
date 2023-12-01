/*
 * Copyright 2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef CUSTOM_ALLOC_CPP_CUSTOMFINALIZERPROCESSOR_HPP_
#define CUSTOM_ALLOC_CPP_CUSTOMFINALIZERPROCESSOR_HPP_

#include "Allocator.hpp"
#include "AtomicStack.hpp"
#include "ExtraObjectCell.hpp"
#include "FinalizerHooks.hpp"
#include "SegregatedFinalizerQueue.hpp"

namespace kotlin::alloc {

using FinalizerQueueSingle = AtomicStack<ExtraObjectCell>;
using FinalizerQueue = SegregatedFinalizerQueue<FinalizerQueueSingle>;

struct FinalizerQueueTraits {
    static bool isEmpty(const FinalizerQueueSingle& queue) noexcept { return queue.isEmpty(); }

    static void add(FinalizerQueueSingle& into, FinalizerQueueSingle from) noexcept { into.TransferAllFrom(std::move(from)); }

    static void process(FinalizerQueueSingle queue) noexcept {
        while (processSingle(queue)) {
        }
    }

    static bool processSingle(FinalizerQueueSingle& queue) noexcept {
        if (auto* cell = queue.Pop()) {
            auto* extraObject = cell->Data();
            if (auto* baseObject = extraObject->GetBaseObject()) {
                RunFinalizers(baseObject);
            } else {
                // This `ExtraObjectData` does not have an object attached. This means
                // that the only finalization step is destroying it.
                destroyExtraObjectData(*extraObject);
            }
            return true;
        }
        return false;
    }
};

} // namespace kotlin::alloc

#endif // CUSTOM_ALLOC_CPP_CUSTOMFINALIZERPROCESSOR_HPP_
