/**
 * Copyright 2010-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 *
 * JIT plugins for the HotReload system.
 */

#ifndef HOTRELOAD_PLUGINS_HPP
#define HOTRELOAD_PLUGINS_HPP

#ifdef KONAN_HOT_RELOAD

#include "llvm/ExecutionEngine/Orc/Core.h"
#include "llvm/ExecutionEngine/Orc/ObjectLinkingLayer.h"
#include "llvm/ExecutionEngine/Orc/IndirectionUtils.h"
#include "llvm/ExecutionEngine/JITLink/JITLink.h"

#if defined(__APPLE__)
#include <objc/runtime.h>
#endif

namespace kotlin::hot {

// Forward declaration
class ObjectManager;

/// Constants for symbol name patterns
inline constexpr const char* MANGLED_FUN_NAME_PREFIX = "_kfun:";
inline constexpr const char* MANGLED_CLASS_NAME_PREFIX = "_kclass:";
inline constexpr std::string_view IMPL_SUFFIX = "$hr_impl";

#if defined(__APPLE__)
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

/// Plugin that collects Kotlin function and class addresses from linked objects.
/// This information is used to update stubs and perform class instance reloading.
class LatestObjectListener : public llvm::orc::ObjectLinkingLayer::Plugin {
public:
    explicit LatestObjectListener(ObjectManager& objManager) : _objManager(objManager) {}

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

/// Definition generator that resolves stable function names to their stub addresses.
/// When JITLink tries to resolve "kfun:foo", this generator returns the pre-created stub address.
/// NOTE: Stubs must be created BEFORE objects are added to the JIT.
class StubDefinitionGenerator : public llvm::orc::DefinitionGenerator {
public:
    explicit StubDefinitionGenerator(llvm::orc::IndirectStubsManager& ISM) : _ISM(ISM) {}

    llvm::Error tryToGenerate(
            llvm::orc::LookupState& LS,
            llvm::orc::LookupKind K,
            llvm::orc::JITDylib& JD,
            llvm::orc::JITDylibLookupFlags JDLookupFlags,
            const llvm::orc::SymbolLookupSet& Symbols) override;

private:
    llvm::orc::IndirectStubsManager& _ISM;
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

} // namespace kotlin::hot

#endif // KONAN_HOT_RELOAD

#endif // HOTRELOAD_PLUGINS_HPP
