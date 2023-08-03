/*
 * Copyright 2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef CUSTOM_ALLOC_CPP_CUSTOMFINALIZERPROCESSOR_HPP_
#define CUSTOM_ALLOC_CPP_CUSTOMFINALIZERPROCESSOR_HPP_

#include "AtomicStack.hpp"
#include "ExtraObjectData.hpp"
#include "ExtraObjectPage.hpp"
#include "FinalizerHooks.hpp"

namespace kotlin::alloc {

using FinalizerQueue = kotlin::alloc::AtomicStack<kotlin::alloc::ExtraObjectCell>;

struct FinalizerQueueTraits {
    static bool isEmpty(const FinalizerQueue& queue) noexcept { return queue.isEmpty(); }

    static void add(FinalizerQueue& into, FinalizerQueue from) noexcept { into.TransferAllFrom(std::move(from)); }

    static void process(FinalizerQueue queue) noexcept {
        while (auto* cell = queue.Pop()) {
            auto* extraObject = cell->Data();
            auto* baseObject = extraObject->GetBaseObject();
            RunFinalizers(baseObject);
        }
    }
};

} // namespace kotlin::alloc

#endif // CUSTOM_ALLOC_CPP_CUSTOMFINALIZERPROCESSOR_HPP_
