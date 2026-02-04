#ifdef KONAN_HOT_RELOAD

#include <unordered_set>
#include <queue>
#include <iomanip>
#include <chrono>
#include <fstream>
#include <utility>
#include <dlfcn.h>
#include <unistd.h>

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
#include "HotReloadPlugins.hpp"

#if KONAN_OBJC_INTEROP
#include "ObjCExportInit.h"

// Extern declaration for the weak symbol defined in ObjCInterop.mm
// This needs to be set from JIT'd code before ObjC class creation
extern "C" __attribute__((weak)) const char* Kotlin_ObjCInterop_uniquePrefix;
#endif

using namespace kotlin;

using kotlin::hot::HotReloadImpl;
using kotlin::hot::KotlinObjectFile;
using kotlin::hot::ObjectManager;
using kotlin::hot::MANGLED_FUN_NAME_PREFIX;
using kotlin::hot::MANGLED_CLASS_NAME_PREFIX;
using kotlin::hot::IMPL_SUFFIX;
using kotlin::hot::LatestObjectListener;
using kotlin::hot::StubDefinitionGenerator;
using kotlin::hot::WeakSymbolFallbackGenerator;
#if defined(__APPLE__)
using kotlin::hot::ObjCSelectorFixupPlugin;
#endif

/// Counter for creating unique JITDylib names for each reload
static int RELOAD_COUNTER = 0;

static const auto ORC_RUNTIME_PATH = "/opt/homebrew/Cellar/llvm/21.1.0/lib/clang/21/lib/darwin/liborc_rt_osx.a";

/// Check if a string ends with a suffix (C++17 compatible)
static bool endsWith(std::string_view str, std::string_view suffix) noexcept {
    return str.size() >= suffix.size() && str.compare(str.size() - suffix.size(), suffix.size(), suffix) == 0;
}

/// Extract stable name from impl name (removes $hr_impl suffix) as string_view.
/// Caller must ensure implName outlives the returned view.
static std::string_view getStableName(const std::string_view implName) noexcept {
    return implName.substr(0, implName.size() - IMPL_SUFFIX.size());
}

static ManuallyScoped<HotReloadImpl> globalDataInstance{};

static llvm::ExitOnError ExitOnErr;

/// Forward declarations for GC functions.
namespace kotlin::gc {
void stopTheWorld(GCHandle gcHandle, const char* reason) noexcept;
void resumeTheWorld(GCHandle gcHandle) noexcept;
} // namespace kotlin::gc

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

void HotReloadImpl::AddDebugInfoRegistrationPlugin(llvm::orc::JITDylib& JD) const {
    auto& ES = _JIT->getExecutionSession();
    auto& TT = _JIT->getTargetTriple();
    if (auto* OLL = llvm::dyn_cast<llvm::orc::ObjectLinkingLayer>(&_JIT->getObjLinkingLayer())) {
#if defined(__APPLE__)
        if (auto debuggerPlugin = llvm::orc::GDBJITDebugInfoRegistrationPlugin::Create(ES, JD, TT)) {
            OLL->addPlugin(std::move(*debuggerPlugin));
            HRLogDebug("GDBJITDebugInfoRegistrationPlugin added for debug symbol support");
        } else {
            HRLogWarning("Failed to create GDBJITDebugInfoRegistrationPlugin: %s", llvm::toString(debuggerPlugin.takeError()).c_str());
        }
#elif defined(__LINUX__)
        HRLogWarning("Debug info registration not supported on Linux yet.");
#endif
    }
}

// MachOPlatform is now set up during LLJIT construction via setPlatformSetUp callback

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
                        auto OLL = std::make_unique<llvm::orc::ObjectLinkingLayer>(
                                ES, ExitOnErr(llvm::jitlink::InProcessMemoryManager::Create()));
                        OLL->addPlugin(std::make_unique<LatestObjectListener>(_objectManager));
