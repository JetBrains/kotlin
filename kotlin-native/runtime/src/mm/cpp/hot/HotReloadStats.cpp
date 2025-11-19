//
// Created by Gabriele.Pappalardo on 19/11/2025.
//

#include "HotReloadStats.hpp"

#include "KString.h"
#include "concurrent/Mutex.hpp"

namespace kotlin::hot {
    class HotReloader : Pinned {
public:
    static HotReloader& Instance() noexcept;
    StatsCollector statsCollector;
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
        const kotlin::hot::Stats current = kotlin::hot::HotReloader::Instance().statsCollector.getCurrent();
        copy = current;
    }
    copy.build(builder);
}

} // extern "C"

namespace kotlin::hot {

void Stats::build(KRef builder) const noexcept {
    ObjHolder loadedLibraryHolder;

    Kotlin_native_internal_HotReload_HotReloadStatsBuilder_setStartEpoch(builder, start);
    Kotlin_native_internal_HotReload_HotReloadStatsBuilder_setEndEpoch(builder, end);
    Kotlin_native_internal_HotReload_HotReloadStatsBuilder_setReboundSymbols(builder, reboundSymbols);
    Kotlin_native_internal_HotReload_HotReloadStatsBuilder_setSuccessful(builder, wasSuccessful);

    CreateStringFromCString(loadedLibrary.c_str(), loadedLibraryHolder.slot());
    Kotlin_native_internal_HotReload_HotReloadStatsBuilder_setLoadedLibrary(builder, loadedLibraryHolder.obj());
}

void StatsCollector::registerStart(long start) noexcept {
    kCurrent.start = start;
}

void StatsCollector::registerEnd(long end) noexcept {
    kCurrent.end = end;
}

void StatsCollector::registerLoadedLibrary(const std::string& loadedLibrary) noexcept {
    kCurrent.loadedLibrary = loadedLibrary;
}

void StatsCollector::registerReboundSymbols(int reboundSymbols) noexcept {
    kCurrent.reboundSymbols = reboundSymbols;
}

void StatsCollector::registerSuccessful(bool wasSuccessful) noexcept {
    kCurrent.wasSuccessful = wasSuccessful;
}
} // namespace kotlin::hot