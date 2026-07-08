/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "GCImpl.hpp"

#include <memory>

#include "CompilerConstants.hpp"
#include "GC.hpp"
#include "GCStatistics.hpp"
#include "MarkAndSweepUtils.hpp"
#include "ObjectOps.hpp"

using namespace kotlin;

// NOTE: BarriersThreadData embeds the RememberedSet (RememberedSet.hpp), whose buffer is sized by
// kCapacity; this TU is recompiled when that capacity changes to keep allocation and bound in sync.
gc::GC::ThreadData::ThreadData(GC& gc, mm::ThreadData& threadData) noexcept : impl_(std::make_unique<Impl>(gc.impl().mark_, threadData)) {}

gc::GC::ThreadData::~ThreadData() = default;

void gc::GC::ThreadData::OnSuspendForGC() noexcept {
    impl_->mark_.onSuspendForGC();
}

void gc::GC::ThreadData::safePoint() noexcept {
    impl_->mark_.onSafePoint();
}

void gc::GC::ThreadData::onThreadRegistration() noexcept {
    impl_->barriers_.onThreadRegistration();
}

void gc::GC::ThreadData::onThreadUnregistration() noexcept {
    impl_->barriers_.onThreadUnregistration();
}

PERFORMANCE_INLINE void gc::GC::ThreadData::onAllocation(ObjHeader* object) noexcept {
    impl_->barriers_.onAllocation(object);
}

gc::GC::GC(alloc::Allocator& allocator, gcScheduler::GCScheduler& gcScheduler) noexcept :
    impl_(std::make_unique<Impl>(allocator, gcScheduler, compiler::gcMutatorsCooperate(), compiler::auxGCThreads())) {
    // Activate the generational write barrier (remembered-set recording) for the whole process
    // lifetime: the barrier must observe old->young stores that happen between collections.
    barriers::setGenerationalActive(true);
    RuntimeLogInfo({kTagGC}, "%s GC initialized", internal::GmsGCTraits::kName);
}

gc::GC::~GC() {
    impl_->state_.shutdown();
}

void gc::GC::ClearForTests() noexcept {
    gc::gms::resetForTests();
    barriers::clearFullCollectionRequestForTests();
    GCHandle::ClearForTests();
}

// static
PERFORMANCE_INLINE void gc::GC::processObjectInMark(void* state, ObjHeader* object) noexcept {
    gc::internal::processObjectInMark<gc::mark::ConcurrentMark::MarkTraits>(state, object);
}

// static
PERFORMANCE_INLINE void gc::GC::processArrayInMark(void* state, ArrayHeader* array) noexcept {
    gc::internal::processArrayInMark<gc::mark::ConcurrentMark::MarkTraits>(state, array);
}

void gc::GC::requestFullCollection() noexcept {
    barriers::requestFullCollection();
}

gc::GC::GenerationalStats gc::GC::generationalStats() noexcept {
    return GenerationalStats{
            gms::edenCollectionCount(),
            gms::fullCollectionCount(),
            gms::oldGenerationBaselineBytes(),
            gms::fullGrowthTriggerPercent(),
    };
}

void gc::GC::setFullGrowthTriggerPercent(uint64_t percent) noexcept {
    gms::setFullGrowthTriggerPercent(percent);
}

int64_t gc::GC::Schedule() noexcept {
    // Pick Eden vs Full per the heap-growth policy. A pending forced-Full request (explicit collect
    // or remembered-set overflow) is applied authoritatively during STW in GmsGCTraits::onCollectionStart.
    return impl_->state_.schedule(impl_->chooseCollectionScope());
}

void gc::GC::WaitFinished(int64_t epoch) noexcept {
    impl_->state_.waitEpochFinished(epoch);
}

void gc::GC::WaitFinalizers(int64_t epoch) noexcept {
    impl_->state_.waitEpochFinalized(epoch);
}

PERFORMANCE_INLINE void gc::beforeHeapRefUpdate(ObjHeader* owner, mm::DirectRefAccessor ref, ObjHeader* value, bool loadAtomic) noexcept {
    barriers::beforeHeapRefUpdate(owner, ref, value, loadAtomic);
}

