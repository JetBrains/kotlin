#ifdef KONAN_HOT_RELOAD

#ifndef KOTLIN_NATIVE_MACHOHOSTDATASYMBOLGENERATOR_HPP
#define KOTLIN_NATIVE_MACHOHOSTDATASYMBOLGENERATOR_HPP

#include "PluginsCommon.hpp"
#include "../HotReloadInternal.hpp"

namespace kotlin::hot::orc::plugins {

/// Provides host binary symbols that dlsym() misses (data symbols not in export trie).
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
#endif