#include "HotReloadInternal.hpp"
#include "HotReloadUtility.hpp"
#include "plugins/PluginsCommon.hpp"

using namespace kotlin::hot;

KotlinObjectFile KotlinObjectFile::Create(const llvm::MemoryBufferRef& Buf, std::shared_ptr<llvm::orc::SymbolStringPool> SSP) {
    KotlinObjectFile result{};
    auto graphOrErr = llvm::jitlink::createLinkGraphFromObject(Buf, std::move(SSP));

    if (!graphOrErr) {
        HRLogError("Failed to create link graph from object file: %s", llvm::toString(graphOrErr.takeError()).c_str());
        return result;
    }

    const auto& linkGraph = *graphOrErr;
    for (auto& section : linkGraph->sections()) {
        for (const auto* symbol : section.symbols()) {
            if (!symbol->hasName()) continue;
            if (symbol->getScope() == llvm::jitlink::Scope::Local) continue;

            const auto symbolName = *symbol->getName();

            if (symbolName.starts_with(kKotlinFunPrefix)) {
                result.functions.push_back(symbolName.str());
            } else if (symbolName.starts_with(kKotlinClassPrefix)) {
                result.classes.push_back(symbolName.str());
            }
        }
    }

    return result;
}