#if defined(__APPLE__)
                        // Add ObjC selector fixup plugin. MachOPlatform may not reliably fix up
                        // selectors in all JIT'd object files, so we do it explicitly during
                        // the JITLink PostFixup pass before any code runs.
                        OLL->addPlugin(std::make_unique<ObjCSelectorFixupPlugin>());
#endif
                        return OLL;
                    })
                    .setJITTargetMachineBuilder(std::move(JTMB))
                    .setPlatformSetUp([](llvm::orc::LLJIT& J) -> llvm::Expected<llvm::orc::JITDylibSP> {
#if defined(__APPLE__)
                        auto& ES = J.getExecutionSession();
                        auto* OLL = llvm::dyn_cast<llvm::orc::ObjectLinkingLayer>(&J.getObjLinkingLayer());
                        if (!OLL) {
                            return llvm::make_error<llvm::StringError>(
                                    "ObjectLinkingLayer required for MachOPlatform", llvm::inconvertibleErrorCode());
                        }

                        auto& JDPlatform = ES.createBareJITDylib("__orc_rt_platform");
                        auto DLG = llvm::orc::DynamicLibrarySearchGenerator::GetForCurrentProcess(J.getDataLayout().getGlobalPrefix());
                        if (!DLG) return DLG.takeError();

                        JDPlatform.addGenerator(std::move(*DLG));

                        auto MachoPlatform = llvm::orc::MachOPlatform::Create(*OLL, JDPlatform, ORC_RUNTIME_PATH);
                        if (!MachoPlatform) return MachoPlatform.takeError();

                        ES.setPlatform(std::move(*MachoPlatform));
                        HRLogDebug("MachOPlatform created and set on ExecutionSession");
                        return &JDPlatform;
#else
                        return nullptr;
#endif
                    })
                    .create());

    auto& MainJD = _JIT->getMainJITDylib();
    auto& TT = _JIT->getTargetTriple();
    auto& ES = _JIT->getExecutionSession();

    AddDebugInfoRegistrationPlugin(MainJD);

    _LCTM = ExitOnErr(llvm::orc::createLocalLazyCallThroughManager(TT, ES, llvm::orc::ExecutorAddr()));

    _ISM = llvm::orc::createLocalIndirectStubsManagerBuilder(TT)();

    // Add stub definition generator - resolves stable function names to stub addresses.
    // When JITLink encounters a reference to "kfun:foo", this generator looks up
    // "kfun:foo$hr_impl", creates a stub, and returns the stub address.
    // Stubs are created lazily on first use.
    MainJD.addGenerator(std::make_unique<StubDefinitionGenerator>(*_ISM));

    // Add fallback generator for missing symbols (e.g., C++ RTTI, ObjC constants).
    // This is added LAST so it only handles symbols that no other generator could resolve.
    // These symbols will have null addresses - if called at runtime, they will crash.
    MainJD.addGenerator(std::make_unique<WeakSymbolFallbackGenerator>());

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

/// Parse an object file and extract all $hr_impl symbols. Returns nullopt on failure.
std::optional<ParsedObjectFile> HotReloadImpl::ParseObjectFile(const std::string_view objectPath) {
    auto objBufferOrError = llvm::MemoryBuffer::getFile(objectPath);
    if (!objBufferOrError) {
        HRLogError("Failed to read object file %s: %s", std::string(objectPath).c_str(), objBufferOrError.getError().message().c_str());
        return std::nullopt;
    }
    auto objBuffer = std::move(*objBufferOrError);

    auto objFile = llvm::object::ObjectFile::createObjectFile(objBuffer->getMemBufferRef());
    if (!objFile) {
        llvm::consumeError(objFile.takeError());
        HRLogError("Failed to parse object file: %s", std::string(objectPath).c_str());
        return std::nullopt;
    }

    std::vector<std::string> implSymbols;

    for (const auto& sym : (*objFile)->symbols()) {
        auto nameOrErr = sym.getName();
        if (!nameOrErr) {
            llvm::consumeError(nameOrErr.takeError());
            continue;
        }
        auto name = nameOrErr->str();

        auto flagsOrErr = sym.getFlags();
        if (!flagsOrErr) {
            llvm::consumeError(flagsOrErr.takeError());
            continue;
        }

        // Check if this is a defined $hr_impl symbol
        if (const auto flags = *flagsOrErr; (flags & llvm::object::SymbolRef::SF_Undefined) == 0 &&
            name.find(MANGLED_FUN_NAME_PREFIX) != std::string::npos && endsWith(name, IMPL_SUFFIX)) {
            implSymbols.push_back(name);
        }
    }

    return ParsedObjectFile{std::move(objBuffer), std::move(implSymbols)};
}

