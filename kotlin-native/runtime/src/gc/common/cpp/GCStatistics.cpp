/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

#include "GCStatistics.hpp"
#include "Mutex.hpp"
#include "Porting.h"

#include "Types.h"
#include "Logging.hpp"
#include "ThreadData.hpp"
#include "std_support/Optional.hpp"
#include <cinttypes>
#include <mutex>

using namespace kotlin;

extern "C" {
void Kotlin_Internal_GC_GCInfoBuilder_setEpoch(KRef thiz, KLong value);
void Kotlin_Internal_GC_GCInfoBuilder_setStartTime(KRef thiz, KLong value);
void Kotlin_Internal_GC_GCInfoBuilder_setEndTime(KRef thiz, KLong value);
void Kotlin_Internal_GC_GCInfoBuilder_setPauseStartTime(KRef thiz, KLong value);
void Kotlin_Internal_GC_GCInfoBuilder_setPauseEndTime(KRef thiz, KLong value);
void Kotlin_Internal_GC_GCInfoBuilder_setPostGcCleanupTime(KRef thiz, KLong value);
void Kotlin_Internal_GC_GCInfoBuilder_setRootSet(KRef thiz,
                                                 KLong threadLocalReferences, KLong stackReferences,
                                                 KLong globalReferences, KLong stableReferences);
void Kotlin_Internal_GC_GCInfoBuilder_setMemoryUsageBefore(KRef thiz, KNativePtr name, KLong sizeBytes);
void Kotlin_Internal_GC_GCInfoBuilder_setMemoryUsageAfter(KRef thiz, KNativePtr name, KLong sizeBytes);
}

namespace {

struct MemoryUsage {
    uint64_t sizeBytes;
};

struct MemoryUsageMap {
    std::optional<MemoryUsage> heap;

    void build(KRef builder, void (*add)(KRef, KNativePtr, KLong)) {
        if (heap) {
            add(builder, const_cast<KNativePtr>(static_cast<const void*>("heap")),
                static_cast<KLong>(heap->sizeBytes));
        }
    }
};

struct RootSetStatistics {
    KLong threadLocalReferences;
    KLong stackReferences;
    KLong globalReferences;
    KLong stableReferences;
    KLong total() const { return threadLocalReferences + stableReferences + globalReferences + stableReferences; }
};

struct GCInfo {
    std::optional<uint64_t> epoch;
    std::optional<KLong> startTime; // time since process start
    std::optional<KLong> endTime;
    std::optional<KLong> pauseStartTime;
    std::optional<KLong> pauseEndTime;
    std::optional<KLong> finalizersDoneTime;
    std::optional<RootSetStatistics> rootSet;
    std::optional<kotlin::gc::MarkStats> markStats;
    std::optional<kotlin::gc::SweepStats> sweepStats;
    std::optional<kotlin::gc::SweepStats> sweepExtraObjectsStats;
    MemoryUsageMap memoryUsageBefore;
    MemoryUsageMap memoryUsageAfter;

    void build(KRef builder) {
        if (!epoch) return;
        Kotlin_Internal_GC_GCInfoBuilder_setEpoch(builder, static_cast<KLong>(*epoch));
        if (startTime) Kotlin_Internal_GC_GCInfoBuilder_setStartTime(builder, *startTime);
        if (endTime) Kotlin_Internal_GC_GCInfoBuilder_setEndTime(builder, *endTime);
        if (pauseStartTime) Kotlin_Internal_GC_GCInfoBuilder_setPauseStartTime(builder, *pauseStartTime);
        if (pauseEndTime) Kotlin_Internal_GC_GCInfoBuilder_setPauseEndTime(builder, *pauseEndTime);
        if (finalizersDoneTime) Kotlin_Internal_GC_GCInfoBuilder_setPostGcCleanupTime(builder, *finalizersDoneTime);
        if (rootSet)
            Kotlin_Internal_GC_GCInfoBuilder_setRootSet(
                    builder, rootSet->threadLocalReferences, rootSet->stackReferences, rootSet->globalReferences,
                    rootSet->stableReferences);
        memoryUsageBefore.build(builder, Kotlin_Internal_GC_GCInfoBuilder_setMemoryUsageBefore);
        memoryUsageAfter.build(builder, Kotlin_Internal_GC_GCInfoBuilder_setMemoryUsageAfter);
    }
};

GCInfo last;
GCInfo current;
// This lock can be got by thread in runnable state making parallel mark, so
kotlin::SpinLock<kotlin::MutexThreadStateHandling::kIgnore> lock;

GCInfo* statByEpoch(uint64_t epoch) {
    if (current.epoch == epoch) return &current;
    if (last.epoch == epoch) return &last;
    return nullptr;
}

MemoryUsage currentHeapUsage() noexcept {
    return MemoryUsage {
        mm::GlobalData::Instance().gc().GetTotalHeapObjectsSizeBytes(),
    };
}

} // namespace

