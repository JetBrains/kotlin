/*
 * Copyright 2010-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <atomic>
#include <optional>
#include <shared_mutex>

#include "Utils.hpp"
#include "GCStatistics.hpp"
#include "ReferenceOps.hpp"
#include "GC.hpp"
#include "RememberedSet.hpp"

/** See. `ConcurrentMark` */
namespace kotlin::gc::barriers {

class BarriersThreadData : private Pinned {
public:
    void onThreadRegistration() noexcept;
    void onThreadUnregistration() noexcept;

    void startMarkingNewObjects(GCHandle gcHandle) noexcept;
    void stopMarkingNewObjects() noexcept;
    bool shouldMarkNewObjects() const noexcept;

    void onAllocation(ObjHeader* allocated);

    // Per-thread generational remembered set (see RememberedSet). Populated by the write barrier,
    // drained by the GC as an Eden root source.
    RememberedSet& rememberedSet() noexcept { return rememberedSet_; }

private:
    std::optional<GCHandle::GCMarkScope> markHandle_{};
    RememberedSet rememberedSet_{};
};

// Enables/queries the generational write barrier (remembered-set recording). Enabled for the whole
// process lifetime by the gms GC constructor; when disabled the barrier fast path has no added cost.
void setGenerationalActive(bool active) noexcept;
bool generationalActive() noexcept;

// Marks the span of a collection (from STW mark start through the end of the -- possibly concurrent --
// sweep). Set/cleared by the collection driver via GmsGCTraits::onCollectionStart/onCollectionFinish.
// While true, the write barrier cannot trust an owner's "old" bit as a stable source-age classifier:
// the concurrent sweep promotes young survivors to old one object at a time, so a store into a
// soon-to-be-promoted owner could read it as young and drop a genuine old->young edge. During this
// window the barrier records by the value's age instead; the STW drain re-filters by container age
// once promotions have settled. See beforeHeapRefUpdate and the R3 concurrent-sweep audit.
void setCollectionInProgress(bool inProgress) noexcept;
bool collectionInProgress() noexcept;

// Forces the next collection to be Full. Set on remembered-set overflow (a Full collection does not
// need the remembered set) and on an explicit GC.collect(). `takeFullCollectionRequest` atomically
// reads and clears the request and must only be called while the world is stopped.
void requestFullCollection() noexcept;
bool takeFullCollectionRequest() noexcept;

// Test support: clear a leaked forced-Full latch between tests.
void clearFullCollectionRequestForTests() noexcept;

// Must be called during STW.
void enableBarriers(uint64_t epoch) noexcept;
void switchToWeakProcessingBarriers() noexcept;
void disableBarriers() noexcept;

// `owner` is the container object whose slot is being overwritten, or nullptr when unknown. With a
// known, stable owner the generational remembered set records only a genuine old->young edge (old
// owner, young value); unknown owners and in-collection stores use a conservative young-value fallback
// that the Eden drain re-filters by resolved container age. This is the JSC-style source-age filter:
// it keeps the SSB bounded so Eden can actually run.
void beforeHeapRefUpdate(ObjHeader* owner, mm::DirectRefAccessor ref, ObjHeader* value, bool loadAtomic) noexcept;

ObjHeader* weakRefReadBarrier(std_support::atomic_ref<ObjHeader*> weakReferee) noexcept;

class ExternalRCRefReleaseGuard::Impl : MoveOnly {
public:
    explicit Impl(mm::DirectRefAccessor ref) noexcept;
    Impl(Impl&& other) = default;
    ~Impl() = default;
    Impl& operator=(Impl&& other) = default;

private:
    std::shared_lock<RWSpinLock> markMutex_{};
    ThreadStateGuard stateGuard_{};
};

} // namespace kotlin::gc::barriers
