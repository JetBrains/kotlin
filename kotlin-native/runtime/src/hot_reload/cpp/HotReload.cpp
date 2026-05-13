#include <deque>
#include <unordered_map>
#include <unordered_set>
#include <queue>
#include <chrono>
#include <fstream>
#include <utility>
#include <unistd.h>
#include <dlfcn.h>

#include "Memory.h"
#include "GlobalData.hpp"
#include "ThreadRegistry.hpp"
#include "ThreadData.hpp"

#include "HotReload.hpp"
#include "HotReloadInternal.hpp"
#include "StateTransfer.hpp"

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
#include "objc_support/ObjectPtr.hpp"
#include <CoreFoundation/CoreFoundation.h>

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

// Flag indicating if the running program has initialized its bootstrap object.
// This flag is used mainly for framework initialization.
static std::atomic_bool hasBeenBootstrapped{false};

static llvm::ExitOnError ExitOnErr;

/// Forward declarations for GC functions.
namespace kotlin::gc {
void stopTheWorld(GCHandle gcHandle, const char* reason) noexcept;
void resumeTheWorld(GCHandle gcHandle) noexcept;
} // namespace kotlin::gc

// region Extern "C" functions

static std::string findBootstrapPathInFramework() {

    using namespace objc_support;

    Dl_info info{};
    if (!dladdr(reinterpret_cast<void*>(&KNHR_LoadObjCStubAddress), &info) || !info.dli_fname)
        return "";

    const object_ptr<CFStringRef> binaryPath{CFStringCreateWithCString(nullptr, info.dli_fname, kCFStringEncodingUTF8)};
    if (!binaryPath) return "";

    const object_ptr<CFURLRef> binaryURL{CFURLCreateWithFileSystemPath(nullptr, binaryPath.get(), kCFURLPOSIXPathStyle, false)};
    if (!binaryURL) return "";

    const object_ptr<CFURLRef> frameworkURL{CFURLCreateCopyDeletingLastPathComponent(nullptr, binaryURL.get())};
    if (!frameworkURL) return "";

    const object_ptr<CFBundleRef> bundle{CFBundleCreate(nullptr, frameworkURL.get())};
    if (!bundle) return "";

    const object_ptr<CFURLRef> resourceURL{CFBundleCopyResourceURL(bundle.get(), CFSTR("bootstrap"), CFSTR("o"), nullptr)};
    if (!resourceURL) return "";

    char path[PATH_MAX];
    if (!CFURLGetFileSystemRepresentation(resourceURL.get(), true, reinterpret_cast<UInt8*>(path), PATH_MAX)) {
        return "";
    }

    return std::string{path};
}

static void ensureBootstrapped() {

    bool expected = false;
    if (!hasBeenBootstrapped.compare_exchange_strong(expected, true)) {
        return;
    }

    Kotlin_initRuntimeIfNeeded();

    // Now, we need to understand if the runtime is running from a framework.
    // In that case, the boostrap object is located within the loaded
    // framework at runtime.
    if (compiler::hotReloadInFramework()) {
        auto bootstrapObjectPath = findBootstrapPathInFramework();
        if (bootstrapObjectPath.empty()) {
            HRLogError("Could not find bootstrap object path in framework. Application will fail to launch.");
            return;
        }
        HotReloadImpl::Instance().LoadBootstrapFile(bootstrapObjectPath);
    }

    // If that's not the case, it means the application has been compiled in `program` mode.
    // Meaning that the boostrap object will be loaded by the hot_launcher component.
    // We do not do anything in this case.
}

