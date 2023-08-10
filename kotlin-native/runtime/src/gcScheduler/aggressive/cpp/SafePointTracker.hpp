/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <cstddef>
#include <unordered_set>

#include "CallsChecker.hpp"
#include "KAssert.h"
#include "Logging.hpp"
#include "Mutex.hpp"
#include "StackTrace.hpp"

namespace kotlin::gcScheduler::internal {

template <size_t SafePointStackSize = 16>
class SafePointTracker {
public:
    using SafePointID = kotlin::StackTrace<SafePointStackSize>;

    explicit SafePointTracker(size_t maxSize = 100000) : maxSize_(maxSize) {}

    /** Returns whether the GC must be triggered on the current safe point or not. */
    NO_INLINE bool registerCurrentSafePoint(size_t skipFrames) noexcept {
        CallsCheckerIgnoreGuard guard;
        auto currentSP = SafePointID::current(skipFrames + 1);

        std::unique_lock lock(mutex_);

        // TODO: Consider replacing this naive cleaning with an LRU cache.
        if (metSafePoints_.size() >= maxSize()) {
            RuntimeLogDebug({kTagGC}, "Clear safe point tracker set since it exceeded maximal size");
            metSafePoints_.clear();
        }

        bool inserted = metSafePoints_.insert(currentSP).second;
        return inserted;
    }

    size_t maxSize() { return maxSize_; }

    size_t size() { return metSafePoints_.size(); }

private:
    size_t maxSize_;

    // TODO: Consider replacing mutex + global set with thread local sets sychronized on STW.
    SpinLock<MutexThreadStateHandling::kIgnore> mutex_;
    std::unordered_set<SafePointID> metSafePoints_;
};

} // namespace kotlin::gcScheduler::internal
