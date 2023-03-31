/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

#pragma once

#include <cstdint>
#include <pthread.h>

#include "Common.h"
#include "Porting.h"
#include "Utils.hpp"

#define GCLogInfo(epoch, format, ...) RuntimeLogInfo({kTagGC}, "Epoch #%" PRIu64 ": " format, epoch, ##__VA_ARGS__)
#define GCLogDebug(epoch, format, ...) RuntimeLogDebug({kTagGC}, "Epoch #%" PRIu64 ": " format, epoch, ##__VA_ARGS__)

namespace kotlin::mm {
class ThreadData;
}

namespace kotlin::gc {

class GCHandle;

struct MemoryUsage {
    uint64_t objectsCount;
    uint64_t totalObjectsSize;
};

class GCHandle {
public:
    class GCStageScopeUsTimer {
    protected:
        uint64_t startTime_ = konan::getTimeMicros();
        uint64_t getStageTime() const { return (konan::getTimeMicros() - startTime_); }
    };

    class GCSweepScope : GCStageScopeUsTimer, Pinned {
        GCHandle& handle_;
        MemoryUsage getUsage();

    public:
        explicit GCSweepScope(GCHandle& handle);
        ~GCSweepScope();
    };

    class GCSweepExtraObjectsScope : GCStageScopeUsTimer, Pinned {
        GCHandle& handle_;
        MemoryUsage getUsage();

    public:
        explicit GCSweepExtraObjectsScope(GCHandle& handle);
        ~GCSweepExtraObjectsScope();
    };

    class GCGlobalRootSetScope : GCStageScopeUsTimer, Pinned {
        GCHandle& handle_;
        uint64_t globalRoots_ = 0;
        uint64_t stableRoots_ = 0;

    public:
        explicit GCGlobalRootSetScope(GCHandle& handle);
        ~GCGlobalRootSetScope();
        void addGlobalRoot() { globalRoots_++; }
        void addStableRoot() { stableRoots_++; }
    };

    class GCThreadRootSetScope : GCStageScopeUsTimer, Pinned {
        GCHandle& handle_;
        mm::ThreadData& threadData_;
        uint64_t stackRoots_ = 0;
        uint64_t threadLocalRoots_ = 0;

    public:
        explicit GCThreadRootSetScope(GCHandle& handle, mm::ThreadData& threadData);
        ~GCThreadRootSetScope();
        void addStackRoot() { stackRoots_++; }
        void addThreadLocalRoot() { threadLocalRoots_++; }
    };

    class GCMarkScope : GCStageScopeUsTimer, Pinned {
        GCHandle& handle_;
        uint64_t objectsCount = 0;
        uint64_t totalObjectSizeBytes = 0;

    public:
        explicit GCMarkScope(GCHandle& handle);
        ~GCMarkScope();
        void addObject(uint64_t objectSize) {
            totalObjectSizeBytes += objectSize;
            objectsCount++;
        }
    };

    class GCProcessWeaksScope : GCStageScopeUsTimer, Pinned {
        GCHandle& handle_;
        uint64_t undisposedCount_ = 0;
        uint64_t aliveCount_ = 0;
        uint64_t nulledCount_ = 0;

    public:
        explicit GCProcessWeaksScope(GCHandle& handle) noexcept;
        ~GCProcessWeaksScope();

        void addUndisposed() noexcept { ++undisposedCount_; }
        void addAlive() noexcept { ++aliveCount_; }
        void addNulled() noexcept { ++nulledCount_; }
    };

private:
    uint64_t epoch_;
    explicit GCHandle(uint64_t epoch) : epoch_(epoch) {}

    void threadRootSetCollected(mm::ThreadData& threadData, uint64_t threadLocalReferences, uint64_t stackReferences);
    void globalRootSetCollected(uint64_t globalReferences, uint64_t stableReferences);
    void heapUsageBefore(MemoryUsage usage);
    void heapUsageAfter(MemoryUsage usage);
    void extraObjectsUsageBefore(MemoryUsage usage);
    void extraObjectsUsageAfter(MemoryUsage usage);
    void marked(MemoryUsage usage);

public:
    static GCHandle create(uint64_t epoch);
    static GCHandle createFakeForTests();
    static GCHandle getByEpoch(uint64_t epoch);
    static void ClearForTests();

    uint64_t getEpoch() { return epoch_; }
    void finished();
    void finalizersDone();
    void finalizersScheduled(uint64_t finalizersCount);
    void suspensionRequested();
    void threadsAreSuspended();
    void threadsAreResumed();
    GCSweepScope sweep() { return GCSweepScope(*this); }
    GCSweepExtraObjectsScope sweepExtraObjects() { return GCSweepExtraObjectsScope(*this); }
    GCGlobalRootSetScope collectGlobalRoots() { return GCGlobalRootSetScope(*this); }
    GCThreadRootSetScope collectThreadRoots(mm::ThreadData& threadData) { return GCThreadRootSetScope(*this, threadData); }
    GCMarkScope mark() { return GCMarkScope(*this); }
    GCProcessWeaksScope processWeaks() noexcept { return GCProcessWeaksScope(*this); }

    MemoryUsage getMarked();
};
}