/// Ensure placeholder stubs exist for all impl symbols.
/// If checkExisting is true, only creates stubs that don't already exist.
void HotReloadImpl::EnsurePlaceholderStubs(const std::vector<std::string>& implSymbols, bool checkExisting) const {
    auto& ES = _JIT->getExecutionSession();

    for (const auto& implName : implSymbols) {
        const auto stableName = getStableName(implName);
        auto stableNameSymbol = ES.intern(llvm::StringRef(stableName.data(), stableName.size()));

        if (checkExisting) {
            if (auto existingStub = _ISM->findStub(*stableNameSymbol, true); existingStub.getAddress()) {
                HRLogDebug("Stub already exists for: %.*s", static_cast<int>(stableName.size()), stableName.data());
                continue;
            }
        }

        if (auto err = _ISM->createStub(*stableNameSymbol, llvm::orc::ExecutorAddr(), llvm::JITSymbolFlags::Callable | llvm::JITSymbolFlags::Exported)) {
            HRLogError(
                    "Failed to create placeholder stub for %.*s: %s", static_cast<int>(stableName.size()), stableName.data(),
                    llvm::toString(std::move(err)).c_str());
            continue;
        }
        HRLogDebug("Created placeholder stub for: %.*s -> %s", static_cast<int>(stableName.size()), stableName.data(), implName.c_str());
    }
}

/// Look up impl symbols in the given JITDylib and update the corresponding stubs.
void HotReloadImpl::UpdateStubPointers(llvm::orc::JITDylib& JD, const std::vector<std::string>& implSymbols) const {
    auto& ES = _JIT->getExecutionSession();

    for (const auto& implName : implSymbols) {
        const auto implSymbol = ES.intern(implName);
        auto lookupResult = ES.lookup(llvm::orc::makeJITDylibSearchOrder(&JD, llvm::orc::JITDylibLookupFlags::MatchAllSymbols), implSymbol);

        if (auto Err = lookupResult.takeError()) {
            HRLogError("Failed to look up impl symbol %s: %s", implName.c_str(), llvm::toString(std::move(Err)).c_str());
            continue;
        }

        const auto implAddr = lookupResult->getAddress();
        const auto stableName = getStableName(implName);

        if (auto err = _ISM->updatePointer(llvm::StringRef(stableName.data(), stableName.size()), implAddr)) {
            HRLogError(
                    "Failed to update stub pointer for %.*s: %s", static_cast<int>(stableName.size()), stableName.data(),
                    llvm::toString(std::move(err)).c_str());
            continue;
        }

        HRLogDebug("Updated stub: %.*s -> 0x%llx", static_cast<int>(stableName.size()), stableName.data(), implAddr.getValue());
    }
}

#if KONAN_OBJC_INTEROP

