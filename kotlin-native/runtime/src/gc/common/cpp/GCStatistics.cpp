/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

#include "GCStatistics.hpp"

#include <cinttypes>
#include <limits>
#include <optional>

#include "Allocator.hpp"
#include "Logging.hpp"
#include "Mutex.hpp"
#include "Porting.h"
#include "ThreadData.hpp"
#include "Types.h"

using namespace kotlin;

extern "C" {
void Kotlin_Internal_GC_GCInfoBuilder_setEpoch(KRef thiz, KLong value);
void Kotlin_Internal_GC_GCInfoBuilder_setStartTime(KRef thiz, KLong value);
void Kotlin_Internal_GC_GCInfoBuilder_setEndTime(KRef thiz, KLong value);
void Kotlin_Internal_GC_GCInfoBuilder_setFirstPauseRequestTime(KRef thiz, KLong value);
void Kotlin_Internal_GC_GCInfoBuilder_setFirstPauseStartTime(KRef thiz, KLong value);
void Kotlin_Internal_GC_GCInfoBuilder_setFirstPauseEndTime(KRef thiz, KLong value);
void Kotlin_Internal_GC_GCInfoBuilder_setSecondPauseRequestTime(KRef thiz, KLong value);
void Kotlin_Internal_GC_GCInfoBuilder_setSecondPauseStartTime(KRef thiz, KLong value);
void Kotlin_Internal_GC_GCInfoBuilder_setSecondPauseEndTime(KRef thiz, KLong value);
void Kotlin_Internal_GC_GCInfoBuilder_setPostGcCleanupTime(KRef thiz, KLong value);
void Kotlin_Internal_GC_GCInfoBuilder_setRootSet(KRef thiz,
                                                 KLong threadLocalReferences, KLong stackReferences,
                                                 KLong globalReferences, KLong stableReferences);
void Kotlin_Internal_GC_GCInfoBuilder_setMarkStats(KRef thiz, KLong markedCount);
void Kotlin_Internal_GC_GCInfoBuilder_setSweepStats(KRef thiz, KNativePtr name, KLong sweptCount, KLong keptCount);
void Kotlin_Internal_GC_GCInfoBuilder_setMemoryUsageBefore(KRef thiz, KNativePtr name, KLong sizeBytes);
void Kotlin_Internal_GC_GCInfoBuilder_setMemoryUsageAfter(KRef thiz, KNativePtr name, KLong sizeBytes);
}

namespace {

constexpr KNativePtr heapPoolKey = const_cast<KNativePtr>(static_cast<const void*>("heap"));
constexpr KNativePtr extraPoolKey = const_cast<KNativePtr>(static_cast<const void*>("extra"));

struct MemoryUsage {
    uint64_t sizeBytes;
};

struct MemoryUsageMap {
    std::optional<MemoryUsage> heap;

    void build(KRef builder, void (*add)(KRef, KNativePtr, KLong)) {
        if (heap) {
            add(builder, heapPoolKey, static_cast<KLong>(heap->sizeBytes));
        }
    }
};

struct SweepStatsMap {
    std::optional<gc::SweepStats> heap;
    std::optional<gc::SweepStats> extra;

