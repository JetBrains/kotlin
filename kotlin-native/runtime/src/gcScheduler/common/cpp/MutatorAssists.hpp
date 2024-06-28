/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <atomic>
#include <condition_variable>
#include <cstdint>
#include <mutex>
#include <optional>

#include "SafePoint.hpp"
#include "ThreadRegistry.hpp"
#include "Utils.hpp"

#if KONAN_WINDOWS
#include "ConditionVariable.hpp"
#else
#include <condition_variable>
#endif

namespace kotlin::gcScheduler::internal {

/**
 * Coordinating mutator assistance to the GC.
 *
 * Currently assisting only by pausing threads. So assisting means not creating
 * more work for the GC thread.
 *
 * Threads (both mutators and any other) can call `requestAssists(epoch)` for any
 * `epoch` at any time.
 *
 * If the current GC epoch is greater than `epoch`, the mutators should ignore
 * the request to assist.
 *
 * Otherwise the mutators must wait in the native state
 * until the GC thread calls `completeEpoch(epoch)` for epoch >= `epoch`.
 *
 * The GC thread shall call `completeEpoch(epoch)` once it is done with the epoch,
 * and it shall wait for all mutators assisting `epoch` (or lower) to continue.
 */
class MutatorAssists : private Pinned {
public:
    using Epoch = int64_t;

    class ThreadData : private Pinned {
    public:
        ThreadData(MutatorAssists& owner, mm::ThreadData& thread) noexcept : owner_(owner), thread_(thread) {}

        void safePoint() noexcept;

        std::pair<Epoch, bool> startedWaiting(std::memory_order ordering) const noexcept {
            auto value = startedWaiting_.load(ordering);
            auto waitingEpoch = value / 2;
            bool isWaiting = value % 2 == 0;
            return {waitingEpoch, isWaiting};
        }

    private:
        friend class MutatorAssists;

        bool completedEpoch(Epoch epoch) const noexcept;

        MutatorAssists& owner_;
        mm::ThreadData& thread_;
        // Contains epoch * 2. The lower bit is 1, if completed waiting.
        std::atomic<int64_t> startedWaiting_ = 1;
    };

    // Request all `kRunnable` mutators to start assisting GC for epoch `epoch`.
    // Can be called multiple times per `epoch`, and `epoch` may point to the past.
    void requestAssists(Epoch epoch) noexcept;

    // Should be called by GC, when it completed epoch `epoch`.
    // The call blocks waiting for all assisting mutators to finish assisting `epoch`.
    // `f` is a map from `mm::ThreadData&` to `MutatorAssists::ThreadData&`.
    // Can only be called once per `epoch`, and `epoch` must be increasing
    // by exactly 1 every time.
    template <typename F>
    void completeEpoch(Epoch epoch, F&& f) noexcept {
        markEpochCompleted(epoch);
        mm::ThreadRegistry::Instance().waitAllThreads(
                [f = std::forward<F>(f), epoch](mm::ThreadData& threadData) noexcept { return f(threadData).completedEpoch(epoch); });
    }

    Epoch assistsRequested(std::memory_order order) noexcept { return assistsEpoch_.load(order); }

private:
    void markEpochCompleted(Epoch epoch) noexcept;

    std::atomic<Epoch> assistsEpoch_ = 0;
    std::atomic<Epoch> completedEpoch_ = 0;
    std::optional<mm::SafePointActivator> safePointActivator_;
    std::mutex m_;
#if KONAN_WINDOWS
    // winpthreads being weird. Using this implementation of condvar means that assisting mutators will spin for the entire duration of the
    // GC. This is fine: reaching assisting state should be rare and this state exists to ward of "memory leaks", and additionally assists
    // can be disabled.
    ConditionVariableSpin cv_;
#else
    std::condition_variable cv_;
#endif
};

} // namespace kotlin::gcScheduler::internal
