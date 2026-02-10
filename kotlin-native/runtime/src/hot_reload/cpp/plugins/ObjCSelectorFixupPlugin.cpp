//
// Created by Gabriele.Pappalardo on 09/02/2026.
//

#include "ObjCSelectorFixupPlugin.hpp"
#include "HotReloadInternal.hpp"

#if defined(__APPLE__)
#include <objc/runtime.h>
#endif

namespace kotlin::hot::orc::plugins {

void ObjCSelectorFixupPlugin::modifyPassConfig(
    llvm::orc::MaterializationResponsibility& MR,
    llvm::jitlink::LinkGraph& G,
    llvm::jitlink::PassConfiguration& Config
) {

    // Add a post-fixup pass that runs after all relocations are applied.
    // At this point, we can find and fix up ObjC selector references.
    Config.PostFixupPasses.emplace_back([](llvm::jitlink::LinkGraph& Graph) {
        // Find __objc_selrefs section - this contains pointers to selector strings.
        // JITLink uses segment,section naming convention: "__DATA,__objc_selrefs"
        llvm::jitlink::Section* selrefsSection = nullptr;

        for (auto& section : Graph.sections()) {
            auto sectionName = section.getName();
            // Check for both naming conventions:
            // - JITLink format: "__DATA,__objc_selrefs"
            // - Simple format: "__objc_selrefs"
            if (sectionName == "__DATA,__objc_selrefs" || sectionName == "__objc_selrefs" || sectionName.ends_with(",__objc_selrefs")) {
                selrefsSection = &section;
                // HRLogDebug("ObjCSelectorFixupPlugin: Found selector section: %s", std::string(sectionName).c_str());
                break;
            }
        }

        if (!selrefsSection) {
            // No selector references, nothing to do
            // HRLogDebug("ObjCSelectorFixupPlugin: No __objc_selrefs section found");
            return llvm::Error::success();
        }

        // HRLogDebug("ObjCSelectorFixupPlugin: Processing selector references...");

        int fixedCount = 0;

        // First, build a map from methname block addresses to their string content.
        // __objc_methname section contains the actual selector strings.
        // Use uint64_t as key since ExecutorAddr doesn't have std::hash.
        std::unordered_map<uint64_t, std::string> methnameStrings;

        // HRLogDebug("ObjCSelectorFixupPlugin: Building methname map...");

        for (auto& section : Graph.sections()) {
            auto sectionName = section.getName();
            if (sectionName == "__TEXT,__objc_methname" || sectionName == "__objc_methname" || sectionName.ends_with(",__objc_methname")) {
                for (auto* block : section.blocks()) {
                    auto content = block->getContent();
                    if (!content.empty()) {
                        // The content is the null-terminated string
                        std::string str(content.data(), strnlen(content.data(), content.size()));
                        methnameStrings[block->getAddress().getValue()] = str;
                        // HRLogDebug("  methname @ 0x%llx: '%s'", block->getAddress().getValue(), str.c_str());
                    }
                }
            }
        }

        // Count total blocks for debugging
        // int totalBlocks = 0;
        // for (auto* block : selrefsSection->blocks()) {
        //     (void)block;
        //     // totalBlocks++;
        // }
        // HRLogDebug("ObjCSelectorFixupPlugin: Found %d blocks in __objc_selrefs", totalBlocks);

        for (auto* block : selrefsSection->blocks()) {
            auto blockContent = block->getContent();

            // HRLogDebug(
            //         "ObjCSelectorFixupPlugin: Processing selref block @ 0x%llx, size=%zu, edges=%zu", block->getAddress().getValue(),
            //         blockContent.size(), block->edges_size());

            if (blockContent.size() < sizeof(void*)) {
                // HRLogDebug("  Block too small, skipping");
                continue;
            }

            // Approach 1: Try to find selector via graph edges (more reliable)
            std::string selectorName;
            // HRLogDebug("  Approach 1: Checking edges...");
            for (auto& edge : block->edges()) {
                auto& targetSym = edge.getTarget();
                // HRLogDebug("    Edge -> target isDefined=%d, hasName=%d", targetSym.isDefined(), targetSym.hasName());
                if (targetSym.isDefined()) {
                    auto targetAddrVal = targetSym.getAddress().getValue();
                    // HRLogDebug("    Edge target addr: 0x%llx", targetAddrVal);
                    if (auto it = methnameStrings.find(targetAddrVal); it != methnameStrings.end()) {
                        selectorName = it->second;
                        // HRLogDebug("    Found selector via edge: '%s'", selectorName.c_str());
                        break;
                    } else {
                        // HRLogDebug("    Target addr not in methname map");
                    }
                }
            }

            // Approach 2: Fall back to reading fixedup content
            if (selectorName.empty()) {
                // HRLogDebug("  Approach 2: Reading fixedup content...");
                void* selectorStringAddr = nullptr;
                memcpy(&selectorStringAddr, blockContent.data(), sizeof(void*));
                // HRLogDebug("    Content points to: %p", selectorStringAddr);
                if (selectorStringAddr != nullptr) {
                    // Look up in our methname map
                    auto targetAddrVal = reinterpret_cast<uint64_t>(selectorStringAddr);
                    if (auto it = methnameStrings.find(targetAddrVal); it != methnameStrings.end()) {
                        selectorName = it->second;
                        // HRLogDebug("    Found in methname map: '%s'", selectorName.c_str());
                    } else {
                        // HRLogDebug("    Not in methname map, trying direct read...");
                        // Last resort: try to read directly from the pointer
                        const char* selectorCStr = static_cast<const char*>(selectorStringAddr);
                        size_t len = strnlen(selectorCStr, 256);
                        if (len > 0 && len < 256) {
                            selectorName = std::string(selectorCStr, len);
                            // HRLogDebug("    Direct read succeeded: '%s'", selectorName.c_str());
                        } else {
                            // HRLogDebug("    Direct read failed (len=%zu)", len);
                        }
                    }
                }
            }

            if (selectorName.empty()) {
                //HRLogDebug("ObjCSelectorFixupPlugin: Could not determine selector for block @ 0x%llx", block->getAddress().getValue());
                continue;
            }

            // HRLogDebug(
            //         "ObjCSelectorFixupPlugin: Successfully resolved selector '%s' for block @ 0x%llx", selectorName.c_str(),
            //         block->getAddress().getValue());

            // Register the selector with the ObjC runtime
            SEL registeredSel = sel_registerName(selectorName.c_str());

            // HRLogDebug(
            //         "ObjCSelectorFixupPlugin: '%s' -> registered %p (selref @ 0x%llx)", selectorName.c_str(), (void*)registeredSel,
            //         block->getAddress().getValue());

            // Update the selector reference to use the registered selector.
            auto mutableContent = block->getMutableContent(Graph);
            memcpy(mutableContent.data(), &registeredSel, sizeof(void*));

            fixedCount++;
        }

        HRLogDebug("ObjCSelectorFixupPlugin: Fixed %d selector references", fixedCount);

        return llvm::Error::success();
    });
}

}