    void build(KRef builder, void (*add)(KRef, KNativePtr, KLong, KLong)) {
        if (heap) {
            add(builder, heapPoolKey, static_cast<KLong>(heap->keptCount), static_cast<KLong>(heap->sweptCount));
        }
        if (extra) {
            add(builder, extraPoolKey, static_cast<KLong>(extra->keptCount), static_cast<KLong>(extra->sweptCount));
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

    std::optional<KLong> firstPauseRequestTime;
    std::optional<KLong> firstPauseStartTime;
    std::optional<KLong> firstPauseEndTime;

    std::optional<KLong> secondPauseRequestTime;
    std::optional<KLong> secondPauseStartTime;
    std::optional<KLong> secondPauseEndTime;

    std::optional<KLong> finalizersDoneTime;
    std::optional<RootSetStatistics> rootSet;
    std::optional<kotlin::gc::MarkStats> markStats;
    SweepStatsMap sweepStats;
    MemoryUsageMap memoryUsageBefore;
    MemoryUsageMap memoryUsageAfter;

    void build(KRef builder) {
        if (!epoch) return;
        Kotlin_Internal_GC_GCInfoBuilder_setEpoch(builder, static_cast<KLong>(*epoch));
        if (startTime) Kotlin_Internal_GC_GCInfoBuilder_setStartTime(builder, *startTime);
        if (endTime) Kotlin_Internal_GC_GCInfoBuilder_setEndTime(builder, *endTime);
        if (firstPauseRequestTime) Kotlin_Internal_GC_GCInfoBuilder_setFirstPauseRequestTime(builder, *firstPauseRequestTime);
        if (firstPauseStartTime) Kotlin_Internal_GC_GCInfoBuilder_setFirstPauseStartTime(builder, *firstPauseStartTime);
        if (firstPauseEndTime) Kotlin_Internal_GC_GCInfoBuilder_setFirstPauseEndTime(builder, *firstPauseEndTime);
        if (secondPauseRequestTime) Kotlin_Internal_GC_GCInfoBuilder_setSecondPauseRequestTime(builder, *secondPauseRequestTime);
        if (secondPauseStartTime) Kotlin_Internal_GC_GCInfoBuilder_setSecondPauseStartTime(builder, *secondPauseStartTime);
        if (secondPauseEndTime) Kotlin_Internal_GC_GCInfoBuilder_setSecondPauseEndTime(builder, *secondPauseEndTime);
        if (finalizersDoneTime) Kotlin_Internal_GC_GCInfoBuilder_setPostGcCleanupTime(builder, *finalizersDoneTime);
        if (rootSet)
            Kotlin_Internal_GC_GCInfoBuilder_setRootSet(
                    builder, rootSet->threadLocalReferences, rootSet->stackReferences, rootSet->globalReferences,
                    rootSet->stableReferences);
        if (markStats)
            Kotlin_Internal_GC_GCInfoBuilder_setMarkStats(builder, markStats->markedCount);
        sweepStats.build(builder, Kotlin_Internal_GC_GCInfoBuilder_setSweepStats);
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
    return MemoryUsage{
            alloc::allocatedBytes(),
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
        auto time = (*current.startTime - *last.endTime) / 1000;
        GCLogInfo(epoch, "Started. Time since last GC %" PRIu64 " microseconds.", time);
    } else {
        GCLogInfo(epoch, "Started.");
    }
    current.memoryUsageBefore.heap = currentHeapUsage();
    return getByEpoch(epoch);
}
GCHandle GCHandle::createFakeForTests() { return getByEpoch(invalid().getEpoch() - 1); }
GCHandle GCHandle::getByEpoch(uint64_t epoch) {
    GCHandle handle{epoch};
    RuntimeAssert(handle.isValid(), "Must be valid");
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

GCHandle GCHandle::invalid() {
    return GCHandle{std::numeric_limits<uint64_t>::max()};
}
void GCHandle::ClearForTests() {
    std::lock_guard guard(lock);
    current = {};
    last = {};
}
bool GCHandle::isValid() const {
    return epoch_ != GCHandle::invalid().epoch_;
}
void GCHandle::finished() {
    std::lock_guard guard(lock);
    if (auto* stat = statByEpoch(epoch_)) {
        stat->endTime = static_cast<KLong>(konan::getTimeNanos());
        stat->memoryUsageAfter.heap = currentHeapUsage();
        if (stat->markStats && stat->sweepStats.heap) {
            RuntimeAssert(stat->markStats->markedCount == stat->sweepStats.heap->keptCount,
                          "Mismatch in statistics: marked %" PRId64 " objects, while %" PRId64 " are alive after sweep",
                          stat->markStats->markedCount, stat->sweepStats.heap->keptCount);
        }
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
            GCLogInfo(epoch_, "Mark: %" PRIu64 " objects.", stat->markStats->markedCount);
        }
        if (auto stats = stat->sweepStats.extra) {
            GCLogInfo(
                    epoch_, "Sweep extra objects: swept %" PRIu64 " objects, kept %" PRIu64 " objects",
                    stats->sweptCount, stats->keptCount);
        }
        if (auto stats = stat->sweepStats.heap) {
            GCLogInfo(
                    epoch_, "Sweep: swept %" PRIu64 " objects, kept %" PRIu64 " objects", stats->sweptCount,
                    stats->keptCount);
        }
        if (stat->memoryUsageBefore.heap && stat->memoryUsageAfter.heap) {
            GCLogInfo(
                    epoch_, "Heap memory usage: before %" PRIu64 " bytes, after %" PRIu64 " bytes", stat->memoryUsageBefore.heap->sizeBytes,
                    stat->memoryUsageAfter.heap->sizeBytes);
        }
        if (stat->firstPauseRequestTime && stat->firstPauseStartTime) {
            auto time = (*stat->firstPauseStartTime - *stat->firstPauseRequestTime) / 1000;
            GCLogInfo(epoch_, "Time to pause #1: %" PRIu64 " microseconds.", time);
        }
        if (stat->firstPauseRequestTime && stat->firstPauseEndTime) {
            auto time = (*stat->firstPauseEndTime - *stat->firstPauseRequestTime) / 1000;
            GCLogInfo(epoch_, "Mutators pause time #1: %" PRIu64 " microseconds.", time);
        }
        if (stat->secondPauseRequestTime && stat->secondPauseStartTime) {
            auto time = (*stat->secondPauseStartTime - *stat->secondPauseRequestTime) / 1000;
            GCLogInfo(epoch_, "Time to pause #2: %" PRIu64 " microseconds.", time);
        }
        if (stat->secondPauseRequestTime && stat->secondPauseEndTime) {
            auto time = (*stat->secondPauseEndTime - *stat->secondPauseRequestTime) / 1000;
            GCLogInfo(epoch_, "Mutators pause time #2: %" PRIu64 " microseconds.", time);
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
        auto requestTime = static_cast<KLong>(konan::getTimeNanos());
        if (!stat->firstPauseRequestTime) {
            stat->firstPauseRequestTime = requestTime;
        } else {
            RuntimeAssert(!stat->secondPauseRequestTime, "GCStatistics support max two pauses per GC epoch");
            stat->secondPauseRequestTime = requestTime;
        }
    }
}
void GCHandle::threadsAreSuspended() {
    std::lock_guard guard(lock);
    if (auto* stat = statByEpoch(epoch_)) {
        auto startTime = static_cast<KLong>(konan::getTimeNanos());
        std::optional<KLong> requestTime;
        if (!stat->firstPauseStartTime) {
            stat->firstPauseStartTime = startTime;
            requestTime = stat->firstPauseRequestTime;
        } else {
            RuntimeAssert(!stat->secondPauseStartTime, "GCStatistics support max two pauses per GC epoch");
            stat->secondPauseStartTime = startTime;
            requestTime = stat->secondPauseRequestTime;
        }
        if (requestTime) {
            auto time = (startTime - *requestTime) / 1000;
            GCLogDebug(epoch_, "Suspended all threads in %" PRIu64 " microseconds", time);
        }
    }
}
void GCHandle::threadsAreResumed() {
    std::lock_guard guard(lock);
    if (auto* stat = statByEpoch(epoch_)) {
        auto endTime = static_cast<KLong>(konan::getTimeNanos());
        std::optional<KLong> startTime;
        if (!stat->firstPauseEndTime) {
            stat->firstPauseEndTime = endTime;
            startTime = stat->firstPauseStartTime;
        } else {
            RuntimeAssert(!stat->secondPauseEndTime, "GCStatistics support max two pauses per GC epoch");
            stat->secondPauseEndTime = endTime;
            startTime = stat->secondPauseStartTime;
        }
        if (startTime) {
            auto time = (endTime - *startTime) / 1000;
            GCLogDebug(epoch_, "Resume all threads. Total pause time is %" PRId64 " microseconds.", time);
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

void GCHandle::swept(gc::SweepStats stats, uint64_t markedCount) noexcept {
    std::lock_guard guard(lock);
    if (auto* stat = statByEpoch(epoch_)) {
        auto& heap = stat->sweepStats.heap;
        if (!heap) {
            heap = gc::SweepStats{};
        }
        heap->keptCount += stats.keptCount;
        heap->sweptCount += stats.sweptCount;
        RuntimeAssert(static_cast<bool>(stat->markStats), "Mark must have already happened");
        stat->markStats->markedCount += markedCount;
    }
}

void GCHandle::sweptExtraObjects(gc::SweepStats stats) noexcept {
    std::lock_guard guard(lock);
    if (auto* stat = statByEpoch(epoch_)) {
        auto& extra = stat->sweepStats.extra;
        if (!extra) {
            extra = gc::SweepStats{};
        }
        extra->keptCount += stats.keptCount;
        extra->sweptCount += stats.sweptCount;
    }
}

GCHandle::GCSweepScope GCHandle::sweep() { return GCSweepScope(*this); }
GCHandle::GCSweepExtraObjectsScope GCHandle::sweepExtraObjects() { return GCSweepExtraObjectsScope(*this); }
GCHandle::GCGlobalRootSetScope GCHandle::collectGlobalRoots() { return GCGlobalRootSetScope(*this); }
GCHandle::GCThreadRootSetScope GCHandle::collectThreadRoots(mm::ThreadData& threadData) { return GCThreadRootSetScope(*this, threadData); }
GCHandle::GCMarkScope GCHandle::mark() { return GCMarkScope(*this); }
GCHandle::GCProcessWeaksScope GCHandle::processWeaks() noexcept { return GCProcessWeaksScope(*this); }

GCHandle::GCSweepScope::GCSweepScope(kotlin::gc::GCHandle handle) : GCStageScopeBase(handle) {}

GCHandle::GCSweepScope::~GCSweepScope() {
    if (!handle_.isValid()) return;
    handle_.swept(stats_, markedCount_);
    GCLogDebug(
            handle_.getEpoch(),
            "Collected %" PRId64 " heap objects in %" PRIu64 " microseconds. "
            "%" PRId64 " heap objects are kept alive.",
            stats_.sweptCount, getStageTime(), stats_.keptCount);
}

GCHandle::GCSweepExtraObjectsScope::GCSweepExtraObjectsScope(kotlin::gc::GCHandle handle) : GCStageScopeBase(handle) {}

GCHandle::GCSweepExtraObjectsScope::~GCSweepExtraObjectsScope() {
    if (!handle_.isValid()) return;
    handle_.sweptExtraObjects(stats_);
    GCLogDebug(
            handle_.getEpoch(),
            "Collected %" PRId64 " extra objects in %" PRIu64 " microseconds. "
            "%" PRId64 " extra objects are kept alive.",
            stats_.sweptCount, getStageTime(), stats_.keptCount);
}

GCHandle::GCGlobalRootSetScope::GCGlobalRootSetScope(kotlin::gc::GCHandle handle) : GCStageScopeBase(handle) {}

GCHandle::GCGlobalRootSetScope::~GCGlobalRootSetScope(){
    if (!handle_.isValid()) return;
    handle_.globalRootSetCollected(globalRoots_, stableRoots_);
    GCLogDebug(
            handle_.getEpoch(), "Collected global root set global=%" PRIu64 " stableRef=%" PRIu64 " in %" PRIu64 " microseconds.",
            globalRoots_, stableRoots_, getStageTime());
}

GCHandle::GCThreadRootSetScope::GCThreadRootSetScope(kotlin::gc::GCHandle handle, mm::ThreadData& threadData) :
        GCStageScopeBase(handle), threadData_(threadData) {}

GCHandle::GCThreadRootSetScope::~GCThreadRootSetScope(){
    if (!handle_.isValid()) return;
    handle_.threadRootSetCollected(threadData_, threadLocalRoots_, stackRoots_);
    GCLogDebug(
            handle_.getEpoch(), "Collected root set for thread #%d: stack=%" PRIu64 " tls=%" PRIu64 " in %" PRIu64 " microseconds.",
            threadData_.threadId(), stackRoots_, threadLocalRoots_, getStageTime());
}

GCHandle::GCMarkScope::GCMarkScope(kotlin::gc::GCHandle handle) : GCStageScopeBase(handle) {}

GCHandle::GCMarkScope::~GCMarkScope() {
    if (!handle_.isValid()) return;
    handle_.marked(stats_);
    GCLogDebug(handle_.getEpoch(), "Marked %" PRIu64 " objects in %" PRIu64 " microseconds.", stats_.markedCount, getStageTime());
}

GCHandle::GCProcessWeaksScope::GCProcessWeaksScope(gc::GCHandle handle) noexcept : GCStageScopeBase(handle) {}

GCHandle::GCProcessWeaksScope::~GCProcessWeaksScope() {
    if (!handle_.isValid()) return;
    GCLogDebug(
            handle_.getEpoch(),
            "Processed special refs in %" PRIu64 " microseconds. %" PRIu64 " are undisposed, %" PRIu64 " are alive, %" PRIu64
            " are nulled out",
            getStageTime(), undisposedCount_, aliveCount_, nulledCount_);
}

} // namespace kotlin::gc
