#include "CompactUnwindStripperPlugin.hpp"

namespace kotlin::hot::orc::plugins {

static auto constexpr kCompactUnwindSectionName = "__compact_unwind";
static auto constexpr kUnwindInfoSectionName = "__unwind_info";

void CompactUnwindStripperPlugin::modifyPassConfig(
        llvm::orc::MaterializationResponsibility& MR, llvm::jitlink::LinkGraph& G, llvm::jitlink::PassConfiguration& Config) {

    Config.PrePrunePasses.emplace_back([](llvm::jitlink::LinkGraph& Graph) -> llvm::Error {

        // We remove sections afterwards to avoid iterator invalidation issues.
        llvm::SmallVector<llvm::jitlink::Section*, 2> sectionsToRemove;

        for (auto& section : Graph.sections()) {
            auto sectionName = section.getName();
            if (sectionName.ends_with(kCompactUnwindSectionName) || sectionName.ends_with(kUnwindInfoSectionName)) {
                sectionsToRemove.push_back(&section);
            }
        }

        for (auto* section : sectionsToRemove) {
            Graph.removeSection(*section);
        }

        return llvm::Error::success();
    });
}

} // namespace kotlin::hot::orc::plugins
