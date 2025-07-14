//
// Created by Gabriele.Pappalardo on 19/11/2025.
//
#include "Common.h"
#include "Types.h"

#ifdef KONAN_HOT_RELOAD

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
    ObjHolder loadedLibraryHolder;

    Kotlin_native_internal_HotReload_HotReloadStatsBuilder_setStartEpoch(builder, start_);
    Kotlin_native_internal_HotReload_HotReloadStatsBuilder_setEndEpoch(builder, end_);
    Kotlin_native_internal_HotReload_HotReloadStatsBuilder_setReboundSymbols(builder, reboundSymbols_);
    Kotlin_native_internal_HotReload_HotReloadStatsBuilder_setSuccessful(builder, wasSuccessful_);

    CreateStringFromCString(loadedLibrary_.c_str(), loadedLibraryHolder.slot());
    Kotlin_native_internal_HotReload_HotReloadStatsBuilder_setLoadedLibrary(builder, loadedLibraryHolder.obj());
}

void StatsCollector::RegisterStart(long start) noexcept {
    currentStats_.start_ = start;
}

void StatsCollector::RegisterEnd(long end) noexcept {
    currentStats_.end_ = end;
}

void StatsCollector::RegisterLoadedObject(const std::string& loadedLibrary) noexcept {
    currentStats_.loadedLibrary_ = loadedLibrary;
}

void StatsCollector::RegisterReboundSymbols(int reboundSymbols) noexcept {
    currentStats_.reboundSymbols_ = reboundSymbols;
}

void StatsCollector::RegisterSuccessful(bool wasSuccessful) noexcept {
    currentStats_.wasSuccessful_ = wasSuccessful;
}
} // namespace kotlin::hot

#endif