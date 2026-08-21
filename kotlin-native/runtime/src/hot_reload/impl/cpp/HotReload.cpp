/**
 * Copyright 2010-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include <atomic>
#include <chrono>
#include <cstdio>
#include <cstdlib>
#include <string>
#include <unordered_map>
#include <unordered_set>
#include <utility>

#include "HotReload.hpp"
#include "HotReloadInternal.hpp"
#include "HotReloadUtility.hpp"
#include "Memory.h"
#include "Runtime.h"
#include "StateTransfer.hpp"
#include "mm/GlobalData.hpp"
#include "mm/ThreadData.hpp"
#include "mm/ThreadRegistry.hpp"

#if KONAN_OBJC_INTEROP
#include "ObjCExportInit.h"
extern "C" __attribute__((weak)) const char* Kotlin_ObjCInterop_uniquePrefix;
#endif

using namespace kotlin;
using kotlin::hot::HotReloadImpl;

extern "C" RUNTIME_WEAK const uint8_t* bootStartManifest;

namespace {

constexpr auto kOrcRuntimePathEnv = "KONAN_ORC_RUNTIME_PATH";
constexpr auto kDefaultOrcRuntimeRelativePath =
        "/.konan/dependencies/llvm-21-aarch64-macos-dev-93/lib/clang/21/lib/darwin/liborc_rt_osx.a";

ManuallyScoped<HotReloadImpl> globalDataInstance{};
std::atomic_bool bootstrapRequested{false};

std::string DefaultOrcRuntimePath() {
    if (const char* home = std::getenv("HOME")) {
        return std::string{home} + kDefaultOrcRuntimeRelativePath;
    }
    return kDefaultOrcRuntimeRelativePath + 1;
}

void EnsureBootstrapped() {
    if (bootstrapRequested.exchange(true)) {
        return;
    }

    Kotlin_initRuntimeIfNeeded();
    HotReloadImpl::Instance().LoadBootstrap(bootStartManifest);
}

} // namespace

extern "C" {

void Kotlin_native_internal_HotReload_perform(ObjHeader*, const ObjHeader*) {
    AssertThreadState(ThreadState::kRunnable);
    HRLogInfo("Kotlin's side HotReload::perform is not implemented yet");
}

void* KNHR_LoadObjCStubAddress(const char* name) {
    CalledFromNativeGuard guard(/* reentrant = */ true);
    EnsureBootstrapped();

    const auto addressOrError = HotReloadImpl::Instance().GetSymbolsOrchestrator().lookupInBootstrap(name);
    if (!addressOrError) {
        HRLogError("Cannot resolve Objective-C stub address for %s: %s", name, addressOrError.error().c_str());
        return nullptr;
    }
    return *addressOrError;
}

} // extern "C"

void hot::HotReload::InitModule() noexcept {
    globalDataInstance.construct();
}

hot::HotReload& hot::HotReload::Instance() noexcept {
    return *globalDataInstance;
}

void hot::HotReload::LoadBootstrap(const uint8_t* manifestData) {
    HotReloadImpl::Instance().LoadBootstrap(manifestData);
}

hot::KonanStartFn hot::HotReload::LookupForKonanStart() const {
    return HotReloadImpl::Instance().LookupForKonanStart();
}

HotReloadImpl& HotReloadImpl::Instance() noexcept {
    return reinterpret_cast<HotReloadImpl&>(*globalDataInstance);
}

HotReloadImpl::HotReloadImpl() {
    HRLogInfo("Initializing Hot-Reload module");
    SetupORC();
    StartServer();
}

void HotReloadImpl::SetupORC() {
    const char* configuredPath = std::getenv(kOrcRuntimePathEnv);
    const auto orcRuntimePath = configuredPath ? std::string{configuredPath} : DefaultOrcRuntimePath();
    HRLogDebug("Loading ORC runtime from %s", orcRuntimePath.c_str());

    llvm::kaldo::Config config{
            .OrcRuntimePath = orcRuntimePath,
#if KONAN_OBJC_INTEROP
            .KonanObjCInteropEnabled = true,
#else
            .KonanObjCInteropEnabled = false,
#endif
    };

    auto orchestrator = llvm::kaldo::SymbolsOrchestrator::create(config);
    if (!orchestrator) {
        HRLogError("Failed to create Kaldo SymbolsOrchestrator: %s", orchestrator.error().c_str());
        return;
    }
    symbolsOrch_ = std::move(*orchestrator);
}

