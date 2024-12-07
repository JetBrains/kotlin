/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "GCSchedulerImpl.hpp"

#include "GlobalData.hpp"
#include "Memory.h"
#include "Logging.hpp"
#include "Porting.h"

using namespace kotlin;

gcScheduler::GCScheduler::ThreadData::Impl::Impl(GCScheduler& scheduler, mm::ThreadData& thread) noexcept :
    scheduler_(scheduler.impl().impl()), mutatorAssists_(scheduler_.mutatorAssists(), thread) {}

gcScheduler::GCScheduler::ThreadData::ThreadData(gcScheduler::GCScheduler& scheduler, mm::ThreadData& thread) noexcept :
    impl_(std::make_unique<Impl>(scheduler, thread)) {}

gcScheduler::GCScheduler::ThreadData::~ThreadData() = default;

gcScheduler::GCScheduler::Impl::Impl(gcScheduler::GCSchedulerConfig& config) noexcept :
    impl_(config, []() noexcept {
        return mm::GlobalData::Instance().gc().Schedule();
    }) {}

gcScheduler::GCScheduler::GCScheduler() noexcept : impl_(std::make_unique<Impl>(config_)) {}

gcScheduler::GCScheduler::~GCScheduler() = default;

PERFORMANCE_INLINE void gcScheduler::GCScheduler::ThreadData::safePoint() noexcept {
    impl().mutatorAssists().safePoint();
}

void gcScheduler::GCScheduler::schedule() noexcept {
    impl().impl().scheduleManually();
}

void gcScheduler::GCScheduler::scheduleAndWaitFinished() noexcept {
    auto epoch = impl().impl().scheduleManually();
    NativeOrUnregisteredThreadGuard guard(/* reentrant = */ true);
    mm::GlobalData::Instance().gc().WaitFinished(epoch);
}

void gcScheduler::GCScheduler::scheduleAndWaitFinalized() noexcept {
    auto epoch = impl().impl().scheduleManually();
    NativeOrUnregisteredThreadGuard guard(/* reentrant = */ true);
    mm::GlobalData::Instance().gc().WaitFinalizers(epoch);
}

PERFORMANCE_INLINE void gcScheduler::GCScheduler::setAllocatedBytes(size_t bytes) noexcept {
    impl().impl().setAllocatedBytes(bytes);
}

PERFORMANCE_INLINE void gcScheduler::GCScheduler::onGCStart() noexcept {
    impl().impl().onGCStart();
}

PERFORMANCE_INLINE void gcScheduler::GCScheduler::onGCFinish(int64_t epoch, size_t aliveBytes) noexcept {
    impl().impl().onGCFinish(epoch, aliveBytes);
}
