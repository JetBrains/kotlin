/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <atomic>
#include <cstdint>
#include <memory>

#include "ExtraObjectData.hpp"
#include "GCScheduler.hpp"
#include "concurrent/Mutex.hpp"
#include "ReferenceOps.hpp"
#include "RunLoopFinalizerProcessor.hpp"
#include "TypeLayout.hpp"
#include "Utils.hpp"

namespace kotlin {

namespace alloc {
class Allocator;
}

namespace mm {
class ThreadData;
}

namespace gc {

class GC : private Pinned {
public:
    class Impl;

    class ThreadData : private Pinned {
    public:
        class Impl;

        ThreadData(GC& gc, mm::ThreadData& threadData) noexcept;
        ~ThreadData();

        Impl& impl() noexcept { return *impl_; }

        void OnSuspendForGC() noexcept;

        void safePoint() noexcept;

        void onThreadRegistration() noexcept;

        void onThreadUnregistration() noexcept;

        void onAllocation(ObjHeader* object) noexcept;

    private:
        std::unique_ptr<Impl> impl_;
    };

    // Header to be placed before each heap object. GC will use this to keep its data if needed.
    // This is used via `type_layout::descriptor_t`, which is specialized below.
    // If GC doesn't need any data, it can make `size()` return 0 and `alignment()`
    // return 1.
    // Note: GC does not deinitialize `ObjectData`, so the implementations must ensure that
    //       the destructor is a trivial one.
    class ObjectData;

    GC(alloc::Allocator& allocator, gcScheduler::GCScheduler& gcScheduler) noexcept;
    ~GC();

    Impl& impl() noexcept { return *impl_; }

    void ClearForTests() noexcept;

    static void processObjectInMark(void* state, ObjHeader* object) noexcept;
    static void processArrayInMark(void* state, ArrayHeader* array) noexcept;

    // Requests that the next scheduled collection be a full (major) one. Meaningful only for
    // generational collectors; a no-op otherwise. Used by explicit GC.collect() so that it reclaims
    // the whole heap rather than just the young generation.
    void requestFullCollection() noexcept;

    // Observable per-scope statistics for generational collectors. All-zero for non-generational
    // collectors (which only ever run Full collections). Surfaced to Kotlin via GC.kt.
    struct GenerationalStats {
        uint64_t edenCollectionCount = 0;
        uint64_t fullCollectionCount = 0;
        uint64_t oldGenerationBaselineBytes = 0; // live bytes after the last Full collection
        uint64_t fullGrowthTriggerPercent = 0; // Eden->Full trigger; 0 when not generational
    };
    GenerationalStats generationalStats() noexcept;

    // Sets the live-heap growth percentage (over the post-Full baseline) that triggers the next Full
    // collection. Meaningful only for generational collectors; a no-op otherwise.
    void setFullGrowthTriggerPercent(uint64_t percent) noexcept;

    // TODO: These should exist only in the scheduler.
    int64_t Schedule() noexcept;
    void WaitFinished(int64_t epoch) noexcept;
    void WaitFinalizers(int64_t epoch) noexcept;

    auto gcLock() noexcept { return std::unique_lock{gcLock_}; }

    void onEpochFinalized(int64_t epoch) noexcept;

private:
    std::unique_ptr<Impl> impl_;
    ThreadStateAware<std::mutex> gcLock_{};
};

// `owner` is the heap object whose field/element is being overwritten (the container), or nullptr
// when it is unknown (static/global slots, some runtime-internal stores). The generational (gms)
// barrier uses it to record only genuine old->young edges; other collectors ignore it.
void beforeHeapRefUpdate(ObjHeader* owner, mm::DirectRefAccessor ref, ObjHeader* value, bool loadAtomic) noexcept;
OBJ_GETTER(weakRefReadBarrier, std_support::atomic_ref<ObjHeader*> weakReferee) noexcept;

bool isMarked(ObjHeader* object) noexcept;

// This will drop the mark bit if it was set and return `true`.
// If the mark bit was unset, this will return `false`.
bool tryResetMark(GC::ObjectData& objectData) noexcept;

namespace barriers {

class ExternalRCRefReleaseGuard : MoveOnly {
    class Impl;

public:
    static bool isNoop();

    ExternalRCRefReleaseGuard(mm::DirectRefAccessor ref) noexcept;
    ExternalRCRefReleaseGuard(ExternalRCRefReleaseGuard&& other) noexcept;
    ~ExternalRCRefReleaseGuard() noexcept;

    ExternalRCRefReleaseGuard& operator=(ExternalRCRefReleaseGuard&& other) noexcept;

private:
    FlatPImpl<Impl, 32> impl_;
};

} // namespace barriers

} // namespace gc

template <>
struct type_layout::descriptor<gc::GC::ObjectData> {
    struct type {
        using value_type = gc::GC::ObjectData;

        static uint64_t size() noexcept;
        static size_t alignment() noexcept;

        static value_type* construct(uint8_t* ptr) noexcept;
    };
};

} // namespace kotlin