extern "C" void Kotlin_Internal_GC_GCInfoBuilder_Fill(KRef builder, int id) {
    GCInfo copy;
    {
        kotlin::ThreadStateGuard stateGuard(kotlin::ThreadState::kNative);
        std::lock_guard guard(lock);
        if (id == 0) {
            copy = last;
        } else if (id == 1) {
            copy = current;
        } else {
            return;
        }
    }
    copy.build(builder);
}

namespace kotlin::gc {
GCHandle GCHandle::create(uint64_t epoch) {
    std::lock_guard guard(lock);
    RuntimeAssert(statByEpoch(epoch) == nullptr, "Starting epoch, which already existed");
    if (current.epoch) {
        last = current;
        current = {};
        GCLogWarning(epoch, "Starting new GC epoch, while previous is not finished");
    }
    current.epoch = static_cast<KLong>(epoch);
    current.startTime = static_cast<KLong>(konan::getTimeNanos());
    if (last.endTime) {
        GCLogInfo(epoch, "Started. Time since last GC %" PRIu64 " microseconds.", *current.startTime - *last.endTime);
    } else {
        GCLogInfo(epoch, "Started.");
    }
    current.memoryUsageBefore.heap = currentHeapUsage();
    return getByEpoch(epoch);
}
GCHandle GCHandle::createFakeForTests() { return getByEpoch(std::numeric_limits<uint64_t>::max()); }
GCHandle GCHandle::getByEpoch(uint64_t epoch) {
    return GCHandle{epoch};
}

// static
std::optional<gc::GCHandle> gc::GCHandle::currentEpoch() noexcept {
    std::lock_guard guard(lock);
    if (auto epoch = current.epoch) {
        return GCHandle::getByEpoch(*epoch);
    }
    return std::nullopt;
}

void GCHandle::ClearForTests() {
    std::lock_guard guard(lock);
    current = {};
    last = {};
}
void GCHandle::finished() {
    std::lock_guard guard(lock);
    if (auto* stat = statByEpoch(epoch_)) {
        stat->endTime = static_cast<KLong>(konan::getTimeNanos());
        stat->memoryUsageAfter.heap = currentHeapUsage();
        if (stat->rootSet) {
            GCLogInfo(
                    epoch_,
                    "Root set: "
                    "%" PRIu64 " thread local references, "
                    "%" PRIu64 " stack references, "
                    "%" PRIu64 " global references, "
                    "%" PRIu64 " stable references. "
                    "In total %" PRIu64 " roots.",
                    stat->rootSet->threadLocalReferences, stat->rootSet->stackReferences, stat->rootSet->globalReferences,
                    stat->rootSet->stableReferences, stat->rootSet->total());
        }
        if (stat->markStats) {
            GCLogInfo(
                    epoch_,
                    "Mark: %" PRIu64 " objects.",
                    stat->markStats->markedCount);
        }
        if (stat->sweepExtraObjectsStats) {
            GCLogInfo(
                    epoch_,
                    "Sweep extra objects: swept %" PRIu64 " objects, kept %" PRIu64 " objects",
                    stat->sweepExtraObjectsStats->sweptCount, stat->sweepExtraObjectsStats->keptCount);
        }
        if (stat->sweepStats) {
            GCLogInfo(
                    epoch_,
                    "Sweep: swept %" PRIu64 " objects, kept %" PRIu64 " objects",
                    stat->sweepStats->sweptCount, stat->sweepStats->keptCount);
        }
        if (stat->memoryUsageBefore.heap && stat->memoryUsageAfter.heap) {
            GCLogInfo(
                    epoch_,
                    "Heap memory usage: before %" PRIu64 " bytes, after %" PRIu64 " bytes",
                    stat->memoryUsageBefore.heap->sizeBytes,
                    stat->memoryUsageAfter.heap->sizeBytes);
        }
        if (stat->pauseStartTime && stat->pauseEndTime) {
            auto time = (*stat->pauseEndTime - *stat->pauseStartTime) / 1000;
            GCLogInfo(
                    epoch_,
                    "Mutators pause time: %" PRIu64 " microseconds.",
                    time);
        }
        if (stat->startTime) {
            auto time = (*current.endTime - *current.startTime) / 1000;
            GCLogInfo(epoch_, "Finished. Total GC epoch time is %" PRId64" microseconds.", time);
        }

        if (stat == &current) {
            last = current;
            current = {};
        }
    }
}
void GCHandle::suspensionRequested() {
    std::lock_guard guard(lock);
    GCLogDebug(epoch_, "Requested thread suspension");
    if (auto* stat = statByEpoch(epoch_)) {
        stat->pauseStartTime = static_cast<KLong>(konan::getTimeNanos());
    }
}
void GCHandle::threadsAreSuspended() {
    std::lock_guard guard(lock);
    if (auto* stat = statByEpoch(epoch_)) {
        if (stat->pauseStartTime) {
            auto time = (konan::getTimeNanos() - *stat->pauseStartTime) / 1000;
            GCLogDebug(epoch_, "Suspended all threads in %" PRIu64 " microseconds", time);
            return;
        }
    }
}
void GCHandle::threadsAreResumed() {
    std::lock_guard guard(lock);
    if (auto* stat = statByEpoch(epoch_)) {
        stat->pauseEndTime = static_cast<KLong>(konan::getTimeNanos());
        if (stat->pauseStartTime) {
            auto time = (*stat->pauseEndTime - *stat->pauseStartTime) / 1000;
            GCLogDebug(epoch_, "Resume all threads. Total pause time is %" PRId64 " microseconds.", time);
            return;
        }
    }
}
void GCHandle::finalizersDone() {
    std::lock_guard guard(lock);
    if (auto* stat = statByEpoch(epoch_)) {
        stat->finalizersDoneTime = static_cast<KLong>(konan::getTimeNanos());
        if (stat->endTime) {
            auto time = (*stat->finalizersDoneTime - *stat->endTime) / 1000;
            GCLogInfo(epoch_, "Finalization is done in %" PRId64 " microseconds after epoch end.", time);
            return;
        } else {
            GCLogInfo(epoch_, "Finalization is done.");
        }
    }
}
void GCHandle::finalizersScheduled(uint64_t finalizersCount) {
    GCLogDebug(epoch_, "Finalization is scheduled for %" PRIu64 " objects.", finalizersCount);
}
void GCHandle::threadRootSetCollected(mm::ThreadData &threadData, uint64_t threadLocalReferences, uint64_t stackReferences) {
    std::lock_guard guard(lock);
    if (auto* stat = statByEpoch(epoch_)) {
        if (!stat->rootSet) {
            stat->rootSet = RootSetStatistics{0, 0, 0, 0};
        }
        stat->rootSet->stackReferences += static_cast<KLong>(stackReferences);
        stat->rootSet->threadLocalReferences += static_cast<KLong>(threadLocalReferences);
    }
}
void GCHandle::globalRootSetCollected(uint64_t globalReferences, uint64_t stableReferences) {
    std::lock_guard guard(lock);
    if (auto* stat = statByEpoch(epoch_)) {
        if (!stat->rootSet) {
            stat->rootSet = RootSetStatistics{0, 0, 0, 0};
        }
        stat->rootSet->globalReferences += static_cast<KLong>(globalReferences);
        stat->rootSet->stableReferences += static_cast<KLong>(stableReferences);
    }
}

void GCHandle::marked(kotlin::gc::MarkStats stats) {
    std::lock_guard guard(lock);
    if (auto* stat = statByEpoch(epoch_)) {
        if (!stat->markStats) {
            stat->markStats = MarkStats{};
        }
        stat->markStats->markedSizeBytes += stats.markedSizeBytes;
        stat->markStats->markedCount += stats.markedCount;
    }
}

MarkStats GCHandle::getMarked() {
    std::lock_guard guard(lock);
    if (auto* stat = statByEpoch(epoch_)) {
        if (stat->markStats) {
            return *stat->markStats;
        }
    }
    return MarkStats{};
}

void GCHandle::swept(gc::SweepStats stats) noexcept {
    std::lock_guard guard(lock);
    if (auto* stat = statByEpoch(epoch_)) {
        if (!stat->sweepStats) {
            stat->sweepStats = gc::SweepStats{};
        }
        stat->sweepStats->keptCount += stats.keptCount;
        stat->sweepStats->sweptCount += stats.sweptCount;
    }
}

void GCHandle::sweptExtraObjects(gc::SweepStats stats) noexcept {
    std::lock_guard guard(lock);
    if (auto* stat = statByEpoch(epoch_)) {
        if (!stat->sweepExtraObjectsStats) {
            stat->sweepExtraObjectsStats = gc::SweepStats{};
        }
        stat->sweepExtraObjectsStats->keptCount += stats.keptCount;
        stat->sweepExtraObjectsStats->sweptCount += stats.sweptCount;
    }
}

GCHandle::GCSweepScope::GCSweepScope(kotlin::gc::GCHandle& handle) : handle_(handle) {}

GCHandle::GCSweepScope::~GCSweepScope() {
    handle_.swept(stats_);
    GCLogDebug(
            handle_.getEpoch(),
            "Collected %" PRId64 " heap objects in %" PRIu64 " microseconds. "
            "%" PRId64 " heap objects are kept alive.",
            stats_.sweptCount, getStageTime(), stats_.keptCount);
}

GCHandle::GCSweepExtraObjectsScope::GCSweepExtraObjectsScope(kotlin::gc::GCHandle& handle) : handle_(handle) {}

GCHandle::GCSweepExtraObjectsScope::~GCSweepExtraObjectsScope() {
    handle_.sweptExtraObjects(stats_);
    GCLogDebug(
            handle_.getEpoch(),
            "Collected %" PRId64 " extra objects in %" PRIu64 " microseconds. "
            "%" PRId64 " extra objects are kept alive.",
            stats_.sweptCount, getStageTime(), stats_.keptCount);
}

GCHandle::GCGlobalRootSetScope::GCGlobalRootSetScope(kotlin::gc::GCHandle& handle) : handle_(handle) {}

GCHandle::GCGlobalRootSetScope::~GCGlobalRootSetScope(){
    handle_.globalRootSetCollected(globalRoots_, stableRoots_);
    GCLogDebug(handle_.getEpoch(), "Collected global root set global=%" PRIu64 " stableRef=%" PRIu64 " in %" PRIu64" microseconds.",
               globalRoots_, stableRoots_, getStageTime());
}

GCHandle::GCThreadRootSetScope::GCThreadRootSetScope(kotlin::gc::GCHandle& handle, mm::ThreadData& threadData) :
    handle_(handle), threadData_(threadData) {}

GCHandle::GCThreadRootSetScope::~GCThreadRootSetScope(){
    handle_.threadRootSetCollected(threadData_, threadLocalRoots_, stackRoots_);
    GCLogDebug(handle_.getEpoch(), "Collected root set for thread #%d: stack=%" PRIu64 " tls=%" PRIu64 " in %" PRIu64" microseconds.",
               threadData_.threadId(), stackRoots_, threadLocalRoots_, getStageTime());
}

GCHandle::GCMarkScope::GCMarkScope(kotlin::gc::GCHandle& handle) : handle_(handle){}

GCHandle::GCMarkScope::~GCMarkScope() {
    handle_.marked(stats_);
    GCLogDebug(handle_.getEpoch(),
               "Marked %" PRIu64 " objects in %" PRIu64 " microseconds.",
               stats_.markedCount, getStageTime());
}

} // namespace kotlin::gc
