/**
 * Copyright 2010-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 *
 * Internal header for HotReload - exposes full implementation with LLVM dependencies.
 * Only include this in modules that need the full HotReload functionality and are
 * compiled with LLVM headers available.
 */

#ifndef HOTRELOAD_INTERNAL_HPP
#define HOTRELOAD_INTERNAL_HPP

#ifdef KONAN_HOT_RELOAD

#include <memory>
#include <string>
#include <vector>

#include "HotReloadServer.hpp"
#include "HotReloadStats.hpp"

#include "llvm/Support/Error.h"
#include "llvm/ExecutionEngine/Orc/LLJIT.h"
#include "llvm/ExecutionEngine/Orc/RTDyldObjectLinkingLayer.h"
#include "llvm/ExecutionEngine/Orc/JITLinkRedirectableSymbolManager.h"

typedef int (*KonanStartFunc)(const ObjHeader*);

namespace kotlin::mm {
class ThreadData;
} // namespace kotlin::mm

extern "C" {
    void Kotlin_native_internal_HotReload_perform(ObjHeader*, const ObjHeader* dylibPath);
    void Kotlin_native_internal_HotReload_invokeSuccessCallback();
}

namespace kotlin::hot {

struct KotlinObjectFile {
    std::vector<std::string> functions{};
    std::vector<std::string> classes{};
};

/// Full implementation of HotReload with LLVM dependencies.
class HotReloadImpl : private Pinned {
public:
    static HotReloadImpl& Instance() noexcept;

    HotReloadImpl();

    void Reload(const std::string& objectPath) noexcept;

    /// Load bootstrap file and return the Konan_start symbol.
    KonanStartFunc LoadBootstrapFile(const char* bootstrapFilePath);

    StatsCollector& GetStatsCollector() noexcept;

private:
    void StartServer();
    void SetupORC();

    KotlinObjectFile ParseKotlinObjectFile(const llvm::MemoryBufferRef& Buf) const;
    llvm::Error CreateRedirectableStubs(const std::vector<std::string>& functionSymbols);
    llvm::Error RedirectStubsToImpl(llvm::orc::JITDylib& JD, const std::vector<std::string>& symbolNames) const;

    static std::unique_ptr<llvm::MemoryBuffer> ReadObjectFileFromPath(std::string_view objectPath);
    bool LoadObjectAndUpdateFunctionPointers(std::string_view objectPath);

#if KONAN_OBJC_INTEROP
    void InitializeObjCUniquePrefixFromJIT(llvm::orc::JITDylib& BootstrapJD) const;
    void InitializeObjCAdaptersFromJIT(llvm::orc::JITDylib& BootstrapJD) const;
#endif

    // Class/instance reloading
    void ReloadClassesAndInstances(mm::ThreadData& currentThreadData) const;
    void Perform(mm::ThreadData& currentThreadData) const;
    std::vector<ObjHeader*> FindObjectsToReload(const TypeInfo* oldTypeInfo) const;

    static ObjHeader* PerformStateTransfer(mm::ThreadData& currentThreadData, ObjHeader* existingObject, const TypeInfo* newTypeInfo);
    static int UpdateHeapReferences(ObjHeader* oldObject, ObjHeader* newObject);
    static void UpdateShadowStackReferences(const ObjHeader* oldObject, ObjHeader* newObject);

    llvm::orc::JITDylib* getStubsJD() const;

    HotReloadServer server_{};
    StatsCollector statsCollector_{};

    std::unique_ptr<llvm::orc::LLJIT> jit_{};
    std::unique_ptr<llvm::orc::RedirectableSymbolManager> rsm_{};
    llvm::DenseSet<llvm::orc::SymbolStringPtr> redirectableSymbols_;
    std::vector<llvm::orc::JITDylib*> jds_;

    std::unique_ptr<KotlinObjectFile> latestLoadedObject_{};
};

} // namespace kotlin::hot

#endif

#endif // HOTRELOAD_INTERNAL_HPP