/// Initialize ObjC unique prefix from JIT'd code.
/// This must happen before any ObjC class creation (CreateKotlinObjCClass).
/// The compiler generates Kotlin_ObjCInterop_uniquePrefix in bootstrap.o,
/// but the runtime has a weak symbol initialized to nullptr.
void HotReloadImpl::InitializeObjCUniquePrefixFromJIT() const {
    auto& MainJD = _JIT->getMainJITDylib();
    auto& ES = _JIT->getExecutionSession();

    HRLogDebug("Looking up ObjC unique prefix symbol from JIT...");

    // Symbol name with underscore prefix for macOS
    const auto prefixName = ES.intern("_Kotlin_ObjCInterop_uniquePrefix");

    auto prefixResult = ES.lookup(llvm::orc::makeJITDylibSearchOrder(&MainJD), prefixName);

    if (!prefixResult) {
        llvm::consumeError(prefixResult.takeError());
        HRLogError("_Kotlin_ObjCInterop_uniquePrefix not found in JIT'd code");
        return;
    }

    // The symbol is a pointer to const char*, so dereference to get the string pointer
    const char* jitPrefix = *prefixResult->getAddress().toPtr<const char**>();

    if (jitPrefix == nullptr) {
        HRLogError("_Kotlin_ObjCInterop_uniquePrefix in JIT'd code is null");
        return;
    }

    HRLogDebug("Found _Kotlin_ObjCInterop_uniquePrefix @ 0x%llx -> \"%s\"", prefixResult->getAddress().getValue(), jitPrefix);

    // Set the runtime's weak symbol to the JIT value
    Kotlin_ObjCInterop_uniquePrefix = jitPrefix;
    HRLogDebug("ObjC unique prefix initialized: \"%s\"", Kotlin_ObjCInterop_uniquePrefix);
}

/// Initialize ObjC type adapters from symbols defined in JIT'd code.
/// This looks up the adapter arrays from the JIT and passes them to the runtime.
///
/// NOTE: We do NOT check _Kotlin_ObjCExport_initTypeAdapters flag here because:
/// - That flag is a weak symbol with default value (false) in host.kexe
/// - The actual adapter arrays are in bootstrap.o (JIT-loaded code)
/// - DynamicLibrarySearchGenerator finds the host's weak default first
/// So we directly look for the adapter arrays themselves.
void HotReloadImpl::InitializeObjCAdaptersFromJIT() const {
    auto& MainJD = _JIT->getMainJITDylib();
    auto& ES = _JIT->getExecutionSession();

    HRLogDebug("Looking up ObjC type adapter symbols from JIT...");

    // Symbol names for ObjC type adapters (with underscore prefix for macOS)
    const auto classAdaptersName = ES.intern("_Kotlin_ObjCExport_sortedClassAdapters");
    const auto classAdaptersNumName = ES.intern("_Kotlin_ObjCExport_sortedClassAdaptersNum");
    const auto protocolAdaptersName = ES.intern("_Kotlin_ObjCExport_sortedProtocolAdapters");
    const auto protocolAdaptersNumName = ES.intern("_Kotlin_ObjCExport_sortedProtocolAdaptersNum");

    // Look up class adapters
    const ObjCTypeAdapter** classAdapters = nullptr;
    int classAdaptersNum = 0;

    auto classAdaptersResult = ES.lookup(llvm::orc::makeJITDylibSearchOrder(&MainJD), classAdaptersName);
    if (!classAdaptersResult) {
        llvm::consumeError(classAdaptersResult.takeError());
        HRLogDebug("_Kotlin_ObjCExport_sortedClassAdapters not found");
    } else {
        // The symbol is a pointer to array, so we dereference once to get the array pointer
        classAdapters = *classAdaptersResult->getAddress().toPtr<const ObjCTypeAdapter***>();
        HRLogDebug(
                "Found _Kotlin_ObjCExport_sortedClassAdapters @ 0x%llx -> 0x%llx", classAdaptersResult->getAddress().getValue(),
                (unsigned long long)classAdapters);
    }

    auto classAdaptersNumResult = ES.lookup(llvm::orc::makeJITDylibSearchOrder(&MainJD), classAdaptersNumName);
    if (!classAdaptersNumResult) {
        llvm::consumeError(classAdaptersNumResult.takeError());
        HRLogDebug("_Kotlin_ObjCExport_sortedClassAdaptersNum not found");
    } else {
        classAdaptersNum = *classAdaptersNumResult->getAddress().toPtr<int*>();
        HRLogDebug("Found _Kotlin_ObjCExport_sortedClassAdaptersNum = %d", classAdaptersNum);
    }

    // Look up protocol adapters
    const ObjCTypeAdapter** protocolAdapters = nullptr;
    int protocolAdaptersNum = 0;

    auto protocolAdaptersResult = ES.lookup(llvm::orc::makeJITDylibSearchOrder(&MainJD), protocolAdaptersName);
    if (!protocolAdaptersResult) {
        llvm::consumeError(protocolAdaptersResult.takeError());
        HRLogDebug("_Kotlin_ObjCExport_sortedProtocolAdapters not found");
    } else {
        protocolAdapters = *protocolAdaptersResult->getAddress().toPtr<const ObjCTypeAdapter***>();
        HRLogDebug(
                "Found _Kotlin_ObjCExport_sortedProtocolAdapters @ 0x%llx -> 0x%llx", protocolAdaptersResult->getAddress().getValue(),
                (unsigned long long)protocolAdapters);
    }

    auto protocolAdaptersNumResult = ES.lookup(llvm::orc::makeJITDylibSearchOrder(&MainJD), protocolAdaptersNumName);
    if (!protocolAdaptersNumResult) {
        llvm::consumeError(protocolAdaptersNumResult.takeError());
        HRLogDebug("_Kotlin_ObjCExport_sortedProtocolAdaptersNum not found");
    } else {
        protocolAdaptersNum = *protocolAdaptersNumResult->getAddress().toPtr<int*>();
        HRLogDebug("Found _Kotlin_ObjCExport_sortedProtocolAdaptersNum = %d", protocolAdaptersNum);
    }

    HRLogDebug("Found %d class adapters and %d protocol adapters from JIT", classAdaptersNum, protocolAdaptersNum);

    // Initialize adapters in the runtime
    if (classAdaptersNum > 0 || protocolAdaptersNum > 0) {
        Kotlin_ObjCExport_initializeTypeAdaptersWithPointers(classAdapters, classAdaptersNum, protocolAdapters, protocolAdaptersNum);
        HRLogDebug("ObjC type adapters initialized from JIT");
    } else {
        HRLogDebug("No ObjC type adapters found in JIT'd code");
    }
}
#endif

