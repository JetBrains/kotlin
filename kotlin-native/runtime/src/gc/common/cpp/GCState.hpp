/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <condition_variable>
#include <mutex>
#include <atomic>
#include <optional>

#include "CallsChecker.hpp"
#include "CollectionScope.hpp"
#include "KAssert.h"
#include "Utils.hpp"

class GCStateHolder {
public:
    struct ScheduledCollection {
        int64_t epoch;
        kotlin::gc::CollectionScope scope;
    };

    // Schedules a collection of the given scope. For non-generational collectors the scope is
    // always Full (the default), so existing callers are unaffected. If a collection is already
    // pending, an Eden request coalesces into it, while a Full request upgrades a pending Eden
    // to Full (a Full collection subsumes an Eden one).
    int64_t schedule(kotlin::gc::CollectionScope scope = kotlin::gc::CollectionScope::Full) {
        // Should be a fast function. `mutex_` is never taken for long.
        kotlin::CallsCheckerIgnoreGuard callsCheckerIgnoreGuard;

        std::unique_lock lock(mutex_);
        if (*scheduledEpoch <= *startedEpoch) {
            scheduledEpoch.set(lock, *startedEpoch + 1);
            scheduledScope_ = scope;
        } else if (scope == kotlin::gc::CollectionScope::Full) {
            scheduledScope_ = kotlin::gc::CollectionScope::Full;
        }
        return *scheduledEpoch;
    }

    void shutdown() {
        std::unique_lock lock(mutex_);
        shutdownFlag_ = true;
        startedEpoch.notify();
        finishedEpoch.notify();
        scheduledEpoch.notify();
        finalizedEpoch.notify();
    }

    void finish(int64_t epoch) { finishedEpoch.set(epoch); }

    void finalized(int64_t epoch) { finalizedEpoch.set(epoch); }

    void waitEpochFinished(int64_t epoch) {
        finishedEpoch.wait([this, epoch] { return *finishedEpoch >= epoch || shutdownFlag_; });
    }

    void waitEpochFinalized(int64_t epoch) {
        finalizedEpoch.wait([this, epoch] { return *finalizedEpoch >= epoch || shutdownFlag_; });
    }

    std::optional<ScheduledCollection> waitScheduled() {
        std::unique_lock lock(mutex_);
        scheduledEpoch.wait(lock, [this] { return *scheduledEpoch > *finishedEpoch || shutdownFlag_; });
        if (shutdownFlag_) return std::nullopt;
        auto result = ScheduledCollection{*scheduledEpoch, scheduledScope_};
        // Claim the scheduled epoch and its scope atomically. After this point a new schedule()
        // request must target the next epoch instead of racing to upgrade a scope the GC thread has
        // already read.
        startedEpoch.set(lock, result.epoch);
        return result;
    }

private:
    template <typename T>
    struct ValueWithCondVar : kotlin::Pinned {
        explicit ValueWithCondVar(T initializer, std::mutex& mutex) noexcept : value_(initializer), mutex_(mutex){};

        const T& operator*() const { return value_; }

        void set(T newValue) {
            std::unique_lock lock(mutex_);
            set(lock, newValue);
        }

        void set(std::unique_lock<std::mutex>& lock, T newValue) {
            RuntimeAssert(lock.owns_lock() && lock.mutex() == &mutex_, "Required the mutex to be locked");
            value_ = newValue;
            cond_.notify_all();
        }

        void notify() { cond_.notify_all(); }

        template <class Predicate>
        const T& wait(Predicate stop_waiting) {
            std::unique_lock lock(mutex_);
            cond_.wait(lock, stop_waiting);
            return value_;
        }

        template <class Predicate>
        const T& wait(std::unique_lock<std::mutex>& lock, Predicate stop_waiting) {
            RuntimeAssert(lock.owns_lock() && lock.mutex() == &mutex_, "Required the mutex to be locked");
            cond_.wait(lock, stop_waiting);
            return value_;
        }

    private:
        T value_;
        std::mutex& mutex_;
        std::condition_variable cond_;
    };

    std::mutex mutex_;
    // Use a separate conditional variable for each counter to mitigate a winpthreads bug (see KT-50948 for details).
    ValueWithCondVar<int64_t> startedEpoch{0, mutex_};
    ValueWithCondVar<int64_t> finishedEpoch{0, mutex_};
    ValueWithCondVar<int64_t> scheduledEpoch{0, mutex_};
    ValueWithCondVar<int64_t> finalizedEpoch{0, mutex_};
    // Scope of the currently scheduled collection. Guarded by mutex_. Only meaningful for
    // generational collectors; always Full otherwise.
    kotlin::gc::CollectionScope scheduledScope_ = kotlin::gc::CollectionScope::Full;
    bool shutdownFlag_ = false;
};
