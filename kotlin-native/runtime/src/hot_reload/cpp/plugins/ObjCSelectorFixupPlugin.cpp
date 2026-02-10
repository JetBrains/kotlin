#include "ObjCSelectorFixupPlugin.hpp"
#include "HotReloadInternal.hpp"

#if defined(__APPLE__)
#include <objc/runtime.h>
#endif

namespace kotlin::hot::orc::plugins {

using MethnameMap = std::unordered_map<uint64_t, std::string>;

static auto constexpr kDataObjcSelrefsSection = "__DATA,__objc_selrefs";
static auto constexpr kObjcSelrefsSection = "__objc_selrefs";

static auto constexpr kTextObjcMethnameSection = "__TEXT,__objc_methname";
static auto constexpr kObjcMethnameSection = "__objc_methname";

static llvm::jitlink::Section* findObjCSelectorSection(llvm::jitlink::LinkGraph& G) {
    // Find __objc_selrefs section, this contains pointers to selector strings.
    // JITLink uses segment,section naming convention: "__DATA,__objc_selrefs"
    llvm::jitlink::Section* selrefsSection = nullptr;

    for (auto& section : G.sections()) {
        auto sectionName = section.getName();
        if (sectionName == kDataObjcSelrefsSection || sectionName == kObjcSelrefsSection ||
            sectionName.ends_with(kObjcSelrefsSection)) {
            selrefsSection = &section;
            HRLogDebug("ObjCSelectorFixupPlugin: Found selector section: %s", std::string(sectionName).c_str());
            break;
        }
    }


    return selrefsSection;
}

static MethnameMap buildMethnameStringMap(llvm::jitlink::LinkGraph& G) {
    // First, build a map from methname block addresses to their string content.
    // __objc_methname section contains the actual selector strings.
    // Use uint64_t as key since ExecutorAddr doesn't have std::hash.
    std::unordered_map<uint64_t, std::string> methnameStrings{};

    for (auto& section : G.sections()) {
        auto sectionName = section.getName();
        if (sectionName == kTextObjcMethnameSection || sectionName == kObjcMethnameSection ||
            sectionName.ends_with(kObjcMethnameSection)) {
            for (const auto* block : section.blocks()) {
                auto content = block->getContent();
                if (!content.empty()) {
                    // The content is the null-terminated string
                    const std::string methName(content.data(), strnlen(content.data(), content.size()));
                    methnameStrings[block->getAddress().getValue()] = methName;
                    // HRLogDebug("  methname @ 0x%llx: '%s'", block->getAddress().getValue(), str.c_str());
                }
            }
        }
    }

    return methnameStrings;
}

static std::string tryToFindSelectoViaGraphEdges(const MethnameMap& methnameStrings, llvm::jitlink::Block* block) {
    for (auto& edge : block->edges()) {
        if (auto& targetSym = edge.getTarget(); targetSym.isDefined()) {
            auto targetAddrVal = targetSym.getAddress().getValue();
            if (auto it = methnameStrings.find(targetAddrVal); it != methnameStrings.end()) {
                return it->second;
            }
        }
    }
    return "";
}

static std::string tryToFindSelectoViaFixedupContent(const MethnameMap& methnameStrings, const llvm::ArrayRef<char>& blockContent) {

    void* selectorStringAddr = nullptr;
    memcpy(&selectorStringAddr, blockContent.data(), sizeof(void*));

    if (selectorStringAddr != nullptr) {
        // Look up in our methname map
        const auto targetAddrVal = reinterpret_cast<uint64_t>(selectorStringAddr);
        if (const auto it = methnameStrings.find(targetAddrVal); it != methnameStrings.end()) {
            return it->second;
        }

        // Last resort, try to read directly from the pointer
        auto selectorCStr = static_cast<const char*>(selectorStringAddr);
        const auto len = strnlen(selectorCStr, 256);
        if (len > 0 && len < 256) {
            return {selectorCStr, len};
        }
    }

    return "";
}

void ObjCSelectorFixupPlugin::modifyPassConfig(
    llvm::orc::MaterializationResponsibility& MR,
    llvm::jitlink::LinkGraph& G,
    llvm::jitlink::PassConfiguration& Config
) {

    // Add a post-fixup pass that runs after all relocations are applied.
    // At this point, we can find and fix up ObjC selector references.
    Config.PostFixupPasses.emplace_back([](llvm::jitlink::LinkGraph& Graph) {

        auto selrefsSection = findObjCSelectorSection(Graph);

        // No selector references, nothing to do
        if (!selrefsSection) {
            return llvm::Error::success();
        }

        int fixedCount = 0;
        const auto methnameStrings = buildMethnameStringMap(Graph);

        for (auto* block : selrefsSection->blocks()) {
            auto blockContent = block->getContent();

            if (blockContent.size() < sizeof(void*)) {
                continue;
            }

            // Approach 1: Try to find selector via graph edges (more reliable)
            std::string selectorName = tryToFindSelectoViaGraphEdges(methnameStrings, block);
            // Approach 2: Fall back to reading fixedup content
            if (selectorName.empty()) {
                selectorName = tryToFindSelectoViaFixedupContent(methnameStrings, blockContent);
            }

            if (selectorName.empty()) {
                HRLogDebug("ObjCSelectorFixupPlugin: Could not determine selector for block @ 0x%llx", block->getAddress().getValue());
                continue;
            }

            // Register patched Objective-C selector to its runtime by
            // updating the selector reference to use the registered selector.
            SEL registeredSel = sel_registerName(selectorName.c_str());
            auto mutableContent = block->getMutableContent(Graph);
            memcpy(mutableContent.data(), &registeredSel, sizeof(void*));

            fixedCount++;
        }

        HRLogDebug("ObjCSelectorFixupPlugin: Fixed %d selector references", fixedCount);

        return llvm::Error::success();
    });
}

}