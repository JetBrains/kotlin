#ifdef KONAN_HOT_RELOAD

#include <deque>
#include <unordered_map>
#include <unordered_set>
#include <queue>
#include <chrono>
#include <fstream>
#include <utility>
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

#include "Runtime.h"
#include "HotReloadUtility.hpp"
#include "plugins/HotReloadPlugins.hpp"

#include "llvm/Support/TargetSelect.h"
#include "llvm/ExecutionEngine/Orc/LinkGraphLinkingLayer.h"
#include "llvm/ExecutionEngine/Orc/Debugging/DebuggerSupportPlugin.h"
#include "llvm/ExecutionEngine/Orc/MachOPlatform.h"
#include "llvm/ExecutionEngine/Orc/ExecutionUtils.h"
#include "llvm/ExecutionEngine/Orc/UnwindInfoRegistrationPlugin.h"
#include "llvm/ExecutionEngine/Orc/MapperJITLinkMemoryManager.h"
#include "llvm/ExecutionEngine/Orc/MemoryMapper.h"

#if KONAN_OBJC_INTEROP
#include "ObjCExportInit.h"

// Extern declaration for the weak symbol defined in ObjCInterop.mm
// This needs to be set from JIT'd code before ObjC class creation
extern "C" __attribute__((weak)) const char* Kotlin_ObjCInterop_uniquePrefix;
#endif

using namespace kotlin;

using hot::HotReloadImpl;
using hot::KotlinObjectFile;

// region Constants
static constexpr auto kStubsJdName = "KNHR_stubs";
static constexpr auto kBootstrapJdName = "KNHR_bootstrap";
static constexpr auto kReloadJdName = "KNHR_reload$";

static constexpr auto kKonanStartSymbol = "_Konan_start";
static constexpr auto kKonanConstructorsSymbol = "__Konan_constructors";

static constexpr auto kOrcRuntimePathEnv = "KONAN_ORC_RUNTIME_PATH";
static constexpr auto kDefaultBrewOrcRuntimePath =
        "~/.konan/dependencies/llvm-21-aarch64-macos-dev-93/lib/clang/21/lib/darwin/liborc_rt_osx.a";
// endregion

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

        // HRLogDebug("processing object of type %s from %s", obj->type_info()->fqName().c_str(), originString);
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

// region Extern "C" functions

extern "C" {
void Kotlin_native_internal_HotReload_perform(ObjHeader* obj, const ObjHeader* dylibPathStr) {
    AssertThreadState(ThreadState::kRunnable);

    // TODO: fix implementation. This is not a priority function atm.

    // const auto dylibPath = kotlin::to_string<KStringConversionMode::UNCHECKED>(dylibPathStr);
    // HotReloadImpl::Instance().Reload(dylibPath);
}
}

// endregion

// region HotReload

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
    return statsCollector_;
}

void HotReloadImpl::StartServer() {
    if (server_.start()) {
        server_.run([this](const std::vector<std::string>& objectPaths) { Reload(objectPaths); });
        return;
    }
    HRLogError("Failed to start HotReload server, maybe the TCP port `%d` is already busy?", HotReloadServer::GetDefaultPort());
}

llvm::orc::JITDylib* HotReloadImpl::getStubsJD() const {
    return jit_->getJITDylibByName(kStubsJdName);
}

