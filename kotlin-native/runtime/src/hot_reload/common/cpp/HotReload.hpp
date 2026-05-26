/**
 * Copyright 2010-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
*/

#ifndef HOTRELOAD_HPP
#define HOTRELOAD_HPP

#ifdef KONAN_HOT_RELOAD

#include <string_view>

#include "Memory.h"
#include "Types.h"

namespace kotlin::hot {

using KonanStartFn = KInt(*)(const ObjHeader*);

class HotReload : private Pinned {
public:
    static void InitModule() noexcept;

    static HotReload& Instance() noexcept;

    void LoadBootstrapFile(std::string_view bootstrapFilePath);

    KonanStartFn LookupForKonanStart() const;
};

} // namespace kotlin::hot

extern "C" {
    void Kotlin_native_internal_HotReload_perform(ObjHeader*, const ObjHeader*);
    void Kotlin_native_internal_HotReload_invokeReloadSuccessHandler();
    void* KNHR_LoadObjCStubAddress(const char* name);
}

#endif

#endif // HOTRELOAD_HPP
