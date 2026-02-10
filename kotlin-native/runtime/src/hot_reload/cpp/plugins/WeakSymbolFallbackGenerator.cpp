//
// Created by Gabriele.Pappalardo on 09/02/2026.
//

#include "WeakSymbolFallbackGenerator.hpp"

namespace kotlin::hot::orc::plugins {

extern "C" void weakStubFallbackFactory() {
    std::fprintf(stderr, "error :: Failed to resolve symbol from WeakSymbolFallbackGenerator\n");
    std::abort();
}

llvm::Error WeakSymbolFallbackGenerator::tryToGenerate(
        llvm::orc::LookupState& LS,
        llvm::orc::LookupKind K,
        llvm::orc::JITDylib& JD,
        llvm::orc::JITDylibLookupFlags JDLookupFlags,
        const llvm::orc::SymbolLookupSet& Symbols) {
    llvm::orc::SymbolMap newSymbols;

    for (const auto& [name, flags] : Symbols) {
        auto nameStr = (*name).str();

        // Skip kfun: symbols, they should be resolved from the loaded object files
        if (nameStr.find(kKotlinFunPrefix) != std::string::npos) continue;

        // Skip C++ RTTI symbols, they are critical for exception handling and must be properly exported from stdlib-cache.a
        if (nameStr.find("__ZTI") == 0 || nameStr.find("__ZTS") == 0) {
            // HRLogDebug("WeakSymbolFallbackGenerator: NOT providing fallback for RTTI symbol %s", NameStr.c_str());
            continue;
        }

        if (nameStr.find("_NSAccessibility") == 0 || nameStr.find("_mach_vm_") == 0) {
            // HRLogDebug("WeakSymbolFallbackGenerator: providing null definition for %s", NameStr.c_str());
            newSymbols[name] = {llvm::orc::ExecutorAddr::fromPtr(weakStubFallbackFactory), llvm::JITSymbolFlags::Exported | llvm::JITSymbolFlags::Weak};
        }
    }

    if (!newSymbols.empty()) {
        return JD.define(llvm::orc::absoluteSymbols(std::move(newSymbols)));
    }

    return llvm::Error::success();
}

} // namespace kotlin::hot::orc::plugins