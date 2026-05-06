#ifndef KOTLIN_NATIVE_MACHOHOSTDATASYMBOLGENERATOR_HPP
#define KOTLIN_NATIVE_MACHOHOSTDATASYMBOLGENERATOR_HPP

#include "PluginsCommon.hpp"
#include "../HotReloadInternal.hpp"

namespace kotlin::hot::orc::plugins {

/// Provides host binary symbols that dlsym() misses.
/// Indexes ALL defined symbols from the host Mach-O binary, including local/hidden
/// symbols (e.g. from static caches compiled with hidden visibility).
/// dlsym() only sees exported symbols, so this generator is needed for symbols
/// that were linked with hidden visibility (private external → local in the final binary).
class MachOHostDataSymbolGenerator : public llvm::orc::DefinitionGenerator {
public:
    static llvm::Expected<std::unique_ptr<MachOHostDataSymbolGenerator>>
    CreateForCurrentProcess();

    llvm::Error tryToGenerate(
            llvm::orc::LookupState& LS,
            llvm::orc::LookupKind K,
            llvm::orc::JITDylib& JD,
            llvm::orc::JITDylibLookupFlags JDLookupFlags,
            const llvm::orc::SymbolLookupSet& Symbols) override;

private:
    explicit MachOHostDataSymbolGenerator(llvm::StringMap<llvm::orc::ExecutorAddr> Symbols)
        : DataSymbols_(std::move(Symbols)) {}

    llvm::StringMap<llvm::orc::ExecutorAddr> DataSymbols_;
};

} // namespace kotlin::hot::orc::plugins

#endif