void HotReloadImpl::StartServer() {
    if (!symbolsOrch_) {
        HRLogWarning("Hot-Reload server disabled because Kaldo initialization failed");
        return;
    }
    if (!server_.Start()) {
        HRLogError("Failed to start Hot-Reload server on port %d", HotReloadServer::GetDefaultPort());
        return;
    }
    server_.Run([this](ReloadRequest request) { Reload(std::move(request)); });
}

void HotReloadImpl::LoadBootstrap(const uint8_t* manifestData) {
    std::lock_guard lock(bootstrapMutex_);
    if (!symbolsOrch_ || symbolsOrch_->itDidBoot()) {
        return;
    }

    auto manifest = llvm::kaldo::manifest::parseKaldoManifestFromBytes(manifestData);
    if (!manifest) {
        HRLogError("Invalid or missing embedded Kaldo bootstrap manifest");
        return;
    }

    auto runtimeInit = [] {
        ReinitializeGlobalVariablesAndTLS();
        HRLogDebug("Global variables and TLS re-initialized after bootstrap loading");
    };

#if KONAN_OBJC_INTEROP
    auto uniquePrefixInit = [](const llvm::kaldo::objc::ObjCUniquePrefixOutput& output) {
        Kotlin_ObjCInterop_uniquePrefix = output.UniquePrefixAddress;
    };
    auto bindAdaptersToTypeInfos = [](const llvm::kaldo::objc::ObjCExportAdaptersOutput& output) {
        Kotlin_ObjCExport_bindTypeAdaptersToTypeInfos(
                output.ClassAdapters, output.ClassAdaptersNum, output.ProtocolAdapters, output.ProtocolAdaptersNum);
    };
    auto bootstrap = symbolsOrch_->loadBootstrap(*manifest, runtimeInit, uniquePrefixInit, bindAdaptersToTypeInfos);
#else
    auto bootstrap = symbolsOrch_->loadBootstrap(*manifest, runtimeInit);
#endif

    if (!bootstrap) {
        std::fprintf(stderr, "error: failed to load embedded bootstrap manifest: %s\n", bootstrap.error().c_str());
        HRLogError("Failed to load embedded bootstrap manifest: %s", bootstrap.error().c_str());
    }
}

hot::KonanStartFn HotReloadImpl::LookupForKonanStart() const {
    if (!symbolsOrch_) {
        return nullptr;
    }
    auto symbol = symbolsOrch_->lookupForKonanStart();
    if (!symbol) {
        HRLogError("Could not find Konan_start: %s", symbol.error().c_str());
        return nullptr;
    }
    return reinterpret_cast<KonanStartFn>(*symbol);
}

hot::StatsCollector& HotReloadImpl::GetStatsCollector() noexcept {
    return statsCollector_;
}

void HotReloadImpl::Perform(mm::ThreadData& currentThreadData) const {
    ReloadClassesAndInstances(currentThreadData);
}

void HotReloadImpl::Reload(const ReloadRequest& request) noexcept {
    CalledFromNativeGuard guard(/* reentrant = */ true);
    auto gcLock = mm::GlobalData::Instance().gc().gcLock();

    ReloadTimings timings{};
    const auto reloadStart = std::chrono::steady_clock::now();
    auto* currentThreadData = mm::ThreadRegistry::Instance().CurrentThreadData();
    const auto suspensionStart = std::chrono::steady_clock::now();
    currentThreadData->suspensionData().requestThreadsSuspension("Hot-Reload");

    bool success = false;
    statsCollector_.RegisterStart(utility::currentEpoch());
    statsCollector_.RegisterLoadedObjects(request.objectPaths);

    llvm::kaldo::LoadTimings kaldoTimings{};
    auto reloaded = symbolsOrch_->reloadObjects(request.objectPaths, kaldoTimings);
    if (!reloaded) {
        HRLogError("Failed to reload objects: %s", reloaded.error().c_str());
    } else {
        timings.loadNs = static_cast<int64_t>(kaldoTimings.LoadNs);
        timings.stubsNs = static_cast<int64_t>(kaldoTimings.StubsNs);
        timings.redirectNs = static_cast<int64_t>(kaldoTimings.RedirectNs);
        try {
            mm::WaitForThreadsSuspension();
            timings.stwWaitNs = nanosecondsSince(suspensionStart);
            {
                ScopeTimer<> stateTimer{"state-transfer", timings.stateTransferNs};
                Perform(*currentThreadData);
            }
            success = true;
        } catch (const std::exception& exception) {
            HRLogError("Hot-Reload failed: %s", exception.what());
        }
    }

    mm::ResumeThreads();
    timings.totalNs = nanosecondsSince(reloadStart);
    PublishStats(request, timings, success);

    if (success) {
        Kotlin_native_internal_HotReload_invokeReloadSuccessHandler();
    }
}

