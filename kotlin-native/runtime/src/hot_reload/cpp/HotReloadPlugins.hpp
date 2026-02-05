/**
 * Copyright 2010-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef HOTRELOAD_PLUGINS_HPP
#define HOTRELOAD_PLUGINS_HPP

#ifdef KONAN_HOT_RELOAD

#include "llvm/ExecutionEngine/Orc/Core.h"
#include "llvm/ExecutionEngine/Orc/ObjectLinkingLayer.h"
#include "llvm/ExecutionEngine/JITLink/JITLink.h"
#include "llvm/ExecutionEngine/JITLink/aarch64.h"

#if defined(__APPLE__)
#include <objc/runtime.h>
#endif

namespace kotlin::hot {
class ObjectManager;

inline constexpr auto MANGLED_KOTLIN_FUN_NAME_PREFIX = "_kfun:";
inline constexpr auto MANGLED_KOTLIN_CLASS_NAME_PREFIX = "_kclass:";

inline constexpr auto MANGLED_KOTLIN_CLASS_PLATFORM_NAME = "_kclass:platform";
inline constexpr auto MANGLED_KOTLIN_FUN_PLATFORM_NAME = "_kfun:platform";

}

namespace kotlin::hot::orc::plugins {

using namespace kotlin::hot;

#if defined(__APPLE__)
// TODO: this is a temporary fix, since MachOPlatform should be handling this...

/// Plugin that registers ObjC selectors with the runtime.
/// This is needed because MachOPlatform's selector fixup may not work correctly
/// for all object files. This plugin explicitly finds selector references and
/// updates them to use the canonical registered selectors.
class ObjCSelectorFixupPlugin : public llvm::orc::ObjectLinkingLayer::Plugin {
public:
    void modifyPassConfig(
            llvm::orc::MaterializationResponsibility& MR,
            llvm::jitlink::LinkGraph& G,
            llvm::jitlink::PassConfiguration& Config) override;

    llvm::Error notifyEmitted(llvm::orc::MaterializationResponsibility& MR) override {
        return llvm::Error::success();
    }

    llvm::Error notifyFailed(llvm::orc::MaterializationResponsibility& MR) override {
        return llvm::Error::success();
    }

    llvm::Error notifyRemovingResources(llvm::orc::JITDylib& JD, llvm::orc::ResourceKey K) override {
        return llvm::Error::success();
    }

    void notifyTransferringResources(
            llvm::orc::JITDylib& JD,
            llvm::orc::ResourceKey DstKey,
            llvm::orc::ResourceKey SrcKey) override {}
};
#endif

class KotlinObjectOverrider : public llvm::orc::ObjectLinkingLayer::Plugin {
public:
    void modifyPassConfig(
            llvm::orc::MaterializationResponsibility& MR,
            llvm::jitlink::LinkGraph& G,
            llvm::jitlink::PassConfiguration& Config) override;

    llvm::Error notifyEmitted(llvm::orc::MaterializationResponsibility& MR) override;

    llvm::Error notifyFailed(llvm::orc::MaterializationResponsibility& MR) override {
        return llvm::Error::success();
    }

    llvm::Error notifyRemovingResources(llvm::orc::JITDylib& JD, llvm::orc::ResourceKey K) override {
        return llvm::Error::success();
    }

    void notifyTransferringResources(
            llvm::orc::JITDylib& JD,
            llvm::orc::ResourceKey DstKey,
            llvm::orc::ResourceKey SrcKey) override {}
private:
    struct DefinitionInfo {
        int redefinitionCount = 1;
        llvm::orc::ExecutorAddr ptrAddress;
    };

    // Maps original function name -> definition info (with resolved ptr address)
    llvm::DenseMap<llvm::orc::SymbolStringPtr, DefinitionInfo> _trackedSymbols;

    // Pending address resolution: pairs of (original func name, ptr symbol)
    // Stored in PostPrunePasses, resolved in PostAllocationPasses (same LinkGraph)
    std::vector<std::pair<llvm::orc::SymbolStringPtr, llvm::jitlink::Symbol*>> _pendingResolution;
};

/// Plugin that collects Kotlin function and class addresses from linked objects.
/// This information is used to update stubs and perform class instance reloading.
class KotlinObjectListener : public llvm::orc::ObjectLinkingLayer::Plugin {
public:
    explicit KotlinObjectListener(ObjectManager& objManager) : _objManager(objManager) {}

    void modifyPassConfig(
            llvm::orc::MaterializationResponsibility& MR,
            llvm::jitlink::LinkGraph& G,
            llvm::jitlink::PassConfiguration& Config) override;

    llvm::Error notifyEmitted(llvm::orc::MaterializationResponsibility& MR) override {
        return llvm::Error::success();
    }

    llvm::Error notifyFailed(llvm::orc::MaterializationResponsibility& MR) override {
        return llvm::Error::success();
    }

    llvm::Error notifyRemovingResources(llvm::orc::JITDylib& JD, llvm::orc::ResourceKey K) override {
        return llvm::Error::success();
    }

    void notifyTransferringResources(
            llvm::orc::JITDylib& JD,
            llvm::orc::ResourceKey DstKey,
            llvm::orc::ResourceKey SrcKey) override {}

private:
    ObjectManager& _objManager;
};

/// Fallback definition generator that provides null/weak definitions for missing symbols.
/// This allows JITLink to complete successfully even when some symbols can't be found.
/// If these symbols are actually called at runtime, they will crash with a clear error.
///
/// This handles:
/// - ObjC notification constants (_NSAccessibility*) - may not be exported from AppKit
/// - System call traps (_mach_vm_*) - may not be available via dlsym
///
/// NOTE: We do NOT handle C++ RTTI symbols (__ZTI*, __ZTS*) here because they are
/// critical for exception handling. Those should be properly exported from stdlib-cache.a.
class WeakSymbolFallbackGenerator : public llvm::orc::DefinitionGenerator {
public:
    WeakSymbolFallbackGenerator() = default;

    llvm::Error tryToGenerate(
            llvm::orc::LookupState& LS,
            llvm::orc::LookupKind K,
            llvm::orc::JITDylib& JD,
            llvm::orc::JITDylibLookupFlags JDLookupFlags,
            const llvm::orc::SymbolLookupSet& Symbols) override;
};

} // namespace kotlin::hot::orc::plugins

#endif // KONAN_HOT_RELOAD

#endif // HOTRELOAD_PLUGINS_HPP
