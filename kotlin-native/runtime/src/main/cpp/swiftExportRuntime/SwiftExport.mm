/*
 * Copyright 2010-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include <optional>

#include "CompilerConstants.hpp"
#include "ObjCExport.h"
#include "MemoryOrdering.hpp"
#include "std_support/Atomic.hpp"

#if KONAN_OBJC_INTEROP
#include "KotlinBase.h"
#endif

using namespace kotlin;

namespace {

std::optional<std_support::atomic_ref<void*>> convertToRetainedForTypeInfo(TypeInfo* typeInfo) noexcept {
    if (!compiler::swiftExport()) return std::nullopt;
    RuntimeAssert(typeInfo != nullptr, "typeInfo cannot be null");
#if KONAN_OBJC_INTEROP
    return std_support::atomic_ref(typeInfo->writableInfo_->objCExport.convertToRetained);
#else
    return std::nullopt;
#endif
}

} // namespace

extern "C" RUNTIME_NOTHROW void* Kotlin_native_internal_swiftExportRuntime_loadToRetainedSwiftFunPtr(TypeInfo* typeInfo, MemoryOrdering ordering) {
    auto location = convertToRetainedForTypeInfo(typeInfo);
    RuntimeAssert(location, "SwiftExport unavailable");
    return location->load(toStdMemoryOrder(ordering));
}

extern "C" RUNTIME_NOTHROW void Kotlin_native_internal_swiftExportRuntime_storeToRetainedSwiftFunPtr(
        TypeInfo* typeInfo, void* value, MemoryOrdering ordering) {
    auto location = convertToRetainedForTypeInfo(typeInfo);
    RuntimeAssert(location, "SwiftExport unavailable");
    return location->store(value, toStdMemoryOrder(ordering));
}

extern "C" RUNTIME_NOTHROW void* Kotlin_native_internal_swiftExportRuntime_compareAndExchangeToRetainedSwiftFunPtr(
        TypeInfo* typeInfo, void* expected, void* value, MemoryOrdering orderingSuccess, MemoryOrdering orderingFailure) {
    auto location = convertToRetainedForTypeInfo(typeInfo);
    RuntimeAssert(location, "SwiftExport unavailable");
    location->compare_exchange_strong(expected, value, toStdMemoryOrder(orderingSuccess), toStdMemoryOrder(orderingFailure));
    return expected;
}

extern "C" RUNTIME_NOTHROW id SwiftExport_kotlin_Any_toRetainedSwift(void* ref) {
    RuntimeAssert(kotlin::compiler::swiftExport(), "Can only be used with swiftExport");
#if KONAN_OBJC_INTEROP
    return [[KotlinBase alloc] initWithExternalRCRef:reinterpret_cast<uintptr_t>(ref)];
#else
    return nullptr;
#endif
}
