#ifdef KONAN_HOT_RELOAD

#include <unordered_set>
#include <queue>
#include <iomanip>
#include <chrono>
#include <fstream>
#include <utility>
#include <iostream>

#include "Memory.h"
#include "Natives.h"
#include "GlobalData.hpp"
#include "ReferenceOps.hpp"
#include "ThreadRegistry.hpp"
#include "ShadowStack.hpp"
#include "ThreadData.hpp"
#include "RootSet.hpp"
#include "ObjectTraversal.hpp"

#include "HotReload.hpp"
#include "HotReloadInternal.hpp"

#include "llvm/Object/ObjectFile.h"

#include "KString.h"
#include "HotReloadUtility.hpp"

#define MANGLED_FUN_NAME_PREFIX "_kfun:"
#define MANGLED_CLASS_NAME_PREFIX "_kclass:"
#define HOT_RELOAD_IMPL_SUFFIX "$hr_impl"

using namespace kotlin;

using kotlin::hot::HotReloadImpl;
using kotlin::hot::KotlinObjectFile;
using kotlin::hot::ObjectManager;

/// Forward declarations for GC functions.
namespace kotlin::gc {
void stopTheWorld(GCHandle gcHandle, const char* reason) noexcept;
void resumeTheWorld(GCHandle gcHandle) noexcept;
} // namespace kotlin::gc

ManuallyScoped<HotReloadImpl> globalDataInstance{};

llvm::ExitOnError ExitOnErr;

/// The HotReloader module will ignore all the classes
const std::unordered_set<std::string_view> NON_RELOADABLE_CLASS_SYMBOLS = {"kclass:kotlin.Annotation"};

template <typename F>
void traverseObjectFieldsInternal(ObjHeader* object, F process) noexcept(noexcept(process(std::declval<mm::RefFieldAccessor>()))) {
    const TypeInfo* typeInfo = object->type_info();
    // Only consider arrays of objects, not arrays of primitives.
    if (typeInfo != theArrayTypeInfo) {
        for (int index = 0; index < typeInfo->objOffsetsCount_; index++) {
            auto fieldPtr = reinterpret_cast<ObjHeader**>(reinterpret_cast<uintptr_t>(object) + typeInfo->objOffsets_[index]);
            process(mm::RefFieldAccessor(fieldPtr));
        }
    } else {
        ArrayHeader* array = object->array();
        for (uint32_t index = 0; index < array->count_; index++) {
            process(mm::RefFieldAccessor(ArrayAddressOfElementAt(array, index)));
        }
    }
}

template <typename F>
void visitObjectGraph(ObjHeader* startObject, F processingFunction) {
    // We need to perform a BFS, while ensuring that the world is stopped.
    // Let's collect the root set, and start the graph exploration.
    // At the moment, let's make things simple, and single-threaded (otherwise, well, headaches).
    std::queue<ObjHeader*> objectsToVisit{};
    std::unordered_set<ObjHeader*> visitedObjects{};

    auto processObject = [&](ObjHeader* obj, utility::ReferenceOrigin origin) {
        // const char* originString = originToString(origin);
        if (obj == nullptr || isNullOrMarker(obj)) return;

        // j HRLogDebug("processing object of type %s from %s", obj->type_info()->fqName().c_str(), originString);
        if (const auto visited = visitedObjects.find(obj); visited != visitedObjects.end()) return;

        visitedObjects.insert(obj);
        objectsToVisit.push(obj);
    };

    // Let's start collecting the root set
    for (auto& thread : mm::ThreadRegistry::Instance().LockForIter()) {
        auto& shadowStack = thread.shadowStack();
        for (const auto& object : shadowStack) {
            processObject(object, utility::ReferenceOrigin::ShadowStack);
        }
    }

    for (const auto& objRef : mm::GlobalData::Instance().globalsRegistry().LockForIter()) {
        if (objRef != nullptr) {
            processObject(*objRef, utility::ReferenceOrigin::Global);
        }
    }

    processObject(startObject, utility::ReferenceOrigin::ObjRef);

    HRLogDebug("Starting object graph visit with %ld nodes", objectsToVisit.size());

    while (!objectsToVisit.empty()) {
        const auto nextObject = objectsToVisit.front();
        objectsToVisit.pop();
        processingFunction(nextObject, processObject);
    }
}

// region "C" functions

extern "C" {
void Kotlin_native_internal_HotReload_perform(ObjHeader* obj, const ObjHeader* dylibPathStr) {
    AssertThreadState(ThreadState::kRunnable);
    const auto dylibPath = kotlin::to_string<KStringConversionMode::UNCHECKED>(dylibPathStr);
    HotReloadImpl::Instance().Reload(dylibPath);
}
}

