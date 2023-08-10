/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

#pragma once

#include <cstdint>
#include <pthread.h>

#include "Common.h"
#include "Logging.hpp"
#include "Porting.h"
#include "Utils.hpp"
#include "std_support/Optional.hpp"

#define GCLogInfo(epoch, format, ...) RuntimeLogInfo({kTagGC}, "Epoch #%" PRIu64 ": " format, epoch, ##__VA_ARGS__)
#define GCLogDebug(epoch, format, ...) RuntimeLogDebug({kTagGC}, "Epoch #%" PRIu64 ": " format, epoch, ##__VA_ARGS__)
#define GCLogWarning(epoch, format, ...) RuntimeLogWarning({kTagGC}, "Epoch #%" PRIu64 ": " format, epoch, ##__VA_ARGS__)

namespace kotlin::mm {
class ThreadData;
}

namespace kotlin::gc {

class GCHandle;

struct SweepStats {
    uint64_t sweptCount = 0;
    uint64_t keptCount = 0;
};

struct MarkStats {
    uint64_t markedCount = 0;
};

class GCStageScopeUsTimer;

class GCHandle {
public:
    class GCSweepScope;
    class GCSweepExtraObjectsScope;
    class GCGlobalRootSetScope;
    class GCThreadRootSetScope;
    class GCMarkScope;
    class GCProcessWeaksScope;

    static GCHandle create(uint64_t epoch);
    static GCHandle createFakeForTests();
    static GCHandle getByEpoch(uint64_t epoch);
    static std::optional<GCHandle> currentEpoch() noexcept;
    static GCHandle invalid();
    static void ClearForTests();

    uint64_t getEpoch() { return epoch_; }
    bool isValid() const;
    void finished();
    void finalizersDone();
    void finalizersScheduled(uint64_t finalizersCount);
    void suspensionRequested();
    void threadsAreSuspended();
    void threadsAreResumed();

    GCSweepScope sweep();
    GCSweepExtraObjectsScope sweepExtraObjects();
    GCGlobalRootSetScope collectGlobalRoots();
    GCThreadRootSetScope collectThreadRoots(mm::ThreadData& threadData);
    GCMarkScope mark();
    GCProcessWeaksScope processWeaks() noexcept;

    MarkStats getMarked();

private:
    uint64_t epoch_;
    explicit GCHandle(uint64_t epoch) : epoch_(epoch) {}

    void threadRootSetCollected(mm::ThreadData& threadData, uint64_t threadLocalReferences, uint64_t stackReferences);
    void globalRootSetCollected(uint64_t globalReferences, uint64_t stableReferences);
    void swept(SweepStats stats, uint64_t markedCount) noexcept;
    void sweptExtraObjects(SweepStats stats) noexcept;
    void marked(MarkStats stats);
};

class GCStageScopeUsTimer {
public:
    GCStageScopeUsTimer() : GCStageScopeUsTimer(GCHandle::invalid()) {}
    explicit GCStageScopeUsTimer(GCHandle gcHandle) : handle_(gcHandle) {}

    friend void swap(GCStageScopeUsTimer& first, GCStageScopeUsTimer& second) noexcept {
        using std::swap;
        swap(first.handle_, second.handle_);
        swap(first.startTime_, second.startTime_);
    }
    GCStageScopeUsTimer(GCStageScopeUsTimer&& that) noexcept : GCStageScopeUsTimer() {
        swap(*this, that);
    }
    GCStageScopeUsTimer& operator=(GCStageScopeUsTimer that) noexcept {
        swap(*this, that);
        return *this;
    }

protected:
    uint64_t getStageTime() const { return (konan::getTimeMicros() - startTime_); }

    GCHandle handle_;
    uint64_t startTime_ = konan::getTimeMicros();
};

class GCHandle::GCSweepScope : GCStageScopeUsTimer {
    SweepStats stats_;
    uint64_t markedCount_ = 0;

public:
    explicit GCSweepScope(GCHandle handle);
    GCSweepScope(GCSweepScope&& that) = default;
    GCSweepScope& operator=(GCSweepScope&& that) = default;
    ~GCSweepScope();

    void addSweptObject() noexcept { stats_.sweptCount += 1; }
    void addKeptObject() noexcept { stats_.keptCount += 1; }
    // Custom allocator only. To be finalized objects are kept alive.
    void addMarkedObject() noexcept { markedCount_ += 1; }
};

class GCHandle::GCSweepExtraObjectsScope : private GCStageScopeUsTimer {
    SweepStats stats_;

public:
    explicit GCSweepExtraObjectsScope(GCHandle handle);
    GCSweepExtraObjectsScope(GCSweepExtraObjectsScope&& that) = default;
    GCSweepExtraObjectsScope& operator=(GCSweepExtraObjectsScope&& that) = default;
    ~GCSweepExtraObjectsScope();

    void addSweptObject() noexcept { stats_.sweptCount += 1; }
    void addKeptObject() noexcept { stats_.keptCount += 1; }
};

class GCHandle::GCGlobalRootSetScope : private GCStageScopeUsTimer {
    uint64_t globalRoots_ = 0;
    uint64_t stableRoots_ = 0;

public:
    explicit GCGlobalRootSetScope(GCHandle handle);
    GCGlobalRootSetScope(GCGlobalRootSetScope&& that) = default;
    GCGlobalRootSetScope& operator=(GCGlobalRootSetScope&& that) = default;
    ~GCGlobalRootSetScope();
    void addGlobalRoot() { globalRoots_++; }
    void addStableRoot() { stableRoots_++; }
};

class GCHandle::GCThreadRootSetScope : private GCStageScopeUsTimer {
    mm::ThreadData& threadData_;
    uint64_t stackRoots_ = 0;
    uint64_t threadLocalRoots_ = 0;

public:
    explicit GCThreadRootSetScope(GCHandle handle, mm::ThreadData& threadData);
    GCThreadRootSetScope(GCThreadRootSetScope&& that) = default;
    GCThreadRootSetScope& operator=(GCThreadRootSetScope&& that) = delete;
    ~GCThreadRootSetScope();
    void addStackRoot() { stackRoots_++; }
    void addThreadLocalRoot() { threadLocalRoots_++; }
};

class GCHandle::GCMarkScope : private GCStageScopeUsTimer {
    MarkStats stats_;

public:
    explicit GCMarkScope(GCHandle handle);
    GCMarkScope(GCMarkScope&& that) = default;
    GCMarkScope& operator=(GCMarkScope&& that) = default;
    ~GCMarkScope();

    void addObject() noexcept { ++stats_.markedCount; }
};

class GCHandle::GCProcessWeaksScope : private GCStageScopeUsTimer {
    uint64_t undisposedCount_ = 0;
    uint64_t aliveCount_ = 0;
    uint64_t nulledCount_ = 0;

public:
    explicit GCProcessWeaksScope(GCHandle handle) noexcept;
    GCProcessWeaksScope(GCProcessWeaksScope&& that) = default;
    GCProcessWeaksScope& operator=(GCProcessWeaksScope&& that) = default;
    ~GCProcessWeaksScope();

    void addUndisposed() noexcept { ++undisposedCount_; }
    void addAlive() noexcept { ++aliveCount_; }
    void addNulled() noexcept { ++nulledCount_; }
};

}