void HotReloadImpl::SetupORC() {
    HRLogDebug("Setting up ORC JIT...");

    const char* envPath = std::getenv(kOrcRuntimePathEnv);
    orcRuntimePath_ = envPath ? std::string{envPath} : std::string{kDefaultBrewOrcRuntimePath};
    const auto& orcRuntimePath = orcRuntimePath_;

    HRLogDebug("Will load ORC Runtime from: %s", orcRuntimePath.c_str());

    // Initialize LLVM
    llvm::InitializeNativeTarget();
    // Required for debug symbols in JITted code
    llvm::InitializeNativeTargetAsmPrinter();
    llvm::InitializeNativeTargetAsmParser();
    llvm::InitializeNativeTargetDisassembler();

    auto JTMB = ExitOnErr(llvm::orc::JITTargetMachineBuilder::detectHost());

    jit_ = ExitOnErr(
            llvm::orc::LLJITBuilder()
                    .setJITTargetMachineBuilder(std::move(JTMB))
                    .setPlatformSetUp(llvm::orc::ExecutorNativePlatform(orcRuntimePath))
                    .setObjectLinkingLayerCreator([](llvm::orc::ExecutionSession& ES) {
                        // This prevents 32-bit delta overflow in __unwind_info
                        constexpr uint64_t slabSize = 1024 * 1024 * 1024; // 1 GB
                        auto oll = std::make_unique<llvm::orc::ObjectLinkingLayer>(
                                ES,
                                ExitOnErr(
                                        llvm::orc::MapperJITLinkMemoryManager::CreateWithMapper<llvm::orc::InProcessMemoryMapper>(
                                                slabSize)));
#if defined(__APPLE__)
                        // TODO: this is a temporary fix, it should be fixed in MachOPlatform
                        // Add ObjC selector fixup plugin. MachOPlatform may not reliably fix up
                        // selectors in all JIT'd object files, so we do it explicitly during
                        // the JITLink PostFixup pass before any code runs.
                        oll->addPlugin(std::make_unique<ObjCSelectorFixupPlugin>());
#endif
                        return oll;
                    })
                    .setNotifyCreatedCallback([this](llvm::orc::LLJIT& J) {
                        auto& mainJD = J.getMainJITDylib();
                        auto& es = J.getExecutionSession();
                        auto& oll = J.getObjLinkingLayer();

                        auto psg = ExitOnErr(
                                llvm::orc::DynamicLibrarySearchGenerator::GetForCurrentProcess(J.getDataLayout().getGlobalPrefix()));

                        mainJD.addGenerator(std::move(psg));

#if defined(__APPLE__)
                        auto dataGen = MachOHostDataSymbolGenerator::CreateForCurrentProcess();
                        if (dataGen) {
                            mainJD.addGenerator(std::move(*dataGen));
                        } else {
                            HRLogWarning("MachOHostDataSymbolGenerator: %s", llvm::toString(dataGen.takeError()).c_str());
                        }
#endif

                        mainJD.addGenerator(std::make_unique<WeakSymbolFallbackGenerator>());

                        auto& stubsJD = ExitOnErr(J.createJITDylib(kStubsJdName));

                        if (const auto jol = llvm::dyn_cast<llvm::orc::ObjectLinkingLayer>(&oll)) {
                            rsm_ = ExitOnErr(llvm::orc::JITLinkRedirectableSymbolManager::Create(*jol));
                            jol->addPlugin(std::make_unique<KotlinSymbolExternalizerPlugin>(stubsJD));
                            jol->addPlugin(ExitOnErr(llvm::orc::UnwindInfoRegistrationPlugin::Create(es)));

            // Setup GDB/LLDB Debugger Plugin to resolve loaded symbols
#if defined(__APPLE__)
                            auto& targetTriple = J.getTargetTriple();
                            jol->addPlugin(ExitOnErr(llvm::orc::GDBJITDebugInfoRegistrationPlugin::Create(es, mainJD, targetTriple)));
#else
                            HRLogWarning("Debug info registration not yet supported on platforms different than macOS.");
#endif
                        }
                        return llvm::Error::success();
                    })
                    .create());

    HRLogDebug("JIT Engine setup completed successfully.");
}

KotlinObjectFile HotReloadImpl::ParseKotlinObjectFile(const llvm::MemoryBufferRef& Buf) const {
    KotlinObjectFile kotlinObjectFile{};
    auto graphOrErr = llvm::jitlink::createLinkGraphFromObject(Buf, jit_->getExecutionSession().getSymbolStringPool());

    if (!graphOrErr) {
        HRLogError("Failed to create link graph from object file: %s", llvm::toString(graphOrErr.takeError()).c_str());
        return kotlinObjectFile;
    }

    const auto& linkGraph = *graphOrErr;
    for (auto& section : linkGraph->sections()) {
        for (const auto* symbol : section.symbols()) {
            if (!symbol->hasName()) continue;
            if (symbol->getScope() == llvm::jitlink::Scope::Local) continue;

            const auto symbolName = *symbol->getName();

            if (symbolName.starts_with(kKotlinFunPrefix)) {
                kotlinObjectFile.functions.push_back(symbolName.str());
            } else if (symbolName.starts_with(kKotlinClassPrefix)) {
                kotlinObjectFile.classes.push_back(symbolName.str());
            }
        }
    }

    return kotlinObjectFile;
}