void HotReloadImpl::PublishStats(
        const ReloadRequest& request, const ReloadTimings& timings, bool success) noexcept {
    statsCollector_.RegisterEnd(utility::currentEpoch());
    statsCollector_.RegisterSuccessful(success);
    statsCollector_.RegisterLoadNs(timings.loadNs);
    statsCollector_.RegisterStubsNs(timings.stubsNs);
    statsCollector_.RegisterRedirectNs(timings.redirectNs);
    statsCollector_.RegisterStateTransferNs(timings.stateTransferNs);
    statsCollector_.RegisterRequestParseNs(request.timings.parseNs);
    statsCollector_.RegisterStwWaitNs(timings.stwWaitNs);

    HRLogInfo(
            "swap_stats {\"total_ns\":%lld,\"request_parse_ns\":%lld,\"stw_wait_ns\":%lld,"
            "\"load_ns\":%lld,\"stubs_ns\":%lld,\"redirect_ns\":%lld,\"state_ns\":%lld,\"success\":%s}",
            static_cast<long long>(timings.totalNs), static_cast<long long>(request.timings.parseNs),
            static_cast<long long>(timings.stwWaitNs), static_cast<long long>(timings.loadNs),
            static_cast<long long>(timings.stubsNs), static_cast<long long>(timings.redirectNs),
            static_cast<long long>(timings.stateTransferNs), success ? "true" : "false");
}

void HotReloadImpl::ReloadClassesAndInstances(mm::ThreadData& currentThreadData) const {
    std::unordered_set<const TypeInfo*> typeFilter;
    std::unordered_map<const TypeInfo*, const TypeInfo*> typesToReload;
    std::unordered_map<ObjHeader*, ObjHeader*> remap;

    struct PairHash {
        size_t operator()(const std::pair<const TypeInfo*, const TypeInfo*>& pair) const noexcept {
            return std::hash<const TypeInfo*>{}(pair.first) ^ (std::hash<const TypeInfo*>{}(pair.second) << 1);
        }
    };
    std::unordered_map<std::pair<const TypeInfo*, const TypeInfo*>, state::StateTransferMap, PairHash>
            transferMapCache;

    auto patches = symbolsOrch_->collectTypeInfoPatchesFromLatestUnit();
    if (!patches) {
        HRLogWarning("Could not collect TypeInfo patches: %s", patches.error().c_str());
        return;
    }

    for (const auto& patch : *patches) {
        const auto* oldTypeInfo = static_cast<const TypeInfo*>(patch.OldTypeInfo);
        const auto* newTypeInfo = static_cast<const TypeInfo*>(patch.NewTypeInfo);
        if (oldTypeInfo == newTypeInfo) {
            continue;
        }
        typesToReload[oldTypeInfo] = newTypeInfo;
        typeFilter.insert(oldTypeInfo);
    }

    auto [instancesByType, liveObjects] = state::WalkHeapAndBucket(typeFilter);
    for (auto& [oldTypeInfo, instances] : instancesByType) {
        const auto* newTypeInfo = typesToReload.at(oldTypeInfo);
        const auto key = std::make_pair(oldTypeInfo, newTypeInfo);
        auto [transfer, inserted] = transferMapCache.try_emplace(key);
        if (inserted) {
            transfer->second = state::CreateStateTransferMap(oldTypeInfo, newTypeInfo);
        }

        for (auto* oldObject : instances) {
            ObjHeader* newObject = currentThreadData.allocator().allocateObject(newTypeInfo);
            if (newObject == nullptr) {
                HRLogError("Failed to allocate replacement object for %s", newTypeInfo->fqName().c_str());
                continue;
            }
            state::PerformStateTransfer(oldObject, newObject, transfer->second);
            remap[oldObject] = newObject;
        }
    }

    for (const auto& [_, newObject] : remap) {
        liveObjects.push_back(newObject);
    }
    state::RewriteAllReferences(remap, liveObjects);
}
