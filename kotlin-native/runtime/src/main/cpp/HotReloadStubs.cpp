#include "Types.h"
#include "Common.h"

extern "C" {

// TODO(Gabriele): show we wrap in the exceptions in Kotlin ones?

// Weak stub for HotReloadStatsBuilder.fill()
RUNTIME_WEAK void Kotlin_native_internal_HotReload_HotReloadStatsBuilder_fill(KRef builder) {
    throw std::runtime_error{"Hot-Reload is not available on this platform."};
}

// Weak stub for HotReload.perform()
RUNTIME_WEAK void Kotlin_native_internal_HotReload_perform(KRef thiz, KConstRef dylibPath) {
    throw std::runtime_error{"Hot-Reload is not available on this platform."};
}

RUNTIME_WEAK void Kotlin_native_internal_HotReload_HotReloadStatsBuilder_setStartEpoch(ObjHeader* thiz, KLong epoch) {
    /* not implemented */
}

RUNTIME_WEAK void Kotlin_native_internal_HotReload_HotReloadStatsBuilder_setEndEpoch(ObjHeader* thiz, KLong epoch) {
    /* not implemented */
}

RUNTIME_WEAK void Kotlin_native_internal_HotReload_HotReloadStatsBuilder_setLoadedLibrary(
        ObjHeader* thiz, ObjHeader* path /* kotlin.String */) {
    /* not implemented */
}

RUNTIME_WEAK void Kotlin_native_internal_HotReload_HotReloadStatsBuilder_setReboundSymbols(ObjHeader* thiz, KInt symbols) {
    /* not implemented */
}

RUNTIME_WEAK void Kotlin_native_internal_HotReload_HotReloadStatsBuilder_setSuccessful(ObjHeader* thiz, KBoolean wasSuccessful) {
    /* not implemented */
}
}