/*
 * Copyright 2010-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <vector>

#include "GC.hpp"
#include "GCScheduler.hpp"
#include "GCState.hpp"
#include "ParallelMark.hpp"
#include "Utils.hpp"
#include "concurrent/UtilityThread.hpp"

namespace kotlin::gc::internal {

class MainGCThread : private MoveOnly {
public:
    MainGCThread(
            GCStateHolder& state,
            mark::ParallelMark& markDispatcher,
            alloc::Allocator& allocator,
            gcScheduler::GCScheduler& gcScheduler) noexcept;

private:
    void body() noexcept;
    void PerformFullGC(int64_t epoch) noexcept;

    GCStateHolder& state_;
    mark::ParallelMark& markDispatcher_;
    alloc::Allocator& allocator_;
    gcScheduler::GCScheduler& gcScheduler_;
    UtilityThread thread_;
};

class AuxiliaryGCThreads : private MoveOnly {
public:
    AuxiliaryGCThreads(mark::ParallelMark& markDispatcher, size_t count) noexcept;

    void stopThreads() noexcept;
    void startThreads(size_t count) noexcept;

private:
    void body() noexcept;

    mark::ParallelMark& markDispatcher_;
    std::vector<UtilityThread> threads_;
};

} // namespace kotlin::gc::internal
