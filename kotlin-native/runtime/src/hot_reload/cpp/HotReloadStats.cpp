/**
* Copyright 2010-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
*/

#include "Common.h"
#include "Memory.h"
#include "Natives.h"
#include "Types.h"

#include "HotReloadStats.hpp"

#include "KString.h"
#include "concurrent/Mutex.hpp"

namespace kotlin::hot {

class HotReloadImpl : Pinned {
public:
    static HotReloadImpl& Instance() noexcept;
    StatsCollector& GetStatsCollector() noexcept;
};

} // namespace kotlin::hot

kotlin::SpinLock lock;

extern "C" {

RUNTIME_NOTHROW void Kotlin_native_internal_HotReload_HotReloadStatsBuilder_setStartEpoch(ObjHeader* thiz, KLong epoch);

RUNTIME_NOTHROW void Kotlin_native_internal_HotReload_HotReloadStatsBuilder_setEndEpoch(ObjHeader* thiz, KLong epoch);

RUNTIME_NOTHROW void Kotlin_native_internal_HotReload_HotReloadStatsBuilder_setLoadedLibrary(ObjHeader* thiz, ObjHeader* path /* kotlin.String */);

RUNTIME_NOTHROW void Kotlin_native_internal_HotReload_HotReloadStatsBuilder_setReboundSymbols(ObjHeader* thiz, KInt symbols);

RUNTIME_NOTHROW void Kotlin_native_internal_HotReload_HotReloadStatsBuilder_setSuccessful(ObjHeader* thiz, KBoolean wasSuccessful);

RUNTIME_NOTHROW void Kotlin_native_internal_HotReload_HotReloadStatsBuilder_setLoadNs(ObjHeader* thiz, KLong ns);

RUNTIME_NOTHROW void Kotlin_native_internal_HotReload_HotReloadStatsBuilder_setStubsNs(ObjHeader* thiz, KLong ns);

RUNTIME_NOTHROW void Kotlin_native_internal_HotReload_HotReloadStatsBuilder_setRedirectNs(ObjHeader* thiz, KLong ns);

RUNTIME_NOTHROW void Kotlin_native_internal_HotReload_HotReloadStatsBuilder_setStateTransferNs(ObjHeader* thiz, KLong ns);

RUNTIME_NOTHROW void Kotlin_native_internal_HotReload_HotReloadStatsBuilder_setRequestParseNs(ObjHeader* thiz, KLong ns);

RUNTIME_NOTHROW void Kotlin_native_internal_HotReload_HotReloadStatsBuilder_setStwWaitNs(ObjHeader* thiz, KLong ns);

RUNTIME_NOTHROW void Kotlin_native_internal_HotReload_HotReloadStatsBuilder_fill(KRef builder) {
    kotlin::hot::Stats copy;
    {
        // TODO: check here?
        kotlin::ThreadStateGuard stateGuard(kotlin::ThreadState::kNative);
        std::lock_guard guard(lock);
        const kotlin::hot::Stats current = kotlin::hot::HotReloadImpl::Instance().GetStatsCollector().GetCurrent();
        copy = current;
    }
    copy.build(builder);
}

} // extern "C"

namespace kotlin::hot {

void Stats::build(KRef builder) const noexcept {
    Kotlin_native_internal_HotReload_HotReloadStatsBuilder_setStartEpoch(builder, start_);
    Kotlin_native_internal_HotReload_HotReloadStatsBuilder_setEndEpoch(builder, end_);
    Kotlin_native_internal_HotReload_HotReloadStatsBuilder_setReboundSymbols(builder, reboundSymbols_);
    Kotlin_native_internal_HotReload_HotReloadStatsBuilder_setSuccessful(builder, wasSuccessful_);
    Kotlin_native_internal_HotReload_HotReloadStatsBuilder_setLoadNs(builder, loadNs_);
    Kotlin_native_internal_HotReload_HotReloadStatsBuilder_setStubsNs(builder, stubsNs_);
    Kotlin_native_internal_HotReload_HotReloadStatsBuilder_setRedirectNs(builder, redirectNs_);
    Kotlin_native_internal_HotReload_HotReloadStatsBuilder_setStateTransferNs(builder, stateTransferNs_);
    Kotlin_native_internal_HotReload_HotReloadStatsBuilder_setRequestParseNs(builder, requestParseNs_);
    Kotlin_native_internal_HotReload_HotReloadStatsBuilder_setStwWaitNs(builder, stwWaitNs_);

    ObjHolder arrayHolder;
    ObjHeader* arrayObj = AllocArrayInstance(theArrayTypeInfo, static_cast<int32_t>(loadedObjects_.size()), arrayHolder.slot());
    ArrayHeader* array = arrayObj->array();
    for (size_t i = 0; i < loadedObjects_.size(); ++i) {
        ObjHolder strHolder;
        CreateStringFromCString(loadedObjects_[i].c_str(), strHolder.slot());
        UpdateHeapRef(ArrayAddressOfElementAt(array, static_cast<KInt>(i)), strHolder.obj());
    }
    Kotlin_native_internal_HotReload_HotReloadStatsBuilder_setLoadedLibrary(builder, arrayObj);
}

void StatsCollector::RegisterStart(long start) noexcept {
    currentStats_.start_ = start;
}

void StatsCollector::RegisterEnd(long end) noexcept {
    currentStats_.end_ = end;
}

void StatsCollector::RegisterLoadedObject(const std::vector<std::string>& loadedObjects) noexcept {
    currentStats_.loadedObjects_ = loadedObjects;
}

void StatsCollector::RegisterReboundSymbols(const int reboundSymbols) noexcept {
    currentStats_.reboundSymbols_ = reboundSymbols;
}

void StatsCollector::RegisterSuccessful(const bool wasSuccessful) noexcept {
    currentStats_.wasSuccessful_ = wasSuccessful;
}

void StatsCollector::RegisterLoadNs(const int64_t ns) noexcept {
    currentStats_.loadNs_ = ns;
}

void StatsCollector::RegisterStubsNs(const int64_t ns) noexcept {
    currentStats_.stubsNs_ = ns;
}

void StatsCollector::RegisterRedirectNs(const int64_t ns) noexcept {
    currentStats_.redirectNs_ = ns;
}

void StatsCollector::RegisterStateTransferNs(const int64_t ns) noexcept {
    currentStats_.stateTransferNs_ = ns;
}

void StatsCollector::RegisterRequestParseNs(const int64_t ns) noexcept {
    currentStats_.requestParseNs_ = ns;
}

void StatsCollector::RegisterStwWaitNs(const int64_t ns) noexcept {
    currentStats_.stwWaitNs_ = ns;
}
} // namespace kotlin::hot