KonanStartFunc HotReloadImpl::LoadBoostrapFile(const char* boostrapFilePath) const {
    HRLogDebug("Loading bootstrap file: %s", boostrapFilePath);

    const auto parsed = ParseObjectFile(boostrapFilePath);
    if (!parsed) {
        HRLogError("Bootstrap file failed to load!");
        return nullptr;
    }

    auto& MainJD = _JIT->getMainJITDylib();

    HRLogDebug("Found %zu impl symbols, creating placeholder stubs", parsed->implSymbols.size());

    // Create placeholder stubs BEFORE adding object to JIT (see doc for why)
    EnsurePlaceholderStubs(parsed->implSymbols, /*checkExisting=*/false);

    // Add object file to main JITDylib
    ExitOnErr(
            _JIT->addObjectFile(llvm::MemoryBuffer::getMemBufferCopy(parsed->buffer->getBuffer(), parsed->buffer->getBufferIdentifier())));

    HRLogDebug("Bootstrap file added to JIT.");

    // Update stubs with real addresses
    UpdateStubPointers(MainJD, parsed->implSymbols);

#if KONAN_OBJC_INTEROP
    // Initialize ObjC unique prefix from JIT'd code.
    // This must happen before any ObjC class creation (CreateKotlinObjCClass).
    InitializeObjCUniquePrefixFromJIT();

    // Initialize ObjC type adapters from JIT'd symbols.
    // This must happen after the bootstrap is loaded but before user code runs,
    // because ObjC interop calls need the type adapters to be set up.
    InitializeObjCAdaptersFromJIT();
#endif

    // Look up Konan_start entry point
    HRLogDebug("Looking up `Konan_start` symbol...");

    const auto konanStartSymbol = ExitOnErr(_JIT->lookup("Konan_start"));
    return konanStartSymbol.toPtr<KonanStartFunc>();
}

