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
void Kotlin_Internal_GC_GCInfoBuilder_setMemoryUsageBefore(KRef thiz, KNativePtr name, KLong objectsCount, KLong totalObjectsSize);
void Kotlin_Internal_GC_GCInfoBuilder_setMemoryUsageAfter(KRef thiz, KNativePtr name, KLong objectsCount, KLong totalObjectsSize);
}

namespace {

struct MemoryUsageMap {
    std::optional<kotlin::gc::MemoryUsage> heap;
    std::optional<kotlin::gc::MemoryUsage> extra;

    void build(KRef builder, void (*add)(KRef, KNativePtr, KLong, KLong)) {
        if (heap) {
            add(builder, const_cast<KNativePtr>(static_cast<const void*>("heap")),
                static_cast<KLong>(heap->objectsCount),
                static_cast<KLong>(heap->totalObjectsSize));
        }
        if (extra) {
            add(builder, const_cast<KNativePtr>(static_cast<const void*>("extra")),
                static_cast<KLong>(extra->objectsCount),
                static_cast<KLong>(extra->totalObjectsSize));
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
    std::optional<kotlin::gc::MemoryUsage> markStats;
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
        RuntimeLogWarning({kTagGC}, "Starting new GC epoch, while previous is not finished\n");
    }
    current.epoch = static_cast<KLong>(epoch);
    current.startTime = static_cast<KLong>(konan::getTimeNanos());
    if (last.endTime) {
        GCLogInfo(epoch, "Started. Time since last GC %" PRIu64 " microseconds.", *current.startTime - *last.endTime);
    } else {
        GCLogInfo(epoch, "Started.");
    }
    return getByEpoch(epoch);
}
GCHandle GCHandle::createFakeForTests() { return getByEpoch(std::numeric_limits<uint64_t>::max()); }
GCHandle GCHandle::getByEpoch(uint64_t epoch) {
    return GCHandle{epoch};
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
    GCLogDebug(epoch_, "Requested thread suspension by thread %d", konan::currentThreadId());
    if (auto* stat = statByEpoch(epoch_)) {
        stat->pauseStartTime = static_cast<KLong>(konan::getTimeNanos());
    }
}
void GCHandle::threadsAreSuspended() {
    std::lock_guard guard(lock);
    if (auto* stat = statByEpoch(epoch_)) {
        if (stat->pauseStartTime) {
            auto time = (konan::getTimeNanos() - *stat->pauseStartTime) / 1000;
            GCLogInfo(epoch_, "Suspended all threads in %" PRIu64 " microseconds", time);
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
            GCLogInfo(epoch_, "Resume all threads. Total pause time is %" PRId64 " microseconds.", time);
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
        }
    }
    GCLogInfo(epoch_, "Finalization is done.");
}
void GCHandle::finalizersScheduled(uint64_t finalizersCount) {
    GCLogInfo(epoch_, "Finalization is scheduled for %" PRIu64 " objects.", finalizersCount);
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


void GCHandle::heapUsageBefore(MemoryUsage usage) {
    std::lock_guard guard(lock);
    if (auto* stat = statByEpoch(epoch_)) {
        stat->memoryUsageBefore.heap = usage;
    }
}

void GCHandle::marked(kotlin::gc::MemoryUsage usage) {
    std::lock_guard guard(lock);
    if (auto* stat = statByEpoch(epoch_)) {
        if (!stat->markStats) {
            stat->markStats = MemoryUsage{0, 0};
        }
        stat->markStats->totalObjectsSize += usage.totalObjectsSize;
        stat->markStats->objectsCount += usage.objectsCount;
    }
}

MemoryUsage GCHandle::getMarked() {
    std::lock_guard guard(lock);
    if (auto* stat = statByEpoch(epoch_)) {
        if (stat->markStats) {
            return *stat->markStats;
        }
    }
    return MemoryUsage{0, 0};
}

void GCHandle::heapUsageAfter(MemoryUsage usage) {
    std::lock_guard guard(lock);
    if (auto* stat = statByEpoch(epoch_)) {
        stat->memoryUsageAfter.heap = usage;
        if (stat->memoryUsageBefore.heap) {
            GCLogInfo(
                    epoch_,
                    "Collected %" PRId64 " heap objects of total size %" PRId64 " bytes. "
                    "%" PRId64 " heap objects of total size %" PRId64 " bytes are still alive.",
                    stat->memoryUsageBefore.heap->objectsCount - stat->memoryUsageAfter.heap->objectsCount,
                    stat->memoryUsageBefore.heap->totalObjectsSize - stat->memoryUsageAfter.heap->totalObjectsSize,
                    stat->memoryUsageAfter.heap->objectsCount, stat->memoryUsageAfter.heap->totalObjectsSize);
        }
        if (stat->markStats) {
            RuntimeAssert(
                    stat->markStats->objectsCount == usage.objectsCount,
                    "Mismatch in statistics: marked %" PRId64 " objects, while %" PRId64 " is alive after sweep",
                    stat->markStats->objectsCount, usage.objectsCount);
            RuntimeAssert(
                    stat->markStats->totalObjectsSize == usage.totalObjectsSize,
                    "Mismatch in statistics: total marked size is %" PRId64 " bytes, while %" PRId64 " bytes is alive after sweep",
                    stat->markStats->totalObjectsSize, usage.totalObjectsSize);
        }
    }
}

void GCHandle::extraObjectsUsageBefore(MemoryUsage usage) {
    std::lock_guard guard(lock);
    if (auto* stat = statByEpoch(epoch_)) {
        stat->memoryUsageBefore.extra = usage;
    }
}
void GCHandle::extraObjectsUsageAfter(MemoryUsage usage) {
    std::lock_guard guard(lock);
    if (auto* stat = statByEpoch(epoch_)) {
        stat->memoryUsageAfter.extra = usage;
        if (stat->memoryUsageBefore.extra) {
            GCLogInfo(
                    epoch_,
                    "Collected %" PRId64 " extra objects of total size %" PRId64 ". "
                    "%" PRId64 " extra objects of total size %" PRId64 " are still alive.",
                    stat->memoryUsageBefore.extra->objectsCount - stat->memoryUsageAfter.extra->objectsCount,
                    stat->memoryUsageBefore.extra->totalObjectsSize - stat->memoryUsageAfter.extra->totalObjectsSize,
                    stat->memoryUsageAfter.extra->objectsCount, stat->memoryUsageAfter.extra->totalObjectsSize);
        }
    }
}

MemoryUsage GCHandle::GCSweepScope::getUsage() {
    return MemoryUsage{
            mm::GlobalData::Instance().gc().GetHeapObjectsCountUnsafe(),
            mm::GlobalData::Instance().gc().GetTotalHeapObjectsSizeUnsafe(),
    };
}

GCHandle::GCSweepScope::GCSweepScope(kotlin::gc::GCHandle& handle) :
    handle_(handle) {
    handle_.heapUsageBefore(getUsage());
}

GCHandle::GCSweepScope::~GCSweepScope() {
    GCLogDebug(handle_.getEpoch(), "Swept is done in %" PRIu64 " microseconds.", getStageTime());
    handle_.heapUsageAfter(getUsage());
}

MemoryUsage GCHandle::GCSweepExtraObjectsScope::getUsage() {
    return MemoryUsage{
            mm::GlobalData::Instance().gc().GetExtraObjectsCountUnsafe(),
            mm::GlobalData::Instance().gc().GetTotalExtraObjectsSizeUnsafe(),
    };
}

GCHandle::GCSweepExtraObjectsScope::GCSweepExtraObjectsScope(kotlin::gc::GCHandle& handle) :
    handle_(handle) {
    handle_.extraObjectsUsageBefore(getUsage());
}

GCHandle::GCSweepExtraObjectsScope::~GCSweepExtraObjectsScope() {
    GCLogDebug(handle_.getEpoch(), "Swept extra objects is done in %" PRIu64 " microseconds", getStageTime());
    handle_.extraObjectsUsageAfter(getUsage());
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
    handle_.marked(MemoryUsage{objectsCount, totalObjectSizeBytes});
    GCLogDebug(handle_.getEpoch(),
               "Marked %" PRIu64 " objects in %" PRIu64 " microseconds in thread %d",
               objectsCount, getStageTime(), konan::currentThreadId());
}

gc::GCHandle::GCProcessWeaksScope::GCProcessWeaksScope(gc::GCHandle& handle) noexcept : handle_(handle) {}

gc::GCHandle::GCProcessWeaksScope::~GCProcessWeaksScope() {
    GCLogDebug(
            handle_.getEpoch(),
            "Processed special refs in %" PRIu64 " microseconds. %" PRIu64 " are undisposed, %" PRIu64 " are alive, %" PRIu64
            " are nulled out",
            getStageTime(), undisposedCount_, aliveCount_, nulledCount_);
}

} // namespace kotlin::gc
