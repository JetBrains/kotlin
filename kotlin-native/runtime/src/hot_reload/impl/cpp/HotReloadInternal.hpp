/**
 * Copyright 2010-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef HOTRELOAD_INTERNAL_HPP
#define HOTRELOAD_INTERNAL_HPP

#include <mutex>
#include <string>

#include <llvm/Kaldo/Kaldo.hpp>

#include "hot_reload/common/cpp/HotReload.hpp"

#include "HotReloadServer.hpp"
#include "HotReloadStats.hpp"

namespace kotlin::mm {
class ThreadData;
} // namespace kotlin::mm

extern "C" {
    void Kotlin_native_internal_HotReload_invokeReloadSuccessHandler();
}

namespace kotlin::hot {

/// Full implementation of HotReload with LLVM dependencies.
class HotReloadImpl : public HotReload {
public:
    static HotReloadImpl& Instance() noexcept;

    HotReloadImpl();

    void Reload(const ReloadRequest& request) noexcept;

    KonanStartFn LookupForKonanStart() const;

    StatsCollector& GetStatsCollector() noexcept;

    void LoadBootstrap(const uint8_t* manifestData);

    const llvm::kaldo::SymbolsOrchestrator& GetSymbolsOrchestrator() const {
        assert(symbolsOrch_ != nullptr && "SymbolsOrchestrator is null");
        return *symbolsOrch_;
    }

private:
    void SetupORC();

    void StartServer();

    void PublishStats(const ReloadRequest& request, const ReloadTimings& timings, bool success) noexcept;

    void ReloadClassesAndInstances(mm::ThreadData& currentThreadData) const;

    void Perform(mm::ThreadData& currentThreadData) const;

    HotReloadServer server_{};
    std::unique_ptr<llvm::kaldo::SymbolsOrchestrator> symbolsOrch_;
    std::mutex bootstrapMutex_;

    StatsCollector statsCollector_{};
};

} // namespace kotlin::hot

#endif // HOTRELOAD_INTERNAL_HPP