std::unique_ptr<llvm::MemoryBuffer> HotReloadImpl::ReadObjectFileFromPath(const std::string_view objectPath) {
    auto objBufferOrError = llvm::MemoryBuffer::getFile(objectPath);
    if (!objBufferOrError) {
        HRLogError("Failed to read object file %s: %s", std::string(objectPath).c_str(), objBufferOrError.getError().message().c_str());
        return nullptr;
    }
    return std::move(*objBufferOrError);
}

#if KONAN_OBJC_INTEROP

/// Initialize ObjC unique prefix from JIT'd code.
/// This must happen before any ObjC class creation (CreateKotlinObjCClass).
/// The compiler generates Kotlin_ObjCInterop_uniquePrefix in bootstrap.o,
/// but the runtime has a weak symbol initialized to nullptr.
void HotReloadImpl::InitializeObjCUniquePrefixFromJIT(llvm::orc::JITDylib& BootstrapJD) const {
    auto& ES = jit_->getExecutionSession();

    HRLogDebug("Looking up ObjC unique prefix symbol from JIT (BootstrapJD)...");

    // Symbol name with underscore prefix for macOS
    const auto prefixName = ES.intern("_Kotlin_ObjCInterop_uniquePrefix");

    // Look up from BootstrapJD where bootstrap.o defines the real value.
    // MainJD's DynamicLibrarySearchGenerator would find the host's weak null symbol instead.
    // Use MatchAllSymbols because these data symbols are "private external" (N_PEXT) in the
    // object file, which maps to Scope::Hidden in JITLink. The default MatchExportedSymbolsOnly
    // would skip them.
    auto bootstrapSearchOrder = llvm::orc::makeJITDylibSearchOrder({&BootstrapJD}, llvm::orc::JITDylibLookupFlags::MatchAllSymbols);
    auto prefixResult = ES.lookup(bootstrapSearchOrder, prefixName);

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
void HotReloadImpl::InitializeObjCAdaptersFromJIT(llvm::orc::JITDylib& BootstrapJD) const {
    auto& ES = jit_->getExecutionSession();

    HRLogDebug("Looking up ObjC type adapter symbols from JIT (BootstrapJD)...");

    // Symbol names for ObjC type adapters (with underscore prefix for macOS)
    const auto classAdaptersName = ES.intern("_Kotlin_ObjCExport_sortedClassAdapters");
    const auto classAdaptersNumName = ES.intern("_Kotlin_ObjCExport_sortedClassAdaptersNum");
    const auto protocolAdaptersName = ES.intern("_Kotlin_ObjCExport_sortedProtocolAdapters");
    const auto protocolAdaptersNumName = ES.intern("_Kotlin_ObjCExport_sortedProtocolAdaptersNum");

    // Look up from BootstrapJD where bootstrap.o defines the real adapter arrays.
    // MainJD's DynamicLibrarySearchGenerator would find the host's weak null symbols instead.
    // Use MatchAllSymbols because these data symbols are "private external" (N_PEXT) in the
    // object file, which maps to Scope::Hidden in JITLink. The default MatchExportedSymbolsOnly
    // would skip them.
    auto bootstrapSearchOrder = llvm::orc::makeJITDylibSearchOrder({&BootstrapJD}, llvm::orc::JITDylibLookupFlags::MatchAllSymbols);

    // Look up class adapters
    const ObjCTypeAdapter** classAdapters = nullptr;
    int classAdaptersNum = 0;

    auto classAdaptersResult = ES.lookup(bootstrapSearchOrder, classAdaptersName);
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

    auto classAdaptersNumResult = ES.lookup(bootstrapSearchOrder, classAdaptersNumName);
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

    auto protocolAdaptersResult = ES.lookup(bootstrapSearchOrder, protocolAdaptersName);
    if (!protocolAdaptersResult) {
        llvm::consumeError(protocolAdaptersResult.takeError());
        HRLogDebug("_Kotlin_ObjCExport_sortedProtocolAdapters not found");
    } else {
        protocolAdapters = *protocolAdaptersResult->getAddress().toPtr<const ObjCTypeAdapter***>();
        HRLogDebug(
                "Found _Kotlin_ObjCExport_sortedProtocolAdapters @ 0x%llx -> 0x%llx", protocolAdaptersResult->getAddress().getValue(),
                (unsigned long long)protocolAdapters);
    }

    auto protocolAdaptersNumResult = ES.lookup(bootstrapSearchOrder, protocolAdaptersNumName);
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

llvm::Error HotReloadImpl::CreateRedirectableStubs(const std::vector<std::string>& functionSymbols) {
    auto& es = jit_->getExecutionSession();
    const auto stubsJD = getStubsJD();

    llvm::orc::SymbolMap initialDests{};

    for (auto& symbolName : functionSymbols) {
        auto Interned = es.intern(symbolName);
        if (redirectableSymbols_.contains(Interned)) continue;

        // Let's create a new stub symbol with the same name as the original one
        initialDests[Interned] =
                llvm::orc::ExecutorSymbolDef(llvm::orc::ExecutorAddr(), llvm::JITSymbolFlags::Callable | llvm::JITSymbolFlags::Exported);
        redirectableSymbols_.insert(Interned);
    }

    if (initialDests.empty()) return llvm::Error::success();

    return rsm_->createRedirectableSymbols(stubsJD->getDefaultResourceTracker(), std::move(initialDests));
}

llvm::Error HotReloadImpl::RedirectStubsToImpl(llvm::orc::JITDylib& JD, const std::unordered_set<std::string>& symbolNames) const {
    const auto stubsJD = getStubsJD();
    auto& es = jit_->getExecutionSession();

    // Force materialization of the impl MU by looking up original names.
    // This triggers the KotlinSymbolExternalizer plugin, which creates $knhr impl
    // symbols via MR.defineMaterializing() and replaces originals with reexports
    // from StubsJD (also materializing the stubs, creating $__stub_ptr symbols).
    {
        llvm::orc::SymbolLookupSet triggerSymbols;
        for (auto& SymName : symbolNames) {
            triggerSymbols.add(es.intern(SymName));
        }
        auto triggerResult = es.lookup(llvm::orc::makeJITDylibSearchOrder({&JD}), std::move(triggerSymbols));
        if (!triggerResult) {
            return triggerResult.takeError();
        }
    }

    // Now look up $knhr impl addresses (created by the plugin during previous materialization)
    llvm::orc::SymbolMap dests{};
    for (auto& symName : symbolNames) {
        auto implName = symName + kImplSymbolSuffix;
        auto symOrErr = es.lookup(llvm::orc::makeJITDylibSearchOrder({&JD}), es.intern(implName));
        if (!symOrErr) {
            HRLogWarning("Failed to lookup impl symbol %s", implName.c_str());
            llvm::consumeError(symOrErr.takeError());
            continue;
        }
        dests[es.intern(symName)] = symOrErr.get();
    }

    if (dests.empty()) return llvm::Error::success();

    return rsm_->redirect(*stubsJD, dests);
}

void HotReloadImpl::LoadCacheDependencies(const std::string_view bootstrapFilePath, llvm::orc::JITDylib& targetJD) const {
    auto& oll = jit_->getObjLinkingLayer();
    const auto manifestPath = std::string(bootstrapFilePath) + ".cache-deps";

    std::fstream manifestFile(manifestPath, std::ios::in);
    if (!manifestFile) {
        HRLogError("Cannot read the manifest file at path: %s", manifestPath.c_str());
        return;
    }

    std::string currentPath{};
    while (std::getline(manifestFile, currentPath)) {
        HRLogDebug("Adding static library to the cache generator: %s", currentPath.c_str());
        auto gen = ExitOnErr(llvm::orc::StaticLibraryDefinitionGenerator::Load(oll, currentPath.c_str()));
        targetJD.addGenerator(std::move(gen));
    }

    manifestFile.close();
}
KonanStartFunc HotReloadImpl::LoadBootstrapFile(const std::string_view bootstrapFilePath) {
    HRLogDebug("Loading bootstrap file: %s", bootstrapFilePath.data());

    auto& es = jit_->getExecutionSession();
    auto& mainJD = jit_->getMainJITDylib();

    auto& bootstrapJD = ExitOnErr(es.createJITDylib(kBootstrapJdName));
    jds_.push_back(&bootstrapJD);

    bootstrapJD.addToLinkOrder(*getStubsJD());
    bootstrapJD.addToLinkOrder(mainJD);

    bootstrapJD.addGenerator(std::make_unique<WeakSymbolFallbackGenerator>());

    if (auto hostGen = MachOHostDataSymbolGenerator::CreateForCurrentProcess()) {
        bootstrapJD.addGenerator(std::move(*hostGen));
    } else {
        HRLogWarning("Failed to create host symbol generator: %s", llvm::toString(hostGen.takeError()).c_str());
    }

    auto dlsymGen = ExitOnErr(llvm::orc::DynamicLibrarySearchGenerator::GetForCurrentProcess(jit_->getDataLayout().getGlobalPrefix()));
    bootstrapJD.addGenerator(std::move(dlsymGen));

    const auto objMemoryBuff = ReadObjectFileFromPath(bootstrapFilePath);
    if (!objMemoryBuff) {
        HRLogError("Failed to parse bootstrap object file, the application cannot be started.");
        return nullptr;
    }

    auto bootstrapObject = ParseKotlinObjectFile(*objMemoryBuff);

    // Create redirectable stubs in StubsJD (addr=0, will be redirected after materialization)
    ExitOnErr(CreateRedirectableStubs(bootstrapObject.functions));

    std::unordered_set<std::string> functionSymbols{};
    std::unordered_set<std::string> classSymbols{};

    for (auto functionSym : bootstrapObject.functions) functionSymbols.insert(functionSym);
    for (auto classSym : bootstrapObject.functions) classSymbols.insert(classSym);

    latestLoadedFunctionSymbols_ = functionSymbols;
    latestLoadedClassSymbols_ = classSymbols;

    HRLogDebug(
            "Found: %zu function, and %zu class symbol in bootstrap.", latestLoadedFunctionSymbols_.size(),
            latestLoadedClassSymbols_.size());

    // Generators only fire for direct lookups on their owning JITDylib.
    LoadCacheDependencies(bootstrapFilePath, bootstrapJD);

    // Resolve ORC runtime TLS symbol from platform JITDylib and re-export on BootstrapJD.
    // Loading the archive directly causes cascading internal dependency failures.
    // TODO: is this really necessary?
    if (auto platformJD = jit_->getPlatformJITDylib()) {
        auto tlvSym = es.lookup(
                llvm::orc::makeJITDylibSearchOrder({platformJD.get()}, llvm::orc::JITDylibLookupFlags::MatchAllSymbols),
                es.intern("___orc_rt_macho_tlv_get_addr"));
        if (tlvSym) {
            ExitOnErr(bootstrapJD.define(llvm::orc::absoluteSymbols({{es.intern("___orc_rt_macho_tlv_get_addr"), *tlvSym}})));
        } else {
            llvm::consumeError(tlvSym.takeError());
            HRLogWarning("___orc_rt_macho_tlv_get_addr not found in platform JITDylib");
        }
    }

    ExitOnErr(jit_->addObjectFile(
            bootstrapJD, llvm::MemoryBuffer::getMemBufferCopy(objMemoryBuff->getBuffer(), objMemoryBuff->getBufferIdentifier())));

    ExitOnErr(RedirectStubsToImpl(bootstrapJD, latestLoadedFunctionSymbols_));

    // Trigger materialization via Konan_start lookup
    const auto KonanStartSymbol = ExitOnErr(es.lookup(llvm::orc::makeJITDylibSearchOrder(&bootstrapJD), es.intern(kKonanStartSymbol)));

    // Call _Konan_constructors from JIT'd code to register InitNodes from all cache libraries.
    // The runtime was initialized before bootstrap.o was loaded, so the InitNode list was empty
    // during the first ALLOC/COMMIT/INIT cycle. Calling constructors now populates the list.
    auto constructorsResult = es.lookup(
            llvm::orc::makeJITDylibSearchOrder({&bootstrapJD}, llvm::orc::JITDylibLookupFlags::MatchAllSymbols),
            es.intern(kKonanConstructorsSymbol));
    if (constructorsResult) {
        auto constructorsFn = constructorsResult->getAddress().toPtr<void (*)()>();
        HRLogDebug("Calling _Konan_constructors @ 0x%llx to register InitNodes", constructorsResult->getAddress().getValue());
        constructorsFn();
    } else {
        llvm::consumeError(constructorsResult.takeError());
        HRLogWarning("_Konan_constructors not found in bootstrap");
    }

    // Re-run ALLOC/COMMIT/INIT for the newly registered InitNodes.
    // Already-initialized entries are safely skipped via per-file state checks.
    ReinitializeGlobalVariablesAndTLS();
    HRLogDebug("Global variables and TLS re-initialized after bootstrap loading");

#if KONAN_OBJC_INTEROP
    // Initialize ObjC unique prefix from JIT'd code.
    // This must happen before any ObjC class creation (CreateKotlinObjCClass).
    // Must look up from BootstrapJD — MainJD's DLG would find host's weak null symbols.
    InitializeObjCUniquePrefixFromJIT(bootstrapJD);

    // Initialize ObjC type adapters from JIT'd symbols.
    // This must happen after the bootstrap is loaded but before user code runs,
    // because ObjC interop calls need the type adapters to be set up.
    InitializeObjCAdaptersFromJIT(bootstrapJD);
#endif

    return KonanStartSymbol.toPtr<KonanStartFunc>();
}

bool HotReloadImpl::LoadObjectsAndUpdateFunctionStubs(const std::vector<std::string>& objectPaths) {
    // Setup a new JITDylib first, where object files will added at the second pass
    auto& es = jit_->getExecutionSession();
    const auto reloadIter = jds_.size() - 1;
    const auto reloadDylibName = kReloadJdName + std::to_string(reloadIter);

    // Use createBareJITDylib to skip MachOPlatform::setupJITDylib (see bootstrap comment).
    auto& reloadedJD = es.createBareJITDylib(reloadDylibName);
    for (auto it = jds_.rbegin(); it != jds_.rend(); ++it) {
        reloadedJD.addToLinkOrder(**it);
    }

    jds_.push_back(&reloadedJD);

    // ReloadJD links to StubsJD so _kfun: refs resolve via stubs
    reloadedJD.addToLinkOrder(*getStubsJD());
    // Link reload dylib to main dylib for runtime symbol access
    reloadedJD.addToLinkOrder(jit_->getMainJITDylib());

    std::unordered_set<std::string> loadedFunctionSymbols;
    std::unordered_set<std::string> loadedClassSymbols;

    std::vector<llvm::MemoryBuffer*> objMemoryBuffs;

    for (const auto& objectPath : objectPaths) {
        HRLogDebug("Loading object file for reload: %s", std::string(objectPath).c_str());

        const auto objMemoryBuff = ReadObjectFileFromPath(objectPath);
        if (!objMemoryBuff) return false;

        auto [kotlinFunctions, kotlinClasses] = ParseKotlinObjectFile(*objMemoryBuff);

        HRLogDebug("Found: %zu function symbols and %zu classes symbols.", kotlinFunctions.size(), kotlinClasses.size());

        // Create stubs for any NEW functions not seen before
        if (auto err = CreateRedirectableStubs(kotlinFunctions)) {
            HRLogError("Failed to create redirectable stubs: %s", llvm::toString(std::move(err)).c_str());
            return false;
        }

        for (const auto& funSym : kotlinFunctions) {
            loadedFunctionSymbols.insert(funSym);
        }

        for (const auto& classSym : kotlinClasses) {
            loadedClassSymbols.insert(classSym);
        }

        // Add object file _kfun:foo will be renamed to _kfun:foo$impl by KotlinSymbolExternalizer
        if (auto err = jit_->addObjectFile(
                    reloadedJD, llvm::MemoryBuffer::getMemBufferCopy(objMemoryBuff->getBuffer(), objMemoryBuff->getBufferIdentifier()))) {
            HRLogError("Failed to add object file: %s", llvm::toString(std::move(err)).c_str());
            return false;
        }
    }

    latestLoadedFunctionSymbols_ = loadedFunctionSymbols;
    latestLoadedClassSymbols_ = loadedClassSymbols;

    // Redirect stubs to point to new implementations
    if (auto err = RedirectStubsToImpl(reloadedJD, latestLoadedFunctionSymbols_)) {
        ExitOnErr(handleErrors(std::move(err), [](const llvm::jitlink::JITLinkError& error) {
            HRLogError("Failed to redirect stubs to implementation after reload: %s", error.getErrorMessage().c_str());
        }));
        return false;
    }

    HRLogDebug("Redirected %zu stubs to reload implementations", latestLoadedFunctionSymbols_.size());
    return true;
}

void HotReloadImpl::Reload(const std::vector<std::string>& objectPaths) noexcept {
    CalledFromNativeGuard guard(true);
    HRLogInfo("Start: Hot-Reload");
    HRLogDebug("Switching to K/N state and requesting threads suspension...");
    auto mainGCLock = mm::GlobalData::Instance().gc().gcLock(); // Serialize global State

    auto* currentThreadData = mm::ThreadRegistry::Instance().CurrentThreadData();
    currentThreadData->suspensionData().requestThreadsSuspension("Hot-Reload");

    // From this point on, the threads could be suspended. Remember to invoke `ResumeThreads`.
    // TODO: maybe it is a good idea to use RAII here.

    CallsCheckerIgnoreGuard allowWait;

    statsCollector_.RegisterStart(static_cast<int64_t>(utility::getCurrentEpoch()));
    // statsCollector_.RegisterLoadedObject(objectPaths);

    /// 1. Load new objects in JIT engine
    const bool objectsLoaded = LoadObjectsAndUpdateFunctionStubs(objectPaths);
    if (!objectsLoaded) {
        statsCollector_.RegisterEnd(static_cast<int64_t>(utility::getCurrentEpoch()));
        statsCollector_.RegisterSuccessful(false);
        mm::ResumeThreads();
        return;
    }

    try {
        mm::WaitForThreadsSuspension();
        Perform(*currentThreadData);
        HRLogInfo("End: Hot-Reload");
        statsCollector_.RegisterEnd(static_cast<int64_t>(utility::getCurrentEpoch()));
        statsCollector_.RegisterSuccessful(true);
        HRLogDebug("Resuming threads...");
        mm::ResumeThreads();
        HRLogDebug("Threads resumed successfully. Invoking success handlers (if any)...");
        Kotlin_native_internal_HotReload_invokeSuccessCallback();
    } catch (const std::exception& e) {
        HRLogError("Hot-reload failed with exception: %s", e.what());
        statsCollector_.RegisterEnd(static_cast<int64_t>(utility::getCurrentEpoch()));
        statsCollector_.RegisterSuccessful(false);
        mm::ResumeThreads();
    }
}

void HotReloadImpl::ReloadClassesAndInstances(mm::ThreadData& currentThreadData) const {
    std::unordered_map<std::string, TypeInfo*> newClasses{};

    auto& es = jit_->getExecutionSession();
    auto* latestJD = jds_.back();

    const std::vector previousJDs(jds_.rbegin() + 1, jds_.rend());
    const auto previousSearchOrder = llvm::orc::makeJITDylibSearchOrder(previousJDs);

    // Look up new class TypeInfos from the latest reload JD
    for (auto& className : latestLoadedClassSymbols_) {
        auto typeInfoOrErr = es.lookup(llvm::orc::makeJITDylibSearchOrder(latestJD), es.intern(className));
        if (!typeInfoOrErr) {
            HRLogWarning("Cannot find new TypeInfo for class: %s", className.c_str());
            llvm::consumeError(typeInfoOrErr.takeError());
            continue;
        }
        newClasses[className] = typeInfoOrErr->getAddress().toPtr<TypeInfo*>();
    }

    for (const auto& [typeInfoName, newTypeInfo] : newClasses) {
        // Look up old TypeInfo from previous JDs (most recent first)
        auto oldTypeInfoOrErr = es.lookup(previousSearchOrder, es.intern(typeInfoName));
        if (!oldTypeInfoOrErr) {
            HRLogWarning("Cannot find old TypeInfo for class: %s", typeInfoName.c_str());
            llvm::consumeError(oldTypeInfoOrErr.takeError());
            continue;
        }

        const auto oldTypeInfo = oldTypeInfoOrErr->getAddress().toPtr<const TypeInfo*>();
        auto objectsToReload = FindObjectsToReload(oldTypeInfo);

        // For each new redefined TypeInfo, reload the instances
        for (const auto& obj : objectsToReload) {
            const auto newInstance = PerformStateTransfer(currentThreadData, obj, newTypeInfo);
            if (newInstance != nullptr) {
                UpdateShadowStackReferences(obj, newInstance);
                UpdateHeapReferences(obj, newInstance);
            }
        }
    }
}

void HotReloadImpl::Perform(mm::ThreadData& currentThreadData) const {
    ReloadClassesAndInstances(currentThreadData);
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
        const int32_t newFieldOffset = newFieldOffsets[i];

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
    const std::string oldTypeFqName = oldTypeInfo->fqName();

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