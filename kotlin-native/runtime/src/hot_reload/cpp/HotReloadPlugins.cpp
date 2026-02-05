/**
 * Copyright 2010-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifdef KONAN_HOT_RELOAD

#include "HotReloadPlugins.hpp"
#include "HotReloadInternal.hpp"

#include <cstring>

namespace kotlin::hot::orc::plugins {

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

            HRLogDebug("ObjCSelectorFixupPlugin: '%s' -> registered %p (selref @ 0x%llx)",
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

static constexpr auto READ_WRITE_MEMPROT = static_cast<llvm::orc::MemProt>(
    static_cast<uint8_t>(llvm::orc::MemProt::Read) | static_cast<uint8_t>(llvm::orc::MemProt::Write)
);

static std::vector<llvm::jitlink::Symbol*> CollectKotlinFunctionSymbols(llvm::jitlink::LinkGraph& G) {
    std::vector<llvm::jitlink::Symbol*> symbolsToProcess{};

    for (auto* DefinedSymbol : G.defined_symbols()) {
        if (!DefinedSymbol->hasName()) continue;
        if (!DefinedSymbol->isCallable()) continue;
        if (DefinedSymbol->getScope() != llvm::jitlink::Scope::Default) continue;

        const auto SymbolName = *DefinedSymbol->getName();
        if (!SymbolName.starts_with(MANGLED_KOTLIN_FUN_NAME_PREFIX))
            continue;

        symbolsToProcess.push_back(DefinedSymbol);
    }

    return symbolsToProcess;
}

static void RenameSymbolAndSetLocalScope(llvm::orc::ExecutionSession& ES, llvm::jitlink::Symbol* Symbol, const std::string& NewName) {
    Symbol->setName(ES.intern(NewName));
    Symbol->setScope(llvm::jitlink::Scope::Local);
}

void KotlinObjectOverrider::modifyPassConfig(
    llvm::orc::MaterializationResponsibility& MR,
    llvm::jitlink::LinkGraph& G,
    llvm::jitlink::PassConfiguration& Config
) {
    // On first definition:
    // * rename function, set scope to local,
    // * introduce stub with original name, stub points to the original body

    // On subsequent definition:
    // * Rename function, set scope to local
    // * Update pointer to point at new body

    // Use PostPrunePasses so new blocks get addresses during allocation
    Config.PostPrunePasses.push_back([this, &MR](llvm::jitlink::LinkGraph& Graph) -> llvm::Error {

        // Collect symbols first to avoid iterator invalidation
        auto& ES = MR.getExecutionSession();
        const auto SymbolsToProcess = CollectKotlinFunctionSymbols(Graph);
        if (SymbolsToProcess.empty()) return llvm::Error::success();

        // Create sections for stubs
        auto& StubsDataSection = Graph.createSection("__DATA,__knhr_stubs", READ_WRITE_MEMPROT);

        // Collect new symbols we'll be defining so we can inform ORC
        llvm::orc::SymbolFlagsMap NewSymbols;

        for (auto* DefinedSymbol : SymbolsToProcess) {
            const auto SymbolName = *DefinedSymbol->getName();
            const auto InternedSymName = ES.intern(SymbolName);

            HRLogDebug("Processing Symbol: %s", SymbolName.str().c_str());

            if (_trackedSymbols.contains(InternedSymName)) {
                // The function was already defined, rename it and mark for pointer update.
                // The pointer update will happen in PostAllocationPasses when addresses are known.
                auto& CurrentInfo = _trackedSymbols[InternedSymName];
                auto NewSymbolName = (SymbolName + "$body_" + std::to_string(++CurrentInfo.redefinitionCount)).str();
                RenameSymbolAndSetLocalScope(ES, DefinedSymbol, NewSymbolName);
            } else {
                auto& TextSection = DefinedSymbol->getSection();

                // Rename the original function
                auto RenamedSymbolName = (SymbolName + "$body_1").str();
                RenameSymbolAndSetLocalScope(ES, DefinedSymbol, RenamedSymbolName);

                // Create the pointer block that will hold the function address
                const char DataPtr[8] = {0, 0, 0, 0, 0, 0, 0, 0};
                auto& PtrBlock = Graph.createContentBlock(
                    StubsDataSection,
                    llvm::ArrayRef(DataPtr, 8),
                    llvm::orc::ExecutorAddr(),
                    8, 0);
                PtrBlock.addEdge(llvm::jitlink::aarch64::Pointer64, 0, *DefinedSymbol, 0);

                auto PtrSymbolName = (SymbolName + "$stub_ptr").str();
                auto& Ptr = Graph.addDefinedSymbol(
                    PtrBlock, 0, PtrSymbolName, 8,
                    llvm::jitlink::Linkage::Strong,
                    llvm::jitlink::Scope::Default,
                    false, true);

                // Track the new $stub_ptr symbol for ORC
                auto InternedPtrSymName = ES.intern(PtrSymbolName);
                NewSymbols[InternedPtrSymName] = llvm::JITSymbolFlags::Exported;

                // Store ptr symbol reference for address resolution in PostAllocationPasses
                _pendingResolution.emplace_back(InternedSymName, &Ptr);

                // Initialize tracking (address will be resolved in PostAllocationPasses)
                _trackedSymbols[InternedSymName] = DefinitionInfo{};

                // Create the stub trampoline with the original function name
                static const uint8_t StubCode[] = {
                    0x08, 0x00, 0x00, 0x90,   // adrp x8, ptr@PAGE
                    0x00, 0x01, 0x40, 0xF9,   // ldr x0, [x8, ptr@PAGEOFF]
                    0x00, 0x00, 0x1F, 0xD6,   // br x0
                };
                constexpr auto StubCodeSize = sizeof(StubCode);
                auto StubContent = Graph.allocateBuffer(StubCodeSize);
                memcpy(StubContent.data(), StubCode, StubCodeSize);

                auto& StubBlock = Graph.createContentBlock(
                    TextSection, StubContent,
                    llvm::orc::ExecutorAddr(),
                    4, 0);

                // Add relocations for the ADRP/LDR pair
                StubBlock.addEdge(llvm::jitlink::aarch64::Page21, 0, Ptr, 0);
                StubBlock.addEdge(llvm::jitlink::aarch64::PageOffset12, 4, Ptr, 0);

                Graph.addDefinedSymbol(
                    StubBlock, 0, SymbolName, StubCodeSize,
                    llvm::jitlink::Linkage::Strong,
                    llvm::jitlink::Scope::Default,
                    true, true);
            }
        }

        // Inform ORC about the new symbols we're defining
        if (!NewSymbols.empty()) {
            if (auto Err = MR.defineMaterializing(std::move(NewSymbols))) {
                return Err;
            }
        }

        return llvm::Error::success();
    });

    // Resolve addresses and update pointers after allocation
    Config.PostAllocationPasses.push_back([this, &MR](llvm::jitlink::LinkGraph& Graph) {
        auto& ES = MR.getExecutionSession();

        // First, resolve addresses for newly created ptr symbols from this materialization
        for (const auto& [symName, ptrSymbol] : _pendingResolution) {
            auto ptrAddr = ptrSymbol->getAddress();
            _trackedSymbols[symName].ptrAddress = ptrAddr;
            HRLogDebug("Resolved ptr address for %s: 0x%llx", (*symName).str().c_str(), ptrAddr.getValue());
        }

        _pendingResolution.clear();

        // Then, update pointers for redefinitions
        for (const auto* DefinedSymbol : Graph.defined_symbols()) {
            if (!DefinedSymbol->hasName()) continue;

            const auto SymbolName = *DefinedSymbol->getName();

            // Check if this is a renamed body (e.g., foo$body_2, foo$body_3, ...)
            auto bodyPos = SymbolName.find("$body_");
            if (bodyPos == llvm::StringRef::npos) continue;

            // Extract the original function name
            auto OriginalName = SymbolName.substr(0, bodyPos);
            auto InternedOrigName = ES.intern(OriginalName);

            auto it = _trackedSymbols.find(InternedOrigName);
            if (it == _trackedSymbols.end()) continue;

            auto& DefInfo = it->second;

            // Skip first definitions (body_1) - they're handled by relocation
            if (DefInfo.redefinitionCount <= 1) continue;

            // For redefinitions, update the pointer to the new implementation
            if (DefInfo.ptrAddress.getValue() != 0) {
                HRLogDebug("Updating pointer for %s to new body at 0x%llx", OriginalName.str().c_str(), DefinedSymbol->getAddress().getValue());

                auto* ptrLocation = DefInfo.ptrAddress.toPtr<uint64_t*>();
                *ptrLocation = DefinedSymbol->getAddress().getValue();
            } else {
                HRLogDebug("Warning: ptrAddress not yet resolved for %s", OriginalName.str().c_str());
            }
        }

        return llvm::Error::success();
    });
}

llvm::Error KotlinObjectOverrider::notifyEmitted(llvm::orc::MaterializationResponsibility& MR) {
    // Address resolution now happens in PostAllocationPasses, nothing to do here
    return llvm::Error::success();
}

void KotlinObjectListener::modifyPassConfig(
        llvm::orc::MaterializationResponsibility& MR,
        llvm::jitlink::LinkGraph& G,
        llvm::jitlink::PassConfiguration& Config
) {
    Config.PostAllocationPasses.push_back([this](llvm::jitlink::LinkGraph& Graph) {
        KotlinObjectFile kotlinObjectFile;

        for (const auto definedSymbol : Graph.defined_symbols()) {
            if (!definedSymbol->hasName()) continue;

            auto symbolNamePtr = definedSymbol->getName();
            auto symbolName = *symbolNamePtr;

            const auto symbolAddress = definedSymbol->getAddress();

            if (symbolName.starts_with(MANGLED_KOTLIN_CLASS_PLATFORM_NAME)) continue;
            if (symbolName.starts_with(MANGLED_KOTLIN_FUN_PLATFORM_NAME)) continue;

            if (!symbolName.starts_with(MANGLED_KOTLIN_FUN_NAME_PREFIX) || symbolName.starts_with(MANGLED_KOTLIN_CLASS_NAME_PREFIX))
                continue;

            HRLogDebug("Found symbol %s at 0x%llx", symbolName.str().c_str(), symbolAddress.getValue());

            if (symbolName.starts_with(MANGLED_KOTLIN_FUN_NAME_PREFIX)) {
                kotlinObjectFile.functions[symbolName.str()] = symbolAddress;
            } else if (symbolName.starts_with(MANGLED_KOTLIN_CLASS_NAME_PREFIX)) {
                kotlinObjectFile.classes[symbolName.str()] = symbolAddress;
            }
        }

        if (kotlinObjectFile.functions.empty() && kotlinObjectFile.classes.empty()) {
            HRLogDebug("No Kotlin functions or classes found in emitted object file");
            return llvm::Error::success();
        }

        _objManager.RegisterKotlinObjectFile(std::move(kotlinObjectFile));
        HRLogDebug("Registered %zu functions and %zu classes from object file", kotlinObjectFile.functions.size(), kotlinObjectFile.classes.size());

        return llvm::Error::success();
    });
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

        // Skip kfun: symbols - they should be resolved from the loaded object files
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

} // namespace kotlin::hot::orc::plugins

#endif // KONAN_HOT_RELOAD
