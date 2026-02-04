/**
 * Copyright 2010-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 *
 * JIT plugin implementations for the HotReload system.
 */

#ifdef KONAN_HOT_RELOAD

#include "HotReloadPlugins.hpp"
#include "HotReloadInternal.hpp"

#include <cstring>

namespace kotlin::hot {

#if defined(__APPLE__)

void ObjCSelectorFixupPlugin::modifyPassConfig(
        llvm::orc::MaterializationResponsibility& MR,
        llvm::jitlink::LinkGraph& G,
        llvm::jitlink::PassConfiguration& Config) {
    // Log all sections in the graph for debugging
    HRLogDebug("ObjCSelectorFixupPlugin: Analyzing graph '%s'", G.getName().c_str());
    for (auto& section : G.sections()) {
        HRLogDebug("  Section: %s", std::string(section.getName()).c_str());
    }

    // Add a post-fixup pass that runs after all relocations are applied.
    // At this point, we can find and fix up ObjC selector references.
    Config.PostFixupPasses.push_back([](llvm::jitlink::LinkGraph& Graph) {
        // Find __objc_selrefs section - this contains pointers to selector strings.
        // JITLink uses segment,section naming convention: "__DATA,__objc_selrefs"
        llvm::jitlink::Section* selrefsSection = nullptr;

        for (auto& section : Graph.sections()) {
            auto sectionName = section.getName();
            // Check for both naming conventions:
            // - JITLink format: "__DATA,__objc_selrefs"
            // - Simple format: "__objc_selrefs"
            if (sectionName == "__DATA,__objc_selrefs" || sectionName == "__objc_selrefs" ||
                sectionName.ends_with(",__objc_selrefs")) {
                selrefsSection = &section;
                HRLogDebug("ObjCSelectorFixupPlugin: Found selector section: %s", std::string(sectionName).c_str());
                break;
            }
        }

        if (!selrefsSection) {
            // No selector references, nothing to do
            HRLogDebug("ObjCSelectorFixupPlugin: No __objc_selrefs section found");
            return llvm::Error::success();
        }

        HRLogDebug("ObjCSelectorFixupPlugin: Processing selector references...");

        int fixedCount = 0;

        // First, build a map from methname block addresses to their string content.
        // __objc_methname section contains the actual selector strings.
        // Use uint64_t as key since ExecutorAddr doesn't have std::hash.
        std::unordered_map<uint64_t, std::string> methnameStrings;

        HRLogDebug("ObjCSelectorFixupPlugin: Building methname map...");

        for (auto& section : Graph.sections()) {
            auto sectionName = section.getName();
            if (sectionName == "__TEXT,__objc_methname" || sectionName == "__objc_methname" ||
                sectionName.ends_with(",__objc_methname")) {
                for (auto* block : section.blocks()) {
                    auto content = block->getContent();
                    if (!content.empty()) {
                        // The content is the null-terminated string
                        std::string str(content.data(), strnlen(content.data(), content.size()));
                        methnameStrings[block->getAddress().getValue()] = str;
                        HRLogDebug("  methname @ 0x%llx: '%s'", block->getAddress().getValue(), str.c_str());
                    }
                }
            }
        }

        // Count total blocks for debugging
        int totalBlocks = 0;
        for (auto* block : selrefsSection->blocks()) {
            (void)block;
            totalBlocks++;
        }
        HRLogDebug("ObjCSelectorFixupPlugin: Found %d blocks in __objc_selrefs", totalBlocks);

        for (auto* block : selrefsSection->blocks()) {
            auto blockContent = block->getContent();

            HRLogDebug(
                    "ObjCSelectorFixupPlugin: Processing selref block @ 0x%llx, size=%zu, edges=%zu",
                    block->getAddress().getValue(), blockContent.size(), block->edges_size());

            if (blockContent.size() < sizeof(void*)) {
                HRLogDebug("  Block too small, skipping");
                continue;
            }

            // Approach 1: Try to find selector via graph edges (more reliable)
            std::string selectorName;
            HRLogDebug("  Approach 1: Checking edges...");
            for (auto& edge : block->edges()) {
                auto& targetSym = edge.getTarget();
                HRLogDebug("    Edge -> target isDefined=%d, hasName=%d", targetSym.isDefined(), targetSym.hasName());
                if (targetSym.isDefined()) {
                    auto targetAddrVal = targetSym.getAddress().getValue();
                    HRLogDebug("    Edge target addr: 0x%llx", targetAddrVal);
                    if (auto it = methnameStrings.find(targetAddrVal); it != methnameStrings.end()) {
                        selectorName = it->second;
                        HRLogDebug("    Found selector via edge: '%s'", selectorName.c_str());
                        break;
                    } else {
                        HRLogDebug("    Target addr not in methname map");
                    }
                }
            }

            // Approach 2: Fall back to reading fixedup content
            if (selectorName.empty()) {
                HRLogDebug("  Approach 2: Reading fixedup content...");
                void* selectorStringAddr = nullptr;
                memcpy(&selectorStringAddr, blockContent.data(), sizeof(void*));
                HRLogDebug("    Content points to: %p", selectorStringAddr);
                if (selectorStringAddr != nullptr) {
                    // Look up in our methname map
                    auto targetAddrVal = reinterpret_cast<uint64_t>(selectorStringAddr);
                    if (auto it = methnameStrings.find(targetAddrVal); it != methnameStrings.end()) {
                        selectorName = it->second;
                        HRLogDebug("    Found in methname map: '%s'", selectorName.c_str());
                    } else {
                        HRLogDebug("    Not in methname map, trying direct read...");
                        // Last resort: try to read directly from the pointer
                        const char* selectorCStr = static_cast<const char*>(selectorStringAddr);
                        size_t len = strnlen(selectorCStr, 256);
                        if (len > 0 && len < 256) {
                            selectorName = std::string(selectorCStr, len);
                            HRLogDebug("    Direct read succeeded: '%s'", selectorName.c_str());
                        } else {
                            HRLogDebug("    Direct read failed (len=%zu)", len);
                        }
                    }
                }
            }

            if (selectorName.empty()) {
                HRLogDebug(
                        "ObjCSelectorFixupPlugin: Could not determine selector for block @ 0x%llx",
                        block->getAddress().getValue());
                continue;
            }

            HRLogDebug(
                    "ObjCSelectorFixupPlugin: Successfully resolved selector '%s' for block @ 0x%llx",
                    selectorName.c_str(), block->getAddress().getValue());

            // Register the selector with the ObjC runtime
            SEL registeredSel = sel_registerName(selectorName.c_str());

            HRLogDebug(
                    "ObjCSelectorFixupPlugin: '%s' -> registered %p (selref @ 0x%llx)",
                    selectorName.c_str(), (void*)registeredSel, block->getAddress().getValue());

            // Update the selector reference to use the registered selector.
            auto mutableContent = block->getMutableContent(Graph);
            memcpy(mutableContent.data(), &registeredSel, sizeof(void*));

            fixedCount++;
        }

        HRLogDebug("ObjCSelectorFixupPlugin: Fixed %d selector references", fixedCount);

        return llvm::Error::success();
    });
}

#endif // __APPLE__

void LatestObjectListener::modifyPassConfig(
        llvm::orc::MaterializationResponsibility& MR,
        llvm::jitlink::LinkGraph& G,
        llvm::jitlink::PassConfiguration& Config) {
    HRLogDebug("modifyPassConfig called for graph: %s", G.getName().c_str());

    // In `PostAllocationPhase`, memory has been allocated and the object file is loaded into memory.
    // We use this to collect function and class addresses from the linked object.
    Config.PostAllocationPasses.push_back([this](llvm::jitlink::LinkGraph& Graph) {
        HRLogDebug("PostAllocationPass executing for: %s", Graph.getName().c_str());
        KotlinObjectFile kotlinObjectFile;

        for (const auto definedSymbol : Graph.defined_symbols()) {
            if (!definedSymbol->hasName()) continue;

            auto symbolNamePtr = definedSymbol->getName();
            auto symbolName = *symbolNamePtr;

            const auto symbolAddress = definedSymbol->getAddress();

            if (symbolName.starts_with(MANGLED_FUN_NAME_PREFIX)) {
                kotlinObjectFile.functions[symbolName.str()] = symbolAddress;
            } else if (symbolName.starts_with(MANGLED_CLASS_NAME_PREFIX)) {
                kotlinObjectFile.classes[symbolName.str()] = symbolAddress;
            }
        }

        _objManager.RegisterKotlinObjectFile(std::move(kotlinObjectFile));
        HRLogDebug(
                "Registered %zu functions and %zu classes from object file",
                kotlinObjectFile.functions.size(), kotlinObjectFile.classes.size());

        return llvm::Error::success();
    });
}

llvm::Error StubDefinitionGenerator::tryToGenerate(
        llvm::orc::LookupState& LS,
        llvm::orc::LookupKind K,
        llvm::orc::JITDylib& JD,
        llvm::orc::JITDylibLookupFlags JDLookupFlags,
        const llvm::orc::SymbolLookupSet& Symbols) {
    llvm::orc::SymbolMap NewSymbols;

    for (const auto& [Name, Flags] : Symbols) {
        auto NameStr = (*Name).str();
        // Only handle kfun: symbols (stable function names)
        if (NameStr.find(MANGLED_FUN_NAME_PREFIX) == std::string::npos) continue;
        // Skip if this is already an impl symbol
        if (NameStr.size() > IMPL_SUFFIX.size() &&
            NameStr.substr(NameStr.size() - IMPL_SUFFIX.size()) == IMPL_SUFFIX) continue;

        // Look for pre-created stub
        const auto StubSym = _ISM.findStub(*Name, true);
        if (StubSym.getAddress()) {
            HRLogDebug("StubDefinitionGenerator: resolved %s -> stub @ 0x%llx", NameStr.c_str(), StubSym.getAddress().getValue());
            NewSymbols[Name] = StubSym;
        } else {
            HRLogDebug("StubDefinitionGenerator: no stub found for %s", NameStr.c_str());
        }
    }
    if (!NewSymbols.empty()) {
        return JD.define(llvm::orc::absoluteSymbols(std::move(NewSymbols)));
    }
    return llvm::Error::success();
}

llvm::Error WeakSymbolFallbackGenerator::tryToGenerate(
        llvm::orc::LookupState& LS,
        llvm::orc::LookupKind K,
        llvm::orc::JITDylib& JD,
        llvm::orc::JITDylibLookupFlags JDLookupFlags,
        const llvm::orc::SymbolLookupSet& Symbols) {
    llvm::orc::SymbolMap NewSymbols;

    for (const auto& [Name, Flags] : Symbols) {
        auto NameStr = (*Name).str();

        // Debug: log kclass symbols that reach the fallback generator
        if (NameStr.find("kclass:") != std::string::npos) {
            fprintf(stderr, "[DEBUG] WeakSymbolFallbackGenerator: kclass symbol not resolved: %s\n", NameStr.c_str());
            fflush(stderr);
        }

        // Skip kfun: symbols - they should be handled by StubDefinitionGenerator
        if (NameStr.find("kfun:") != std::string::npos) continue;

        // Skip C++ RTTI symbols - they are critical for exception handling
        // and must be properly exported from stdlib-cache.a
        if (NameStr.find("__ZTI") == 0 || NameStr.find("__ZTS") == 0) {
            HRLogDebug("WeakSymbolFallbackGenerator: NOT providing fallback for RTTI symbol %s", NameStr.c_str());
            continue;
        }

        // Check if this is a symbol we can provide a fallback for
        bool shouldProvideWeakDef =
                // ObjC notification constants
                NameStr.find("_NSAccessibility") == 0 ||
                // Mach kernel traps
                NameStr.find("_mach_vm_") == 0;

        if (shouldProvideWeakDef) {
            HRLogDebug("WeakSymbolFallbackGenerator: providing null definition for %s", NameStr.c_str());
            // Provide a null address - if called, will crash with null pointer
            // The Weak flag tells the linker this can be overridden
            NewSymbols[Name] = {llvm::orc::ExecutorAddr(), llvm::JITSymbolFlags::Exported | llvm::JITSymbolFlags::Weak};
        }
    }

    if (!NewSymbols.empty()) {
        return JD.define(llvm::orc::absoluteSymbols(std::move(NewSymbols)));
    }
    return llvm::Error::success();
}

} // namespace kotlin::hot

#endif // KONAN_HOT_RELOAD