bool HotReloadImpl::LoadObjectFromPath(std::string_view objectPath) const {
    HRLogDebug("Loading object file for reload: %s", std::string(objectPath).c_str());

    const auto parsed = ParseObjectFile(objectPath);
    if (!parsed) return false;

    HRLogDebug("Found %zu impl symbols in reload object", parsed->implSymbols.size());

    // Ensure placeholder stubs exist (check for existing ones from previous loads)
    EnsurePlaceholderStubs(parsed->implSymbols, /*checkExisting=*/true);

    // Create a NEW JITDylib for this reload to avoid duplicate symbol errors
    auto& ES = _JIT->getExecutionSession();

    const auto reloadDylibName = "reload_" + std::to_string(++RELOAD_COUNTER);
    auto& ReloadJD = ES.createBareJITDylib(reloadDylibName);
    HRLogDebug("Created new JITDylib: %s", reloadDylibName.c_str());

    // Link reload dylib to main dylib for runtime symbol access
    ReloadJD.addToLinkOrder(_JIT->getMainJITDylib());

    // Add DynamicLibrarySearchGenerator directly to ReloadJD for resolving system symbols.
    // While ReloadJD links to MainJD (which has its own DLG), the link order doesn't always
    // propagate generator lookups correctly during __mod_init_func processing.
    // Adding a DLG directly ensures ReloadJD can resolve symbols like _atexit.
    auto ReloadDLG = llvm::orc::DynamicLibrarySearchGenerator::GetForCurrentProcess(
            _JIT->getDataLayout().getGlobalPrefix(), [](const llvm::orc::SymbolStringPtr& Name) {
                const auto& nameStr = *Name;
                if (nameStr.starts_with("_Kotlin_") || nameStr.starts_with("_kfun:") || nameStr.starts_with("_kclass:") ||
                    nameStr.starts_with("_ktypew:") || nameStr.starts_with("_kniBridge") || nameStr == "___dso_handle") {
                    return false;
                }
                return true;
            });
    if (ReloadDLG) {
        ReloadJD.addGenerator(std::move(*ReloadDLG));
        HRLogDebug("Added DynamicLibrarySearchGenerator to %s", reloadDylibName.c_str());
    } else {
        HRLogWarning(
                "Failed to create DynamicLibrarySearchGenerator for %s: %s", reloadDylibName.c_str(),
                llvm::toString(ReloadDLG.takeError()).c_str());
    }

    // Set up reload dylib with MachOPlatform so it can resolve _atexit and other
    // platform symbols needed for __mod_init_func processing
    if (auto* Platform = ES.getPlatform()) {
        if (auto Err = Platform->setupJITDylib(ReloadJD)) {
            HRLogWarning("setupJITDylib(ReloadJD) failed: %s", llvm::toString(std::move(Err)).c_str());
        }
    }

    // Add object file to the reload JITDylib
    if (auto err = _JIT->addObjectFile(
                ReloadJD, llvm::MemoryBuffer::getMemBufferCopy(parsed->buffer->getBuffer(), parsed->buffer->getBufferIdentifier()))) {
        HRLogError("Failed to add object file to JIT engine: %s", llvm::toString(std::move(err)).c_str());
        return false;
    }
    HRLogDebug("Object file added to JIT (dylib: %s).", reloadDylibName.c_str());

    // Update stubs with new addresses from reload dylib
    UpdateStubPointers(ReloadJD, parsed->implSymbols);

    return true;
}

