#include "KotlinSymbolExternalizerPlugin.hpp"
#include "PluginsCommon.hpp"
#include "llvm/ExecutionEngine/Orc/Shared/MachOObjectFormat.h"

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

        // IMPORTANT note on exceptions and stack unwinding:
        // To keep exceptions working, we need to skip MachOCompactUnwindSectionName and
        // MachOEHFrameSectionName sections. Their edges must point to the actual function implementation,
        // not the stub! The unwinder uses these addresses to find unwind info for the current PC.

        for (auto& section : Graph.sections()) {
            const auto sectionName = section.getName();
            if (sectionName == llvm::orc::MachOCompactUnwindSectionName ||
                sectionName == llvm::orc::MachOEHFrameSectionName)
                continue;

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

    if (&MR.getTargetJITDylib() == &stubsJD_) return;
    // Ignore symbols coming from cache artifacts (i.e., non user-defined code)
    if (G.getName().find("-cache.a") != std::string::npos) return;

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