#include "KotlinSymbolExternalizerPlugin.hpp"
#include "PluginsCommon.hpp"

namespace kotlin::hot::orc::plugins {

constexpr auto kCallableExportedFlags = llvm::JITSymbolFlags::Callable | llvm::JITSymbolFlags::Exported;

struct SymbolRename {
    llvm::jitlink::Symbol* symbol;
    llvm::orc::SymbolStringPtr originalSymName;
    llvm::orc::SymbolStringPtr implSymName;
};

static std::vector<SymbolRename> findSymbolsToExternalize(llvm::jitlink::LinkGraph& G) {
    std::vector<SymbolRename> toExternalize{};

    // Collect _kfun: defined symbols that need externalizing
    for (auto* symbol : G.defined_symbols()) {
        if (!symbol->hasName()) continue;
        if (symbol->getScope() == llvm::jitlink::Scope::Local) continue;

        llvm::StringRef Name = *symbol->getName();
        if (!Name.starts_with(kKotlinFunPrefix)) continue;

        // TODO: Skip platform interop symbols, they don't need redirection (should be done at compiler level, not runtime).
        if (Name.starts_with("_kfun:platform.")) continue;

        auto originalName = G.getSymbolStringPool()->intern(Name);
        auto implementationName = G.getSymbolStringPool()->intern((Name + kImplSymbolSuffix).str());

        toExternalize.push_back({symbol, std::move(originalName), std::move(implementationName)});
    }

    return toExternalize;
}

/// Rename definitions and retarget edges.
static void retargetEdges(llvm::jitlink::LinkGraph& Graph, const std::vector<SymbolRename>& toExternalize) {
    for (const auto& [symbol, originalSymName, implSymName] : toExternalize) {

        // Create external symbol for the original name
        auto& externalSymbol = Graph.addExternalSymbol(*originalSymName, 0, false);

        // Retarget all edges pointing to this symbol → point to external instead
        for (auto& section : Graph.sections()) {
            for (auto* block : section.blocks()) {
                for (auto& edge : block->edges()) {
                    if (&edge.getTarget() == symbol) {
                        edge.setTarget(externalSymbol);
                    }
                }
            }
        }

        // Rename the definition to $knhr, keep it visible so MR can emit it
        symbol->setName(implSymName);
        symbol->setScope(llvm::jitlink::Scope::Default);
    }
}

void KotlinSymbolExternalizerPlugin::modifyPassConfig(
        llvm::orc::MaterializationResponsibility& MR, llvm::jitlink::LinkGraph& G, llvm::jitlink::PassConfiguration& Config) {

    // Skip StubsJD materializations, those are the stubs themselves
    if (&MR.getTargetJITDylib() == &stubsJD_) return;

    // Note from the documentation: graph nodes still have their original vmaddrs.
    Config.PrePrunePasses.emplace_back([&MR, this](llvm::jitlink::LinkGraph& Graph) -> llvm::Error {

        const auto toExternalize = findSymbolsToExternalize(Graph);
        if (toExternalize.empty()) return llvm::Error::success();

        // Register $knhr names with MR and hand back originals as reexports from StubsJD
        llvm::orc::SymbolFlagsMap implFlags;
        llvm::orc::SymbolAliasMap aliases;

        for (auto& entry : toExternalize) {
            implFlags[entry.implSymName] = kCallableExportedFlags;
            aliases[entry.originalSymName] = llvm::orc::SymbolAliasMapEntry(entry.originalSymName, kCallableExportedFlags);
        }

        // Tell MR we're defining the new $knhr impl names
        if (auto err = MR.defineMaterializing(std::move(implFlags))) {
            return err;
        }

        // Hand back original names, they'll resolve from StubsJD via reexports
        if (auto err = MR.replace(llvm::orc::reexports(stubsJD_, std::move(aliases)))) {
            return err;
        }

        retargetEdges(Graph, toExternalize);

        HRLogDebug("KotlinSymbolExternalizerPlugin: Externalized %zu symbols", toExternalize.size());
        return llvm::Error::success();
    });
}

} // namespace kotlin::hot::orc::plugins