// endregion

// region LLVM Plugins
class LatestObjectListener : public llvm::orc::ObjectLinkingLayer::Plugin {
public:
    explicit LatestObjectListener(ObjectManager& objManager) : _objManager(objManager) {}

    void modifyPassConfig(
            llvm::orc::MaterializationResponsibility& MR, llvm::jitlink::LinkGraph& G, llvm::jitlink::PassConfiguration& Config) override {
        // In `PostAllocationPhase`, memory has been allocated and the object file is loaded into memory.
        Config.PostAllocationPasses.push_back([&](llvm::jitlink::LinkGraph& Graph) {
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

            return llvm::Error::success();
        });
    }

    llvm::Error notifyEmitted(llvm::orc::MaterializationResponsibility& MR) override { return llvm::Error::success(); }

    llvm::Error notifyFailed(llvm::orc::MaterializationResponsibility& MR) override { return llvm::Error::success(); }

    llvm::Error notifyRemovingResources(llvm::orc::JITDylib& JD, llvm::orc::ResourceKey K) override { return llvm::Error::success(); }

    void notifyTransferringResources(llvm::orc::JITDylib& JD, llvm::orc::ResourceKey DstKey, llvm::orc::ResourceKey SrcKey) override {}

private:
    ObjectManager& _objManager;
};

/// Definition generator that resolves stable function names to their stub addresses.
/// When JITLink tries to resolve "kfun:foo", this generator returns the pre-created
/// stub address.
/// NOTE: Stubs must be created BEFORE objects are added to the JIT.
class StubDefinitionGenerator : public llvm::orc::DefinitionGenerator {
public:
    explicit StubDefinitionGenerator(llvm::orc::IndirectStubsManager& ISM) : _ISM(ISM) {}