PERFORMANCE_INLINE OBJ_GETTER(gc::weakRefReadBarrier, std_support::atomic_ref<ObjHeader*> weakReferee) noexcept {
    RETURN_OBJ(gc::barriers::weakRefReadBarrier(weakReferee));
}

PERFORMANCE_INLINE bool gc::isMarked(ObjHeader* object) noexcept {
    auto& objectData = alloc::objectDataForObject(object);
    if (objectData.marked()) {
        return true;
    }
    // During an Eden (minor) collection old-generation survivors are implicitly live: Eden never
    // traces them, so they carry no mark this cycle. Every liveness query at sweep time must still
    // report them alive. This is critical for the allocators that decide reclamation through
    // isMarked() -- the legacy/std allocator's object sweep (alloc/legacy/cpp/AllocatorImpl.cpp) and
    // its ExtraObjectData sweep (alloc/legacy/cpp/ObjectFactorySweep.hpp) -- otherwise a still-reachable
    // old object (and its weak references, associated Obj-C peer, monitor, or Cleaner) would be
    // wrongly reclaimed on the first Eden collection. Mirrors GmsProcessWeaksTraits::IsMarked.
    return gc::mark::edenCollection() && objectData.isOld();
}

// Invoked by the allocator's sweep for every swept object; returns true to KEEP the object, false to
// reclaim it. For the generational collector this is scope-aware:
//  - Eden: old survivors are kept untouched; young objects reached this cycle are kept and promoted
//    to old; unmarked young objects are reclaimed. (Dead old objects are retained as floating
//    garbage until the next Full collection.)
//  - Full: objects reached this cycle are kept (and promoted); everything else is reclaimed.
// When the generational scheduler is dormant, only Full collections run, so this matches CMS.
PERFORMANCE_INLINE bool gc::tryResetMark(GC::ObjectData& objectData) noexcept {
    if (mark::edenCollection()) {
        return objectData.edenSweepKeep();
    }
    return objectData.fullSweepKeep();
}

// During an Eden collection the sweep may skip pages that hold only old survivors and were not
// allocated into since the last collection: Eden neither traces nor reclaims the old generation, so a
// full-old page's outcome is identical to its previous sweep. A Full collection re-traces everything
// and must visit every page, so it never skips. See FixedBlockPage/NextFitPage::Sweep.
bool gc::sweepSkipsCleanOldPages() noexcept {
    return mark::edenCollection();
}

ALWAYS_INLINE bool gc::barriers::ExternalRCRefReleaseGuard::isNoop() {
    return false;
}
PERFORMANCE_INLINE gc::barriers::ExternalRCRefReleaseGuard::ExternalRCRefReleaseGuard(mm::DirectRefAccessor ref) noexcept : impl_(ref) {}
PERFORMANCE_INLINE gc::barriers::ExternalRCRefReleaseGuard::ExternalRCRefReleaseGuard(ExternalRCRefReleaseGuard&& other) noexcept = default;
PERFORMANCE_INLINE gc::barriers::ExternalRCRefReleaseGuard::~ExternalRCRefReleaseGuard() noexcept = default;
PERFORMANCE_INLINE gc::barriers::ExternalRCRefReleaseGuard& gc::barriers::ExternalRCRefReleaseGuard::ExternalRCRefReleaseGuard::operator=(
        ExternalRCRefReleaseGuard&&) noexcept = default;

// static
ALWAYS_INLINE uint64_t type_layout::descriptor<gc::GC::ObjectData>::type::size() noexcept {
    return sizeof(gc::GC::ObjectData);
}

// static
ALWAYS_INLINE size_t type_layout::descriptor<gc::GC::ObjectData>::type::alignment() noexcept {
    return alignof(gc::GC::ObjectData);
}

// static
ALWAYS_INLINE gc::GC::ObjectData* type_layout::descriptor<gc::GC::ObjectData>::type::construct(uint8_t* ptr) noexcept {
    return new (ptr) gc::GC::ObjectData();
}

void gc::GC::onEpochFinalized(int64_t epoch) noexcept {
    GCHandle::getByEpoch(epoch).finalizersDone();
    impl_->state_.finalized(epoch);
}
