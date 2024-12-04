/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

#pragma once

#include <cstdint>
#include <optional>

#include "Logging.hpp"
#include "Porting.h"
#include "Utils.hpp"

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
    size_t keptSizeBytes = 0;
};

struct MarkStats {
    uint64_t markedCount = 0;
};

class GCHandle {
public:
    class GCStageScopeBase;

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

    uint64_t getEpoch() const {
        RuntimeAssert(isValid(), "Invalid GC handle");
        return epoch_;
    }
    bool isValid() const;
    void finished();
    void finalizersDone();
    void finalizersScheduled(uint64_t finalizersCount);
    void suspensionRequested();
    void threadsAreSuspended();
    void threadsAreResumed();

    [[nodiscard]] GCSweepScope sweep();
    [[nodiscard]] GCSweepExtraObjectsScope sweepExtraObjects();
    [[nodiscard]] GCGlobalRootSetScope collectGlobalRoots();
    [[nodiscard]] GCThreadRootSetScope collectThreadRoots(mm::ThreadData& threadData);
    [[nodiscard]] GCMarkScope mark();
    [[nodiscard]] GCProcessWeaksScope processWeaks() noexcept;

    MarkStats getMarked();
    size_t getKeptSizeBytes() noexcept;

private:
    uint64_t epoch_;
    explicit GCHandle(uint64_t epoch) : epoch_(epoch) {}

    void threadRootSetCollected(mm::ThreadData& threadData, uint64_t threadLocalReferences, uint64_t stackReferences);
    void globalRootSetCollected(uint64_t globalReferences, uint64_t stableReferences);
    void swept(SweepStats stats, uint64_t markedCount) noexcept;
    void sweptExtraObjects(SweepStats stats) noexcept;
    void marked(MarkStats stats);
};

class GCHandle::GCStageScopeBase : private MoveOnly {
public:
    ALWAYS_INLINE void requireValid() const {
        RuntimeAssert(handle_.isValid(), "Invalid GC handle accessed");
    }

protected:
    explicit GCStageScopeBase(GCHandle gcHandle) : handle_(gcHandle) {}

    friend void swap(GCStageScopeBase& first, GCStageScopeBase& second) noexcept {
        using std::swap;
        swap(first.handle_, second.handle_);
        swap(first.startTime_, second.startTime_);
    }
    GCStageScopeBase(GCStageScopeBase&& that) noexcept : GCStageScopeBase(GCHandle::invalid()) {
        swap(*this, that);
    }
    GCStageScopeBase& operator=(GCStageScopeBase&& that) noexcept {
        auto tmp = std::move(that);
        swap(*this, tmp);
        return *this;
    }

    uint64_t getStageTime() const { return (konan::getTimeMicros() - startTime_); }

    GCHandle handle_;
    uint64_t startTime_ = konan::getTimeMicros();
};

class GCHandle::GCSweepScope : public GCStageScopeBase {
    SweepStats stats_;
    uint64_t markedCount_ = 0;

public:
    explicit GCSweepScope(GCHandle handle);
    GCSweepScope(GCSweepScope&& that) noexcept : GCSweepScope(GCHandle::invalid()) {
        swap(*this, that);
    }
    GCSweepScope& operator=(GCSweepScope&& that) noexcept {
        auto tmp = std::move(that);
        swap(*this, tmp);
        return *this;
    }
    friend void swap(GCSweepScope& first, GCSweepScope& second) noexcept {
        using std::swap;
        swap(reinterpret_cast<GCStageScopeBase&>(first), reinterpret_cast<GCStageScopeBase&>(second));
        swap(first.stats_, second.stats_);
        swap(first.markedCount_, second.markedCount_);
    }
    ~GCSweepScope();

    void addSweptObject() noexcept {
        requireValid();
        stats_.sweptCount += 1;
    }
    void addKeptObject(size_t sizeBytes) noexcept {
        requireValid();
        stats_.keptCount += 1;
        stats_.keptSizeBytes += sizeBytes;
    }
    // Custom allocator only. To be finalized objects are kept alive.
    void addMarkedObject() noexcept {
        requireValid();
        markedCount_ += 1;
    }
};

class GCHandle::GCSweepExtraObjectsScope : public GCStageScopeBase {
    SweepStats stats_;

public:
    explicit GCSweepExtraObjectsScope(GCHandle handle);
    GCSweepExtraObjectsScope(GCSweepExtraObjectsScope&& that) noexcept : GCSweepExtraObjectsScope(GCHandle::invalid()) {
        swap(*this, that);
    }
    GCSweepExtraObjectsScope& operator=(GCSweepExtraObjectsScope&& that) noexcept {
        auto tmp = std::move(that);
        swap(*this, tmp);
        return *this;
    }
    friend void swap(GCSweepExtraObjectsScope& first, GCSweepExtraObjectsScope& second) noexcept {
        using std::swap;
        swap(reinterpret_cast<GCStageScopeBase&>(first), reinterpret_cast<GCStageScopeBase&>(second));
        swap(first.stats_, second.stats_);
    }
    ~GCSweepExtraObjectsScope();