extern "C" {

void Kotlin_native_internal_HotReload_perform(ObjHeader* obj, const ObjHeader* dylibPathStr) {
    AssertThreadState(ThreadState::kRunnable);
    // TODO: fix implementation. This is not a priority function atm.
    // const auto dylibPath = kotlin::to_string<KStringConversionMode::UNCHECKED>(dylibPathStr);
    // HotReloadImpl::Instance().Reload(dylibPath);
}

void* KNHR_LoadObjCStubAddress(const char* name) {
    ensureBootstrapped();
    void* address = HotReloadImpl::Instance().LookupForSymbolInBootstrap(name);
    HRLogInfo("Loading ObjC stub address for symbol: %s. value: %p", name, address);
    return address;
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

void* HotReloadImpl::LookupForSymbolInBootstrap(const char* symbolName) const {
    auto BootstrapJD = jit_->getJITDylibByName(kBootstrapJdName);
    auto sym = jit_->lookup(*BootstrapJD, symbolName);
    if (!sym) {
        llvm::consumeError(sym.takeError());
        HRLogError("failed to lookup symbol '%s' in BootstrapJD", symbolName);
        return nullptr;
    }
    return sym->toPtr<void*>();
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

                        auto psg = ExitOnErr(llvm::orc::DynamicLibrarySearchGenerator::GetForCurrentProcess(J.getDataLayout().getGlobalPrefix()));

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

    // Set the runtime's weak symbol to the JIT value
    Kotlin_ObjCInterop_uniquePrefix = jitPrefix;
    HRLogDebug("ObjC unique prefix initialized: \"%s\"", Kotlin_ObjCInterop_uniquePrefix);
}

struct ObjCAdapterTable {
    const ObjCTypeAdapter** entries = nullptr;
    int count = 0;
};

template <typename T>
std::optional<T> LookupOptional(llvm::orc::ExecutionSession& es, const llvm::orc::JITDylibSearchOrder& order, const llvm::orc::SymbolStringPtr& name, const char* logLabel) {
    auto result = es.lookup(order, name);
    if (!result) {
        llvm::consumeError(result.takeError());
        HRLogWarning("%s not found in JIT'd code", logLabel);
        return std::nullopt;
    }
    return *result->getAddress().toPtr<T*>();
}

ObjCAdapterTable LookupObjCExportAdapters(llvm::orc::ExecutionSession& ES, llvm::orc::JITDylib& BootstrapJD, const char* adaptersSym, const char* adaptersNumSym) {
    const ObjCTypeAdapter** adapters = nullptr;
    int numAdapters = 0;

    const auto bootstrapSearchOrder = llvm::orc::makeJITDylibSearchOrder({&BootstrapJD}, llvm::orc::JITDylibLookupFlags::MatchAllSymbols);

    const auto adaptersName = ES.intern(adaptersSym);
    const auto adaptersNumName = ES.intern(adaptersNumSym);

    const auto adaptersResult = LookupOptional<const ObjCTypeAdapter**>(ES, bootstrapSearchOrder, adaptersName, adaptersSym);
    const auto adaptersNumResult = LookupOptional<int>(ES, bootstrapSearchOrder, adaptersNumName, adaptersNumSym);

    if (adaptersResult.has_value()) {
        adapters = adaptersResult.value();
    }

    if (adaptersNumResult.has_value()) {
        numAdapters = adaptersNumResult.value();
    }

    return {adapters, numAdapters};
}

/// Initialize ObjC type adapters from symbols defined in JIT'd code.
/// This looks up the adapter arrays from the JIT and passes them to the runtime.
void HotReloadImpl::InitializeObjCAdaptersFromJIT(llvm::orc::JITDylib& BootstrapJD) const {
    /// We do NOT check _Kotlin_ObjCExport_initTypeAdapters flag here because:
    /// - That flag is a weak symbol with default value (false) in host.kexe
    /// - The actual adapter arrays are in bootstrap.o (JIT-loaded code)
    /// - DynamicLibrarySearchGenerator finds the host's weak default first
    /// So we directly look for the adapter arrays themselves.

    HRLogDebug("Looking up ObjC type adapter symbols from JIT (BootstrapJD)...");

    auto& ES = jit_->getExecutionSession();
    auto clazzAdapters = LookupObjCExportAdapters(ES, BootstrapJD, "_Kotlin_ObjCExport_sortedClassAdapters", "_Kotlin_ObjCExport_sortedClassAdaptersNum");
    auto protoAdapters = LookupObjCExportAdapters(ES, BootstrapJD, "_Kotlin_ObjCExport_sortedProtocolAdapters", "_Kotlin_ObjCExport_sortedProtocolAdaptersNum");

    HRLogDebug("Found %d class adapters and %d protocol adapters from JIT", clazzAdapters.count, protoAdapters.count);

    // Initialize adapters in the runtime
    if (clazzAdapters.count > 0 || protoAdapters.count > 0) {
        Kotlin_ObjCExport_initializeTypeAdaptersWithPointers(clazzAdapters.entries, clazzAdapters.count, protoAdapters.entries  , protoAdapters.count);
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
        auto triggerResult = es.lookup(
                llvm::orc::makeJITDylibSearchOrder({&JD}, llvm::orc::JITDylibLookupFlags::MatchAllSymbols),
                std::move(triggerSymbols));
        if (!triggerResult) {
            return triggerResult.takeError();
        }
    }

    // Now look up $knhr impl addresses (created by the plugin during previous materialization)
    llvm::orc::SymbolMap dests{};
    for (auto& symName : symbolNames) {
        auto implName = symName + kImplSymbolSuffix;
        auto symOrErr = es.lookup(
                llvm::orc::makeJITDylibSearchOrder({&JD}, llvm::orc::JITDylibLookupFlags::MatchAllSymbols),
                es.intern(implName));
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

KonanStartFunc HotReloadImpl::LookupForKonanStart() const {
    auto& es = jit_->getExecutionSession();

    auto bootstrapJD = es.getJITDylibByName(kBootstrapJdName);
    if (!bootstrapJD) {
        HRLogError("Bootstrap JITDylib not found");
        return nullptr;
    }

    // Trigger materialization via Konan_start lookup
    const auto KonanStartSymbol = ExitOnErr(es.lookup(llvm::orc::makeJITDylibSearchOrder(bootstrapJD), es.intern(kKonanStartSymbol)));
    return KonanStartSymbol.toPtr<KonanStartFunc>();
}

void HotReloadImpl::LoadBootstrapFile(const std::string_view bootstrapFilePath) {
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
        return;
    }

    const auto bootstrapObject = KotlinObjectFile::Create(*objMemoryBuff, jit_->getExecutionSession().getSymbolStringPool());

    // Create redirectable stubs in StubsJD (addr=0, will be redirected after materialization)
    ExitOnErr(CreateRedirectableStubs(bootstrapObject.functions));

    std::unordered_set<std::string> functionSymbols{};
    std::unordered_set<std::string> classSymbols{};

    for (auto functionSym : bootstrapObject.functions) functionSymbols.insert(functionSym);
    for (auto classSym : bootstrapObject.classes) classSymbols.insert(classSym);

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

    // TODO: remove this: used to trigger materialization
    // auto _ignored = es.lookup(llvm::orc::makeJITDylibSearchOrder(&bootstrapJD), es.intern(kKonanStartSymbol));

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

    reloadedJD.addToLinkOrder(*getStubsJD());
    reloadedJD.addToLinkOrder(jit_->getMainJITDylib());

    // Add generators directly to reloadJD so host symbols are resolvable
    // during materialization (generators on linked JITDylibs may not fire).
    if (auto hostGen = MachOHostDataSymbolGenerator::CreateForCurrentProcess()) {
        reloadedJD.addGenerator(std::move(*hostGen));
    } else {
        llvm::consumeError(hostGen.takeError());
    }
    if (auto dlsymGen = llvm::orc::DynamicLibrarySearchGenerator::GetForCurrentProcess(jit_->getDataLayout().getGlobalPrefix())) {
        reloadedJD.addGenerator(std::move(*dlsymGen));
    } else {
        llvm::consumeError(dlsymGen.takeError());
    }

    std::unordered_set<std::string> loadedFunctionSymbols;
    std::unordered_set<std::string> loadedClassSymbols;

    std::vector<llvm::MemoryBuffer*> objMemoryBuffs;

    for (const auto& objectPath : objectPaths) {
        HRLogDebug("Loading object file for reload: %s", std::string(objectPath).c_str());

        const auto objMemoryBuff = ReadObjectFileFromPath(objectPath);
        if (!objMemoryBuff) return false;

        auto [kotlinFunctions, kotlinClasses] = KotlinObjectFile::Create(*objMemoryBuff, jit_->getExecutionSession().getSymbolStringPool());

        HRLogDebug("Found: %zu function symbols and %zu classes symbols.", kotlinFunctions.size(), kotlinClasses.size());

        // Create stubs for any NEW functions not seen before
        {
            auto stubsT0 = std::chrono::steady_clock::now();
            auto err = CreateRedirectableStubs(kotlinFunctions);
            stubsNs += std::chrono::duration_cast<std::chrono::nanoseconds>(
                    std::chrono::steady_clock::now() - stubsT0).count();
            if (err) {
                HRLogError("Failed to create redirectable stubs: %s", llvm::toString(std::move(err)).c_str());
                return false;
            }
        }

        for (const auto& funSym : kotlinFunctions) {
            loadedFunctionSymbols.insert(funSym);
        }

        for (const auto& classSym : kotlinClasses) {
            loadedClassSymbols.insert(classSym);
        }

        // Add object file _kfun:foo will be renamed to _kfun:foo$impl by KotlinSymbolExternalizer
        {
            auto loadT0 = std::chrono::steady_clock::now();
            auto err = jit_->addObjectFile(reloadedJD, llvm::MemoryBuffer::getMemBufferCopy(objMemoryBuff->getBuffer(), objMemoryBuff->getBufferIdentifier()));
            loadNs += std::chrono::duration_cast<std::chrono::nanoseconds>(
                    std::chrono::steady_clock::now() - loadT0).count();
            if (err) {
                HRLogError("Failed to add object file: %s", llvm::toString(std::move(err)).c_str());
                return false;
            }
        }
    }

    latestLoadedFunctionSymbols_ = loadedFunctionSymbols;
    latestLoadedClassSymbols_ = loadedClassSymbols;

    // Redirect stubs to point to new implementations
    {
        auto redirectT0 = std::chrono::steady_clock::now();
        auto err = RedirectStubsToImpl(reloadedJD, latestLoadedFunctionSymbols_);
        redirectNs = std::chrono::duration_cast<std::chrono::nanoseconds>(
                std::chrono::steady_clock::now() - redirectT0).count();
        if (err) {
            HRLogError("RedirectStubsToImpl failed: %s\n", llvm::toString(std::move(err)).c_str());
            return false;
        }
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

    std::unordered_map<std::string, TypeInfo*> newClasses{};
    llvm::DenseMap<std::pair<const TypeInfo*, const TypeInfo*>, state::StateTransferMap> stateTransferMaps;

    const std::vector previousJDs(jds_.rbegin() + 1, jds_.rend());
    const auto previousSearchOrder = llvm::orc::makeJITDylibSearchOrder(previousJDs);

    // Look up new class TypeInfos from the latest reload JD
    for (auto& className : latestLoadedClassSymbols_) {
        auto typeInfoOrErr = es.lookup(llvm::orc::makeJITDylibSearchOrder(latestJD, llvm::orc::JITDylibLookupFlags::MatchAllSymbols), es.intern(className));
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

        // HRLogDebug("Old TypeInfo: %s@%p | New TypeInfo: %s@%p", oldTypeInfo->fqName().c_str(), oldTypeInfo, typeInfoName.c_str(), newTypeInfo);
        assert(oldTypeInfo != newTypeInfo && "The new type info should be different than the previous one.");

        auto [it, inserted] = stateTransferMaps.try_emplace(std::make_pair(oldTypeInfo, newTypeInfo));
        if (inserted) {
            it->second = state::CreateStateTransferMap(oldTypeInfo, newTypeInfo);
        }

        const auto& transferMap = it->second;
        auto objectsToReload = state::FindObjectsToReload(oldTypeInfo);

        // For each new redefined TypeInfo, reload the instances
        for (const auto& oldObject : objectsToReload) {
            ObjHeader* newObject = currentThreadData.allocator().allocateObject(newTypeInfo);
            if (newObject == nullptr) {
                HRLogError("allocation of new object of type %s failed!", newTypeInfo->fqName().c_str());
                continue;
            }
            state::PerformStateTransfer(oldObject, newObject, transferMap);
            state::RewriteAllReferencesTo(oldObject, newObject);
        }
    }
}

void HotReloadImpl::Perform(mm::ThreadData& currentThreadData) const {
    ReloadClassesAndInstances(currentThreadData);
}

// endregion