    llvm::Error tryToGenerate(
            llvm::orc::LookupState& LS,
            llvm::orc::LookupKind K,
            llvm::orc::JITDylib& JD,
            llvm::orc::JITDylibLookupFlags JDLookupFlags,
            const llvm::orc::SymbolLookupSet& Symbols) override {

        llvm::orc::SymbolMap NewSymbols;
        const std::string implSuffix = HOT_RELOAD_IMPL_SUFFIX;

        for (const auto& [Name, Flags] : Symbols) {
            auto NameStr = (*Name).str();
            // Only handle kfun: symbols (stable function names)
            if (NameStr.find(MANGLED_FUN_NAME_PREFIX) == std::string::npos) continue;
            // Skip if this is already an impl symbol
            if (NameStr.size() > implSuffix.size() &&
                NameStr.substr(NameStr.size() - implSuffix.size()) == implSuffix) continue;

            // Look for pre-created stub
            const auto StubSym = _ISM.findStub(*Name, true);
            if (StubSym.getAddress()) {
                HRLogDebug("StubDefinitionGenerator: resolved %s -> stub @ 0x%llx",
                           NameStr.c_str(), StubSym.getAddress().getValue());
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

private:
    llvm::orc::IndirectStubsManager& _ISM;
};
// endregion

// region HotReloader

HotReloadImpl::HotReloadImpl() {
    HRLogInfo("Initializing HotReload Module (JIT Engine) and Server (Hot Requests)");
    SetupORC();
    StartServer();
}

void HotReload::InitModule() noexcept {
    globalDataInstance.construct();
}

HotReloadImpl& HotReloadImpl::Instance() noexcept {
    return *globalDataInstance;
}

StatsCollector& HotReloadImpl::GetStatsCollector() noexcept {
    return _statsCollector;
}

void HotReloadImpl::SetupORC() {
    HRLogDebug("Setting up ORC JIT...");

    // Initialize LLVM
    llvm::InitializeNativeTarget();

    // Required for debug symbols in JITted code
    llvm::InitializeNativeTargetAsmPrinter();
    llvm::InitializeNativeTargetAsmParser();

    auto JTMB = ExitOnErr(llvm::orc::JITTargetMachineBuilder::detectHost());

    _JIT = ExitOnErr(
            llvm::orc::LLJITBuilder()
                    .setObjectLinkingLayerCreator([this](llvm::orc::ExecutionSession& ES) {
                        // Install the LatestObjectListener plugin to a custom ObjectLinkingLayer
                        // https://llvm.org/docs/JITLink.html
                        auto OLL = std::make_unique<llvm::orc::ObjectLinkingLayer>(
                                ES, ExitOnErr(llvm::jitlink::InProcessMemoryManager::Create()));
                        // Add an instance of our plugin.
                        OLL->addPlugin(std::make_unique<LatestObjectListener>(_objectManager));

                        // TODO: Add GDB/LLDB debug symbol support for JITted code
                        // JITLink uses plugins instead of JITEventListener

                        return OLL;
                    })
                    .setJITTargetMachineBuilder(std::move(JTMB))
                    .create());

    _LCTM = ExitOnErr(
            llvm::orc::createLocalLazyCallThroughManager(_JIT->getTargetTriple(), _JIT->getExecutionSession(), llvm::orc::ExecutorAddr()));

    _ISM = llvm::orc::createLocalIndirectStubsManagerBuilder(_JIT->getTargetTriple())();

    // Create a generator that looks up symbols in the current process.
    auto& MainJD = _JIT->getMainJITDylib();
    auto DLG = ExitOnErr(llvm::orc::DynamicLibrarySearchGenerator::GetForCurrentProcess(_JIT->getDataLayout().getGlobalPrefix()));
    MainJD.addGenerator(std::move(DLG));

    // Add stub definition generator - resolves stable function names to stub addresses.
    // When JITLink encounters a reference to "kfun:foo", this generator looks up
    // "kfun:foo$hr_impl", creates a stub, and returns the stub address.
    // Stubs are created lazily on first use.
    MainJD.addGenerator(std::make_unique<StubDefinitionGenerator>(*_ISM));

    HRLogDebug("JIT Engine setup completed successfully.");
}

void HotReloadImpl::StartServer() {
    if (_server.start()) {
        _server.run([this](const std::vector<std::string>& objectPaths) {
            HRLogWarning("Note that only a object at time is supported right now");
            HRLogDebug("A new reload request has arrived, containing: ");
            for (auto& obj : objectPaths) HRLogDebug("\t* %s", obj.c_str());
            const auto& objectPath = objectPaths[0];
            Reload(objectPath);
        });
        return;
    }
    HRLogError("Failed to start HotReload server, maybe the TCP port `%d` is already busy?", HotReloadServer::GetDefaultPort());
}

KonanStartFunc HotReloadImpl::LoadBoostrapFile(const char* boostrapFilePath) {
    HRLogDebug("Loading bootstrap file: %s", boostrapFilePath);

    // Read the object file
    auto objBufferOrError = llvm::MemoryBuffer::getFile(boostrapFilePath);
    if (!objBufferOrError) {
        HRLogError("Failed to read bootstrap file: %s", objBufferOrError.getError().message().c_str());
        return nullptr;
    }
    auto objBuffer = std::move(*objBufferOrError);

    // Parse the object file to find all $hr_impl symbols
    std::vector<std::string> implSymbols;
    const std::string implSuffix = HOT_RELOAD_IMPL_SUFFIX;

    auto objFile = llvm::object::ObjectFile::createObjectFile(objBuffer->getMemBufferRef());
    if (!objFile) {
        llvm::consumeError(objFile.takeError());
        HRLogError("Failed to parse bootstrap object file");
        return nullptr;
    }

    auto& ES = _JIT->getExecutionSession();

    // First pass: find all $hr_impl symbols and create PLACEHOLDER stubs
    // The stubs initially point to address 0, but this allows JITLink to
    // resolve the stable names during materialization.
    for (const auto& sym : (*objFile)->symbols()) {
        auto nameOrErr = sym.getName();
        if (!nameOrErr) {
            llvm::consumeError(nameOrErr.takeError());
            continue;
        }
        auto name = nameOrErr->str();

        // Check if this is an $hr_impl symbol (defined function)
        auto flagsOrErr = sym.getFlags();
        if (!flagsOrErr) {
            llvm::consumeError(flagsOrErr.takeError());
            continue;
        }
        auto flags = *flagsOrErr;
        if ((flags & llvm::object::SymbolRef::SF_Undefined) == 0 &&  // Not undefined
            name.find(MANGLED_FUN_NAME_PREFIX) != std::string::npos &&  // Is a kfun: symbol
            name.size() > implSuffix.size() &&
            name.substr(name.size() - implSuffix.size()) == implSuffix) {  // Has $hr_impl suffix
            implSymbols.push_back(name);

            // Create placeholder stub with stable name (remove $hr_impl suffix)
            std::string stableName = name.substr(0, name.size() - implSuffix.size());
            auto stableNameSymbol = ES.intern(stableName);

            // Placeholder address 0 - will be updated after materialization
            if (auto err = _ISM->createStub(*stableNameSymbol, llvm::orc::ExecutorAddr(),
                    llvm::JITSymbolFlags::Callable | llvm::JITSymbolFlags::Exported)) {
                HRLogError("Failed to create placeholder stub for %s: %s",
                           stableName.c_str(), llvm::toString(std::move(err)).c_str());
                continue;
            }
            HRLogDebug("Created placeholder stub for: %s -> %s", stableName.c_str(), name.c_str());
        }
    }

    HRLogDebug("Found %zu impl symbols, created placeholder stubs", implSymbols.size());

    // Add the object file to JIT - stable names will resolve to our placeholder stubs
    ExitOnErr(_JIT->addObjectFile(llvm::MemoryBuffer::getMemBufferCopy(objBuffer->getBuffer(), objBuffer->getBufferIdentifier())));

    HRLogDebug("Bootstrap file added to JIT.");

    // Second pass: look up impl symbols to get real addresses and update stubs
    auto& MainJD = _JIT->getMainJITDylib();

    for (const auto& implName : implSymbols) {
        // Look up the impl symbol
        auto implSymbol = ES.intern(implName);
        auto lookupResult = ES.lookup(
            llvm::orc::makeJITDylibSearchOrder(&MainJD, llvm::orc::JITDylibLookupFlags::MatchAllSymbols),
            implSymbol);

        if (auto Err = lookupResult.takeError()) {
            HRLogError("Failed to look up impl symbol %s: %s",
                       implName.c_str(), llvm::toString(std::move(Err)).c_str());
            continue;
        }

        auto implAddr = lookupResult->getAddress();

        // Update stub with real address
        const std::string stableName = implName.substr(0, implName.size() - implSuffix.size());
        if (auto err = _ISM->updatePointer(stableName, implAddr)) {
            HRLogError("Failed to update stub pointer for %s: %s",
                       stableName.c_str(), llvm::toString(std::move(err)).c_str());
            continue;
        }

        HRLogDebug("Updated stub: %s -> 0x%llx", stableName.c_str(), implAddr.getValue());
    }

    // Now look up Konan_start - all stable function names should be resolvable through stubs
    HRLogDebug("Looking up for `Konan_start` symbol...");
    const auto konanStartSymbol = ExitOnErr(_JIT->lookup("Konan_start"));
    return konanStartSymbol.toPtr<KonanStartFunc>();
}

bool HotReloadImpl::LoadObjectFromPath(const std::string_view objectPath) {
    HRLogDebug("Loading object file for reload: %s", std::string(objectPath).c_str());

    // Read the object file
    auto objBufferOrError = llvm::MemoryBuffer::getFile(objectPath);
    if (!objBufferOrError) {
        HRLogError("Failed to load object file: %s", objBufferOrError.getError().message().c_str());
        return false;
    }
    auto objBuffer = std::move(*objBufferOrError);

    // Parse the object file to find all $hr_impl symbols
    std::vector<std::string> implSymbols;
    const std::string implSuffix = HOT_RELOAD_IMPL_SUFFIX;

    auto objFile = llvm::object::ObjectFile::createObjectFile(objBuffer->getMemBufferRef());
    if (!objFile) {
        llvm::consumeError(objFile.takeError());
        HRLogError("Failed to parse object file");
        return false;
    }

    auto& ES = _JIT->getExecutionSession();

    // Find all $hr_impl symbols and create/update stubs as needed
    for (const auto& sym : (*objFile)->symbols()) {
        auto nameOrErr = sym.getName();
        if (!nameOrErr) {
            llvm::consumeError(nameOrErr.takeError());
            continue;
        }
        auto name = nameOrErr->str();

        // Check if this is an $hr_impl symbol (defined function)
        auto flagsOrErr = sym.getFlags();
        if (!flagsOrErr) {
            llvm::consumeError(flagsOrErr.takeError());
            continue;
        }
        auto flags = *flagsOrErr;
        if ((flags & llvm::object::SymbolRef::SF_Undefined) == 0 &&  // Not undefined
            name.find(MANGLED_FUN_NAME_PREFIX) != std::string::npos &&  // Is a kfun: symbol
            name.size() > implSuffix.size() &&
            name.substr(name.size() - implSuffix.size()) == implSuffix) {  // Has $hr_impl suffix
            implSymbols.push_back(name);

            // Check if stub already exists for this function
            std::string stableName = name.substr(0, name.size() - implSuffix.size());
            auto stableNameSymbol = ES.intern(stableName);
            auto existingStub = _ISM->findStub(*stableNameSymbol, true);

            if (!existingStub.getAddress()) {
                // New function - create placeholder stub
                if (auto err = _ISM->createStub(*stableNameSymbol, llvm::orc::ExecutorAddr(),
                        llvm::JITSymbolFlags::Callable | llvm::JITSymbolFlags::Exported)) {
                    HRLogError("Failed to create placeholder stub for new function %s: %s",
                               stableName.c_str(), llvm::toString(std::move(err)).c_str());
                    continue;
                }
                HRLogDebug("Created placeholder stub for new function: %s", stableName.c_str());
            } else {
                HRLogDebug("Stub already exists for: %s", stableName.c_str());
            }
        }
    }

    HRLogDebug("Found %zu impl symbols in reload object", implSymbols.size());

    // Create a NEW JITDylib for this reload to avoid duplicate symbol errors.
    // Each reload gets its own dylib so symbols can be redefined.
    std::string reloadDylibName = "reload_" + std::to_string(++reloadCounter);
    auto& ReloadJD = ES.createBareJITDylib(reloadDylibName);
    HRLogDebug("Created new JITDylib: %s", reloadDylibName.c_str());

    // The reload dylib needs access to symbols from the main dylib (runtime, etc.)
    auto& MainJD = _JIT->getMainJITDylib();
    ReloadJD.addToLinkOrder(MainJD);

    // Add the object file to the NEW reload JITDylib
    auto err = _JIT->addObjectFile(ReloadJD, llvm::MemoryBuffer::getMemBufferCopy(objBuffer->getBuffer(), objBuffer->getBufferIdentifier()));
    if (err) {
        HRLogError("Failed to add object file to JIT engine: %s", llvm::toString(std::move(err)).c_str());
        return false;
    }

    HRLogDebug("Object file added to JIT (dylib: %s).", reloadDylibName.c_str());

    // Look up impl symbols from the RELOAD dylib (not main) and update stub pointers
    for (const auto& implName : implSymbols) {
        auto implSymbol = ES.intern(implName);
        auto lookupResult = ES.lookup(
            llvm::orc::makeJITDylibSearchOrder(&ReloadJD, llvm::orc::JITDylibLookupFlags::MatchAllSymbols),
            implSymbol);

        if (auto Err = lookupResult.takeError()) {
            HRLogError("Failed to look up impl symbol %s: %s",
                       implName.c_str(), llvm::toString(std::move(Err)).c_str());
            continue;
        }

        auto implAddr = lookupResult->getAddress();

        // Update stub with new address
        std::string stableName = implName.substr(0, implName.size() - implSuffix.size());
        if (auto err = _ISM->updatePointer(stableName, implAddr)) {
            HRLogError("Failed to update stub pointer for %s: %s",
                       stableName.c_str(), llvm::toString(std::move(err)).c_str());
            continue;
        }

        HRLogInfo("Updated stub: %s -> 0x%llx", stableName.c_str(), implAddr.getValue());
    }

    return true;
}

void HotReloadImpl::Reload(const std::string& objectPath) noexcept {
    CalledFromNativeGuard guard(true);

    HRLogDebug("Switching to K/N state and requesting threads suspension...");
    auto mainGCLock = mm::GlobalData::Instance().gc().gcLock(); // Serialize global State

    auto* currentThreadData = mm::ThreadRegistry::Instance().CurrentThreadData();
    currentThreadData->suspensionData().requestThreadsSuspension("Hot-Reload");

    CallsCheckerIgnoreGuard allowWait;

    _statsCollector.RegisterStart(static_cast<int64_t>(utility::getCurrentEpoch()));
    _statsCollector.RegisterLoadedObject(objectPath);

    /// 1. Load new object in JIT engine
    auto objFile = LoadObjectFromPath(objectPath);
    if (!objFile) {
        _statsCollector.RegisterEnd(static_cast<int64_t>(utility::getCurrentEpoch()));
        _statsCollector.RegisterSuccessful(false);
        mm::ResumeThreads();
        return;
    }

    try {
        mm::WaitForThreadsSuspension();
        perform(*currentThreadData, parsedDynamicLib);
        statsCollector.registerEnd(static_cast<int64_t>(utility::getCurrentEpoch()));
        statsCollector.registerSuccessful(true);
        HRLogDebug("Invoking HotReload Success Handlers...");
        Kotlin_native_internal_HotReload_invokeSuccessCallback();
        mm::ResumeThreads();
    } catch (const std::exception& e) {
        HRLogError("Hot-reload failed with exception: %s", e.what());
        _statsCollector.RegisterEnd(static_cast<int64_t>(utility::getCurrentEpoch()));
        _statsCollector.RegisterSuccessful(false);
        mm::ResumeThreads();
    }
}

void HotReloadImpl::ReloadClassesAndInstances(
        mm::ThreadData& currentThreadData, std::unordered_map<std::string, llvm::orc::ExecutorAddr> newClasses) const {
    for (const auto& [typeInfoName, typeInfoAddress] : newClasses) {
        if (NON_RELOADABLE_CLASS_SYMBOLS.find(typeInfoName) != NON_RELOADABLE_CLASS_SYMBOLS.end()) {
            HRLogWarning("Cannot reload class of type: %s", typeInfoName.c_str());
            continue;
        }

        const TypeInfo* oldTypeInfo = _objectManager.GetPreviousTypeInfo(typeInfoName);
        if (oldTypeInfo == nullptr) {
            HRLogWarning("Cannot find the old TypeInfo for class: %s", typeInfoName.c_str());
            continue;
        }

        const TypeInfo* newTypeInfo = typeInfoAddress.toPtr<TypeInfo*>();

        auto objectsToReload = FindObjectsToReload(oldTypeInfo);
        /// 2. For each new redefined TypeInfo, reload the instances
        for (const auto& obj : objectsToReload) {
            const auto newInstance = PerformStateTransfer(currentThreadData, obj, newTypeInfo);
            UpdateShadowStackReferences(obj, newInstance);
            UpdateHeapReferences(obj, newInstance);
        }
    }
}

void HotReloadImpl::CreateFunctionStubs(const std::unordered_map<std::string, llvm::orc::ExecutorAddr>& functions) const {
    const std::string implSuffix = HOT_RELOAD_IMPL_SUFFIX;

    for (const auto& [name, addr] : functions) {
        // Only create stubs for implementation symbols (ending with $hr_impl)
        if (name.size() <= implSuffix.size() ||
            name.substr(name.size() - implSuffix.size()) != implSuffix) {
            HRLogDebug("Skipping non-impl function: %s", name.c_str());
            continue;
        }

        // Extract stable name by removing the $hr_impl suffix
        const auto stableName = name.substr(0, name.size() - implSuffix.size());

        // Create stub with stable name pointing to implementation
        auto err = _ISM->createStub(stableName, addr, llvm::JITSymbolFlags::Callable | llvm::JITSymbolFlags::Exported);
        if (err) {
            HRLogError("Failed to create stub for %s: %s",
                       stableName.c_str(), llvm::toString(std::move(err)).c_str());
            continue;
        }

        HRLogDebug("Created stub: %s -> %s @ 0x%llx",
                   stableName.c_str(), name.c_str(), addr.getValue());
    }
}

void HotReloadImpl::ReplaceFunctionStubs(const std::unordered_map<std::string, llvm::orc::ExecutorAddr>& functions) const {
    const std::string implSuffix = HOT_RELOAD_IMPL_SUFFIX;

    for (const auto& [name, newAddr] : functions) {
        // Only handle implementation symbols (ending with $hr_impl)
        if (name.size() <= implSuffix.size() ||
            name.substr(name.size() - implSuffix.size()) != implSuffix) {
            continue;
        }

        // Extract stable name
        std::string stableName = name.substr(0, name.size() - implSuffix.size());

        // Check if stub already exists
        auto existingStub = _ISM->findStub(stableName, true);
        if (!existingStub.getAddress()) {
            // New function added in reload - create stub
            auto err = _ISM->createStub(stableName, newAddr, llvm::JITSymbolFlags::Callable | llvm::JITSymbolFlags::Exported);
            if (err) {
                HRLogError("Failed to create stub for new function %s: %s",
                           stableName.c_str(), llvm::toString(std::move(err)).c_str());
            } else {
                HRLogInfo("Created stub for new function: %s -> 0x%llx",
                          stableName.c_str(), newAddr.getValue());
            }
            continue;
        }

        // Update existing stub to point to new implementation
        if (auto err = _ISM->updatePointer(stableName, newAddr)) {
            HRLogError("Failed to update stub %s: %s",
                       stableName.c_str(), llvm::toString(std::move(err)).c_str());
            continue;
        }

        HRLogInfo("Updated stub: %s -> 0x%llx", stableName.c_str(), newAddr.getValue());
    }
}

void HotReloadImpl::Perform(mm::ThreadData& currentThreadData) noexcept {
    auto& [newFunctions, newClasses] = _objectManager.GetLatestLoadedObject();
    ReloadClassesAndInstances(currentThreadData, newClasses);
    ReplaceFunctionStubs(newFunctions);
    HRLogInfo("End: Hot-Reload");
}

ObjHeader* HotReloadImpl::PerformStateTransfer(mm::ThreadData& currentThreadData, ObjHeader* oldObject, const TypeInfo* newTypeInfo) {
    struct FieldData {
        int32_t offset;
        Konan_RuntimeType runtimeType;

        FieldData() : offset(0), runtimeType(RT_INVALID) {}

        FieldData(const int32_t offset, const Konan_RuntimeType _runtimeType) : offset(offset), runtimeType(_runtimeType) {}
    };

    ObjHeader* newObject = currentThreadData.allocator().allocateObject(newTypeInfo);
    if (newObject == nullptr) {
        HRLogError("allocation of new object of type %s failed!?", newTypeInfo->fqName().c_str());
        return nullptr;
    }

    const auto oldObjectTypeInfo = oldObject->type_info();

    const auto newClassName = newTypeInfo->fqName();
    const auto oldClassName = oldObjectTypeInfo->fqName();

    const ExtendedTypeInfo* oldObjExtendedInfo = oldObjectTypeInfo->extendedInfo_;
    const int32_t oldFieldsCount = oldObjExtendedInfo->fieldsCount_; // How many fields the old objects declared
    const char** oldFieldNames = oldObjExtendedInfo->fieldNames_; // field names are null-terminated
    const int32_t* oldFieldOffsets = oldObjExtendedInfo->fieldOffsets_;
    const auto oldFieldRuntimeTypes = oldObjExtendedInfo->fieldTypes_;

    std::unordered_map<std::string, FieldData> oldObjectFields{};

    for (int32_t i = 0; i < oldFieldsCount; i++) {
        std::string fieldName{oldFieldNames[i]};
        FieldData fieldData{oldFieldOffsets[i], static_cast<Konan_RuntimeType>(oldFieldRuntimeTypes[i])};
        oldObjectFields[fieldName] = fieldData;
    }

    const ExtendedTypeInfo* newObjExtendedInfo = newObject->type_info()->extendedInfo_;
    const int32_t newFieldsCount = newObjExtendedInfo->fieldsCount_; // How many fields the old objects declared
    const char** newFieldNames = newObjExtendedInfo->fieldNames_; // field names are null-terminated
    const int32_t* newFieldOffsets = newObjExtendedInfo->fieldOffsets_;
    const auto newFieldRuntimeTypes = newObjExtendedInfo->fieldTypes_;

    for (int32_t i = 0; i < newFieldsCount; i++) {
        const char* newFieldName = newFieldNames[i];
        const uint8_t newFieldRuntimeType = newFieldRuntimeTypes[i];
        const uint8_t newFieldOffset = newFieldOffsets[i];

        if (const auto foundField = oldObjectFields.find(newFieldName); foundField == oldObjectFields.end()) {
            HRLogDebug("Field '%s::%s' is new, it won't be copied", newFieldName, newClassName.c_str());
            continue;
        }

        // Performs type-checking. Note that this type checking is shallow, i.e., it does not check the object classes.
        const auto& [oldFieldOffset, oldFieldRuntimeType] = oldObjectFields[newFieldName];
        if (oldFieldRuntimeType != newFieldRuntimeType) {
            HRLogInfo(
                    "Failed type-checking: %s::%s:%s != %s::%s:%s", newClassName.c_str(), newFieldName,
                    utility::kTypeNames[oldFieldRuntimeType], oldClassName.c_str(), newFieldName, utility::kTypeNames[newFieldRuntimeType]);
            continue;
        }

        // Handle Kotlin Objects in a different way, the updates must be notified to the GC

        // TODO: investigate null discussion
        if (oldFieldRuntimeType == RT_OBJECT) {
            const auto oldFieldLocation = reinterpret_cast<ObjHeader**>(reinterpret_cast<uint8_t*>(oldObject) + oldFieldOffset);
            const auto newFieldLocation = reinterpret_cast<ObjHeader**>(reinterpret_cast<uint8_t*>(newObject) + newFieldOffset);

            UpdateHeapRef(newFieldLocation, *oldFieldLocation);
            *newFieldLocation = *oldFieldLocation; // Just copy the reference to the previous object

            HRLogWarning("EXPERIMENTAL: Object reference updated from '%p' to '%p'.", *oldFieldLocation, *newFieldLocation);
            HRLogWarning("EXPERIMENTAL: For the current milestone, object reference type check is omitted.");
            continue;
        }

        const auto oldFieldData = reinterpret_cast<uint8_t*>(oldObject) + oldFieldOffset;
        const auto newFieldData = reinterpret_cast<uint8_t*>(newObject) + newFieldOffset;

        // HRLogDebug("copying field %s", utility::field2String(newFieldName.c_str(), oldFieldData, oldFieldRuntimeType).c_str());

        // Perform byte-copy of the field
        std::memcpy(newFieldData, oldFieldData, utility::kRuntimeTypeSize[oldFieldRuntimeType]);
    }

    return newObject;
}

std::vector<ObjHeader*> HotReloadImpl::FindObjectsToReload(const TypeInfo* oldTypeInfo) const {
    std::vector<ObjHeader*> existingObjects{};
    std::string oldTypeFqName = oldTypeInfo->fqName();

    visitObjectGraph(nullptr, [&existingObjects, &oldTypeFqName](ObjHeader* nextObject, auto processObject) {
        // Traverse object references inside class properties
        traverseObjectFieldsInternal(nextObject, [&](const mm::RefFieldAccessor& fieldAccessor) {
            processObject(fieldAccessor.direct(), utility::ReferenceOrigin::ObjRef);
        });

        if (nextObject->type_info()->fqName() == oldTypeFqName) {
            HRLogDebug("Instance of class '%s' at '%p', must be reloaded", nextObject->type_info()->fqName().c_str(), nextObject);
            existingObjects.emplace_back(nextObject);
        }
    });
    return existingObjects;
}

int HotReloadImpl::UpdateHeapReferences(ObjHeader* oldObject, ObjHeader* newObject) {
    int32_t updatedObjects{0};

    std::queue<ObjHeader*> objectsToVisit{};
    std::unordered_set<ObjHeader*> visitedObjects{};

    // Let's start collecting the root set
    auto processObject = [&](ObjHeader* obj, utility::ReferenceOrigin origin) {
        const char* originString = utility::referenceOriginToString(origin);
        if (obj == nullptr || isNullOrMarker(obj)) return;

        if (const auto visited = visitedObjects.find(obj); visited != visitedObjects.end()) return;
        HRLogDebug("processing object of type '%s' from %s", obj->type_info()->fqName().c_str(), originString);

        visitedObjects.insert(obj);
        objectsToVisit.push(obj);
    };

    auto threadRegistry = mm::ThreadRegistry::Instance().LockForIter();
    for (auto& thread : threadRegistry) {
        for (const auto& objectLocation : thread.tls()) {
            if (*objectLocation == oldObject) {
                *objectLocation = newObject;
                UpdateHeapRef(objectLocation, newObject);
                updatedObjects++;
            }
            processObject(*objectLocation, utility::ReferenceOrigin::Global);
        }
    }

    auto globalsIterable = mm::GlobalData::Instance().globalsRegistry().LockForIter();
    for (const auto& objectLocation : globalsIterable) {
        if (*objectLocation == oldObject) {
            *objectLocation = newObject;
            UpdateHeapRef(objectLocation, newObject);
            updatedObjects++;
        }

        processObject(*objectLocation, utility::ReferenceOrigin::Global);
    }

    for (auto& thread : mm::ThreadRegistry::Instance().LockForIter()) {
        auto& shadowStack = thread.shadowStack();
        for (const auto& object : shadowStack) {
            processObject(object, utility::ReferenceOrigin::ShadowStack);
        }
    }

    processObject(oldObject, utility::ReferenceOrigin::ObjRef);

    HRLogDebug("Updating Heap References :: starting visit with %ld objects", objectsToVisit.size());

    while (!objectsToVisit.empty()) {
        const auto nextObject = objectsToVisit.front();
        objectsToVisit.pop();

        traverseObjectFields(nextObject, [&](mm::RefFieldAccessor fieldAccessor) {
            ObjHeader* fieldValue = fieldAccessor.direct();
            if (fieldValue == oldObject) {
                fieldAccessor.store(newObject);
                updatedObjects++;
            }
            processObject(fieldValue, utility::ReferenceOrigin::ObjRef);
        });
    }

    return updatedObjects;
}

void HotReloadImpl::UpdateShadowStackReferences(const ObjHeader* oldObject, ObjHeader* newObject) {
    for (auto& threadData : mm::ThreadRegistry::Instance().LockForIter()) {
        mm::ShadowStack& shadowStack = threadData.shadowStack();
        for (auto it = shadowStack.begin(); it != shadowStack.end(); ++it) {
            if (ObjHeader*& currentRef = *it; currentRef == oldObject) {
                currentRef = newObject;
            }
        }
    }
}

// endregion

#endif