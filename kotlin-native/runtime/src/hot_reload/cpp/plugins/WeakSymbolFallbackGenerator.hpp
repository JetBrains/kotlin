#ifndef KOTLIN_NATIVE_WEAKSYMBOLFALLBACKGENERATOR_HPP
#define KOTLIN_NATIVE_WEAKSYMBOLFALLBACKGENERATOR_HPP

#include "PluginsCommon.hpp"
#include "../HotReloadInternal.hpp"

namespace kotlin::hot::orc::plugins {

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

#endif // KOTLIN_NATIVE_WEAKSYMBOLFALLBACKGENERATOR_HPP
