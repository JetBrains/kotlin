#include "Types.h"
#include "Common.h"

extern "C" {

// Weak stub for HotReloadStatsBuilder.fill()
// referenced from kotlin.native.runtime.HotReloadStatsBuilder.build()
RUNTIME_WEAK void Kotlin_native_internal_HotReload_HotReloadStatsBuilder_fill(KRef builder) {
    throw std::runtime_error{"Hot-Reload is available only on macOS and iOS."};
}

// Weak stub for HotReload.perform()
RUNTIME_WEAK void Kotlin_native_internal_HotReload_perform(KRef thiz, KConstRef dylibPath) {
    throw std::runtime_error{"Hot-Reload is available only on macOS and iOS."};
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