    void addSweptObject() noexcept {
        requireValid();
        stats_.sweptCount += 1;
    }
    void addKeptObject(size_t sizeBytes) noexcept {
        requireValid();
        stats_.keptCount += 1;
        stats_.keptSizeBytes += sizeBytes;
    }
};

class GCHandle::GCGlobalRootSetScope : public GCStageScopeBase {
    uint64_t globalRoots_ = 0;
    uint64_t stableRoots_ = 0;

public:
    explicit GCGlobalRootSetScope(GCHandle handle);
    GCGlobalRootSetScope(GCGlobalRootSetScope&& that) noexcept : GCGlobalRootSetScope(GCHandle::invalid()) {
        swap(*this, that);
    }
    GCGlobalRootSetScope& operator=(GCGlobalRootSetScope&& that) noexcept {
        auto tmp = std::move(that);
        swap(*this, tmp);
        return *this;
    }
    friend void swap(GCGlobalRootSetScope& first, GCGlobalRootSetScope& second) noexcept {
        using std::swap;
        swap(reinterpret_cast<GCStageScopeBase&>(first), reinterpret_cast<GCStageScopeBase&>(second));
        swap(first.globalRoots_, second.globalRoots_);
        swap(first.stableRoots_, second.stableRoots_);
    }
    ~GCGlobalRootSetScope();
    void addGlobalRoot() {
        requireValid();
        globalRoots_++;
    }
    void addStableRoot() {
        requireValid();
        stableRoots_++;
    }
};

class GCHandle::GCThreadRootSetScope : public GCStageScopeBase {
    mm::ThreadData& threadData_;
    uint64_t stackRoots_ = 0;
    uint64_t threadLocalRoots_ = 0;

public:
    explicit GCThreadRootSetScope(GCHandle handle, mm::ThreadData& threadData);
    GCThreadRootSetScope(GCThreadRootSetScope&& that) = default;
    GCThreadRootSetScope& operator=(GCThreadRootSetScope&& that) = delete;
    ~GCThreadRootSetScope();
    void addStackRoot() {
        requireValid();
        stackRoots_++;
    }
    void addThreadLocalRoot() {
        requireValid();
        threadLocalRoots_++;
    }
};

class GCHandle::GCMarkScope : public GCStageScopeBase {
    MarkStats stats_;

public:
    explicit GCMarkScope(GCHandle handle);
    GCMarkScope(GCMarkScope&& that) noexcept : GCMarkScope(GCHandle::invalid()) {
        swap(*this, that);
    }
    GCMarkScope& operator=(GCMarkScope&& that) noexcept {
        auto tmp = std::move(that);
        swap(*this, tmp);
        return *this;
    }
    friend void swap(GCMarkScope& first, GCMarkScope& second) noexcept {
        using std::swap;
        swap(reinterpret_cast<GCStageScopeBase&>(first), reinterpret_cast<GCStageScopeBase&>(second));
        swap(first.stats_, second.stats_);
    }
    ~GCMarkScope();

    void addObject() noexcept {
        requireValid();
        ++stats_.markedCount;
    }
};

class GCHandle::GCProcessWeaksScope : public GCStageScopeBase {
    uint64_t undisposedCount_ = 0;
    uint64_t aliveCount_ = 0;
    uint64_t nulledCount_ = 0;

public:
    explicit GCProcessWeaksScope(GCHandle handle) noexcept;
    GCProcessWeaksScope(GCProcessWeaksScope&& that) noexcept : GCProcessWeaksScope(GCHandle::invalid()) {
        swap(*this, that);
    }
    GCProcessWeaksScope& operator=(GCProcessWeaksScope&& that) noexcept {
        auto tmp = std::move(that);
        swap(*this, tmp);
        return *this;
    }
    friend void swap(GCProcessWeaksScope& first, GCProcessWeaksScope& second) noexcept {
        using std::swap;
        swap(reinterpret_cast<GCStageScopeBase&>(first), reinterpret_cast<GCStageScopeBase&>(second));
        swap(first.undisposedCount_, second.undisposedCount_);
        swap(first.aliveCount_, second.aliveCount_);
        swap(first.nulledCount_, second.nulledCount_);
    }
    ~GCProcessWeaksScope();

    void addUndisposed() noexcept {
        requireValid();
        ++undisposedCount_;
    }
    void addAlive() noexcept {
        requireValid();
        ++aliveCount_;
    }
    void addNulled() noexcept {
        requireValid();
        ++nulledCount_;
    }
};

}
