/**
 * Copyright 2010-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
*/

#ifndef HOTRELOAD_HPP
#define HOTRELOAD_HPP

#ifdef KONAN_HOT_RELOAD

namespace kotlin::hot {

/// Public interface for HotReload - does not expose LLVM dependencies.
/// The full implementation with LLVM types is in HotReload.cpp.
class HotReload {
public:
    static void InitModule() noexcept;
};

} // namespace kotlin::hot

extern "C" {
    void Kotlin_native_internal_HotReload_perform(ObjHeader*, const ObjHeader* dylibPath);
    void Kotlin_native_internal_HotReload_invokeSuccessCallback();
}

#endif

#endif // HOTRELOAD_HPP
