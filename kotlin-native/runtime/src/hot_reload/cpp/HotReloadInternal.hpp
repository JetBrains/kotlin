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
#include <optional>
#include <string>
#include <unordered_map>
#include <deque>
#include <vector>

#include "HotReloadServer.hpp"
#include "HotReloadStats.hpp"

#include "llvm/Support/InitLLVM.h"
#include "llvm/Support/TargetSelect.h"
#include "llvm/Support/Error.h"
#include "llvm/ExecutionEngine/Orc/LLJIT.h"
#include "llvm/ExecutionEngine/Orc/LinkGraphLinkingLayer.h"
#include "llvm/ExecutionEngine/Orc/IndirectionUtils.h"
#include "llvm/ExecutionEngine/Orc/RTDyldObjectLinkingLayer.h"
#include "llvm/ExecutionEngine/Orc/Debugging/DebuggerSupportPlugin.h"
#include "llvm/ExecutionEngine/Orc/MachOPlatform.h"
#include "llvm/ExecutionEngine/SectionMemoryManager.h"

typedef int (*KonanStartFunc)(const ObjHeader*);

namespace kotlin::mm {
class ThreadData;
} // namespace kotlin::mm

extern "C" {
    void Kotlin_native_internal_HotReload_perform(ObjHeader*, const ObjHeader* dylibPath);
    void Kotlin_native_internal_HotReload_invokeSuccessCallback();
}

namespace kotlin::hot {

/// Result of parsing an object file for hot reload symbols.
struct ParsedObjectFile {
    std::unique_ptr<llvm::MemoryBuffer> buffer;
    std::vector<std::string> implSymbols;  // List of $hr_impl symbol names
};

struct KotlinObjectFile {
    std::unordered_map<std::string, llvm::orc::ExecutorAddr> functions{};
    std::unordered_map<std::string, llvm::orc::ExecutorAddr> classes{};
};

/// This acts as a collector for newly loaded objects from the JIT engine (through the custom plugin).
class ObjectManager {
public:
    void RegisterKotlinObjectFile(KotlinObjectFile object) noexcept {
        _objects.push_back(std::move(object));
    }

    KotlinObjectFile& GetLatestLoadedObject() noexcept {
        assert(_objects.size() > 0);
        return _objects.back();
    }

    TypeInfo* GetPreviousTypeInfo(const std::string& name) const {
        assert(_objects.size() > 1);
        auto& [_, classes] = _objects.at(_objects.size() - 2);
        if (const auto found = classes.find(name); found != classes.end())
            return found->second.toPtr<TypeInfo*>();
        return nullptr;
    }

private:
    std::deque<KotlinObjectFile> _objects{};
};

/// Full implementation of HotReload with LLVM dependencies.
class HotReloadImpl : private Pinned {
public:
    static HotReloadImpl& Instance() noexcept;

    HotReloadImpl();

    void Reload(const std::string& objectPath) noexcept;

    /// Load bootstrap file and return the Konan_start symbol.
    KonanStartFunc LoadBoostrapFile(const char* boostrapFilePath) const;

    StatsCollector& GetStatsCollector() noexcept;
    void SetupMachOPlatform() const;

private:
    void StartServer();
    void SetupORC();

    // Object loading helpers
    static std::optional<ParsedObjectFile> ParseObjectFile(std::string_view objectPath);
    void EnsurePlaceholderStubs(const std::vector<std::string>& implSymbols, bool checkExisting) const;
    void UpdateStubPointers(llvm::orc::JITDylib& JD, const std::vector<std::string>& implSymbols) const;
    bool LoadObjectFromPath(std::string_view objectPath) const;

#if KONAN_OBJC_INTEROP
    // ObjC interop initialization
    void InitializeObjCUniquePrefixFromJIT() const;
    void InitializeObjCAdaptersFromJIT() const;
#endif

    // Legacy stub management (may be removed)
    void CreateFunctionStubs(const std::unordered_map<std::string, llvm::orc::ExecutorAddr>& functions) const;
    void ReplaceFunctionStubs(const std::unordered_map<std::string, llvm::orc::ExecutorAddr>& pairs) const;

    // Class/instance reloading
    void ReloadClassesAndInstances(mm::ThreadData& currentThreadData, std::unordered_map<std::string, llvm::orc::ExecutorAddr> newClasses) const;
    void Perform(mm::ThreadData& currentThreadData) noexcept;
    static ObjHeader* PerformStateTransfer(mm::ThreadData& currentThreadData, ObjHeader* existingObject, const TypeInfo* newTypeInfo);
    std::vector<ObjHeader*> FindObjectsToReload(const TypeInfo* oldTypeInfo) const;
    static int UpdateHeapReferences(ObjHeader* oldObject, ObjHeader* newObject);
    static void UpdateShadowStackReferences(const ObjHeader* oldObject, ObjHeader* newObject);

    HotReloadServer _server{};
    StatsCollector _statsCollector{};
    ObjectManager _objectManager{};
    std::unique_ptr<llvm::orc::LLJIT> _JIT{};
    std::unique_ptr<llvm::orc::IndirectStubsManager> _ISM{};
    std::unique_ptr<llvm::orc::LazyCallThroughManager> _LCTM{};
};

} // namespace kotlin::hot

#endif

#endif // HOTRELOAD_INTERNAL_HPP