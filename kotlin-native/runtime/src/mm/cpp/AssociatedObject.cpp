/*
 * Copyright 2010-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ExtraObjectData.hpp"
#include "MemoryOrdering.hpp"
#include "Types.h"

using namespace kotlin;

namespace {

std::atomic<void*>& associatedObjectForObject(KRef ref) noexcept {
    return mm::ExtraObjectData::GetOrInstall(ref).AssociatedObject();
}

} // namespace

extern "C" RUNTIME_NOTHROW void* Kotlin_native_internal_ref_loadAssociatedObject(KRef ref, MemoryOrdering ordering) {
    auto& location = associatedObjectForObject(ref);
    return location.load(toStdMemoryOrder(ordering));
}

extern "C" RUNTIME_NOTHROW void Kotlin_native_internal_ref_storeAssociatedObject(KRef ref, void* value, MemoryOrdering ordering) {
    auto& location = associatedObjectForObject(ref);
    return location.store(value, toStdMemoryOrder(ordering));
}

extern "C" RUNTIME_NOTHROW void* Kotlin_native_internal_ref_compareAndExchangeAssociatedObject(
        KRef ref, void* expected, void* value, MemoryOrdering orderingSuccess, MemoryOrdering orderingFailure) {
    auto& location = associatedObjectForObject(ref);
    location.compare_exchange_strong(expected, value, toStdMemoryOrder(orderingSuccess), toStdMemoryOrder(orderingFailure));
    return expected;
}