void HotReloadImpl::Reload(const std::string& objectPath) noexcept {
    CalledFromNativeGuard guard(true);

    HRLogDebug("Switching to K/N state and requesting threads suspension...");
    auto mainGCLock = mm::GlobalData::Instance().gc().gcLock(); // Serialize global State

    auto* currentThreadData = mm::ThreadRegistry::Instance().CurrentThreadData();
    currentThreadData->suspensionData().requestThreadsSuspension("Hot-Reload");

    // From this point on, the threads could be suspended. Remember to invoke `ResumeThreads`.
    // TODO: maybe it is a good idea to use RAII here.

    CallsCheckerIgnoreGuard allowWait;

    _statsCollector.RegisterStart(static_cast<int64_t>(utility::getCurrentEpoch()));
    _statsCollector.RegisterLoadedObject(objectPath);

    /// 1. Load new object in JIT engine
    if (const auto objFile = LoadObjectFromPath(objectPath); !objFile) {
        _statsCollector.RegisterEnd(static_cast<int64_t>(utility::getCurrentEpoch()));
        _statsCollector.RegisterSuccessful(false);
        mm::ResumeThreads();
        return;
    }

    try {
        mm::WaitForThreadsSuspension();
        Perform(*currentThreadData);
        _statsCollector.RegisterEnd(static_cast<int64_t>(utility::getCurrentEpoch()));
        _statsCollector.RegisterSuccessful(true);
        HRLogDebug("Resuming threads...");
        mm::ResumeThreads();
        HRLogDebug("Threads resumed successfully. Invoking success handlers (if any)...");
        Kotlin_native_internal_HotReload_invokeSuccessCallback();
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
    for (const auto& [name, addr] : functions) {
        // Only create stubs for implementation symbols (ending with $hr_impl)
        if (name.size() <= IMPL_SUFFIX.size() || name.substr(name.size() - IMPL_SUFFIX.size()) != IMPL_SUFFIX) {
            HRLogDebug("Skipping non-impl function: %s", name.c_str());
            continue;
        }

        // Extract stable name by removing the $hr_impl suffix
        const auto stableName = name.substr(0, name.size() - IMPL_SUFFIX.size());

        // Create stub with stable name pointing to implementation
        if (auto err = _ISM->createStub(stableName, addr, llvm::JITSymbolFlags::Callable | llvm::JITSymbolFlags::Exported)) {
            HRLogError("Failed to create stub for %s: %s", stableName.c_str(), llvm::toString(std::move(err)).c_str());
            continue;
        }

        HRLogDebug("Created stub: %s -> %s @ 0x%llx", stableName.c_str(), name.c_str(), addr.getValue());
    }
}

void HotReloadImpl::ReplaceFunctionStubs(const std::unordered_map<std::string, llvm::orc::ExecutorAddr>& functions) const {
    for (const auto& [name, newAddr] : functions) {
        // Only handle implementation symbols (ending with $hr_impl)
        if (name.size() <= IMPL_SUFFIX.size() || name.substr(name.size() - IMPL_SUFFIX.size()) != IMPL_SUFFIX) {
            continue;
        }

        // Extract stable name
        const auto stableName = name.substr(0, name.size() - IMPL_SUFFIX.size());

        // Check if a stub already exists
        auto existingStub = _ISM->findStub(stableName, true);
        if (!existingStub.getAddress()) {
            // New function added in reload - create stub
            if (auto err = _ISM->createStub(stableName, newAddr, llvm::JITSymbolFlags::Callable | llvm::JITSymbolFlags::Exported)) {
                HRLogError("Failed to create stub for new function %s: %s", stableName.c_str(), llvm::toString(std::move(err)).c_str());
            } else {
                HRLogInfo("Created stub for new function: %s -> 0x%llx", stableName.c_str(), newAddr.getValue());
            }
            continue;
        }

        // Update the existing stub to point to the new implementation
        if (auto err = _ISM->updatePointer(stableName, newAddr)) {
            HRLogError("Failed to update stub %s: %s", stableName.c_str(), llvm::toString(std::move(err)).c_str());
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