/*
 * Copyright 2010-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include <atomic>

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "Allocator.hpp"
#include "AllocatorTestSupport.hpp"
#include "ExternalRCRefRegistry.hpp"
#include "GCImpl.hpp"
#include "GCState.hpp"
#include "GlobalData.hpp"
#include "GmsCollectionPolicy.hpp"
#include "ObjectData.hpp"
#include "ObjectOps.hpp"
#include "ObjectTestSupport.hpp"
#include "ReferenceOps.hpp"
#include "TestSupport.hpp"

using namespace kotlin;

namespace {

struct Payload {
    mm::RefField field1;
    mm::RefField field2;
    mm::RefField field3;

    static constexpr std::array kFields = {
            &Payload::field1,
            &Payload::field2,
            &Payload::field3,
    };
};

test_support::TypeInfoHolder typeHolder{test_support::TypeInfoHolder::ObjectBuilder<Payload>()};
test_support::TypeInfoHolder typeHolderWithFinalizer{test_support::TypeInfoHolder::ObjectBuilder<Payload>().addFlag(TF_HAS_FINALIZER)};

bool isOld(ObjHeader* object) noexcept {
    return alloc::objectDataForObject(object).isOld();
}

std::vector<ObjHeader*> alive(mm::ThreadData& threadData) {
    return alloc::test_support::allocatedObjects(threadData);
}

std::vector<mm::ExtraObjectData*> allocatedExtraObjects() {
    std::vector<mm::ExtraObjectData*> result;
    mm::GlobalData::Instance().allocator().TraverseAllocatedExtraObjects(
            [&](mm::ExtraObjectData* extraObject) { result.push_back(extraObject); });
    return result;
}

void collectForcing(gc::CollectionScope scope) {
    gc::gms::setForcedScopeForTests(true, scope);
    mm::GlobalData::Instance().gcScheduler().scheduleAndWaitFinalized();
}

class GmsGenerationalTest : public testing::Test {
public:
    ~GmsGenerationalTest() override {
        gc::gms::setForcedScopeForTests(false, gc::CollectionScope::Full);
        mm::GlobalsRegistry::Instance().ClearForTests();
        mm::ExternalRCRefRegistry::instance().clearForTests();
        mm::GlobalData::Instance().gc().ClearForTests();
        mm::GlobalData::Instance().allocator().clearForTests();
    }
};

} // namespace

TEST(GCStateTest, FullRequestAfterClaimedEdenGetsNextEpoch) {
    GCStateHolder state;

    auto edenEpoch = state.schedule(gc::CollectionScope::Eden);
    auto eden = state.waitScheduled();
    ASSERT_TRUE(eden.has_value());
    EXPECT_EQ(eden->epoch, edenEpoch);
    EXPECT_EQ(eden->scope, gc::CollectionScope::Eden);

    auto fullEpoch = state.schedule(gc::CollectionScope::Full);
    EXPECT_EQ(fullEpoch, edenEpoch + 1);

    state.finish(edenEpoch);
    auto full = state.waitScheduled();
    ASSERT_TRUE(full.has_value());
    EXPECT_EQ(full->epoch, fullEpoch);
    EXPECT_EQ(full->scope, gc::CollectionScope::Full);
}

TEST(GCStateTest, FullRequestBeforeClaimUpgradesPendingEden) {
    GCStateHolder state;

    auto edenEpoch = state.schedule(gc::CollectionScope::Eden);
    auto fullEpoch = state.schedule(gc::CollectionScope::Full);
    EXPECT_EQ(fullEpoch, edenEpoch);

    auto collection = state.waitScheduled();
    ASSERT_TRUE(collection.has_value());
    EXPECT_EQ(collection->epoch, edenEpoch);
    EXPECT_EQ(collection->scope, gc::CollectionScope::Full);
}

// A young object reachable from the roots is kept by an Eden collection and promoted to old.
TEST_F(GmsGenerationalTest, EdenPromotesReachableYoung) {
    RunInNewThread([](mm::ThreadData& threadData) {
        ObjHolder holder;
        mm::AllocateObject(&threadData, typeHolder.typeInfo(), holder.slot());
        ObjHeader* object = holder.obj();
        EXPECT_FALSE(isOld(object));

        collectForcing(gc::CollectionScope::Eden);

        EXPECT_THAT(alive(threadData), testing::Contains(object));
        EXPECT_TRUE(isOld(object));
    });
}

// An old object holding the only reference to a freshly allocated young object keeps that young
// object alive across an Eden collection, via the remembered set populated by the write barrier.
TEST_F(GmsGenerationalTest, OldToYoungKeptViaRememberedSet) {
    RunInNewThread([](mm::ThreadData& threadData) {
        ObjHolder oldHolder;
        mm::AllocateObject(&threadData, typeHolder.typeInfo(), oldHolder.slot());
        ObjHeader* oldObject = oldHolder.obj();

        // Promote oldObject to the old generation.
        collectForcing(gc::CollectionScope::Full);
        ASSERT_TRUE(isOld(oldObject));

        // Allocate a young object referenced only from oldObject's field.
        auto& rememberedSet = threadData.gc().impl().barriers_.rememberedSet();
        ObjHeader* youngObject = nullptr;
        {
            ObjHolder youngHolder;
            mm::AllocateObject(&threadData, typeHolder.typeInfo(), youngHolder.slot());
            youngObject = youngHolder.obj();
            // Heap store fires the generational write barrier, recording the old->young edge.
            auto& field = test_support::Object<Payload>::FromObjHeader(oldObject)->field1;
            mm::RefAccessor<false>{oldObject, field.ptr()}.store(youngObject);
        }
        ASSERT_FALSE(isOld(youngObject));
        ASSERT_FALSE(rememberedSet.empty());
        EXPECT_FALSE(rememberedSet.hasConservativeEntries()) << "known old-owner stores should use exact remembered-set entries";

        // Eden collection: youngObject must survive even though it is reachable only via an old object.
        collectForcing(gc::CollectionScope::Eden);

        EXPECT_THAT(alive(threadData), testing::Contains(youngObject));
        EXPECT_TRUE(isOld(youngObject)); // promoted after surviving
    });
}

TEST_F(GmsGenerationalTest, ResetForTestsClearsForcedScope) {
    gc::gms::setForcedScopeForTests(true, gc::CollectionScope::Eden);

    gc::gms::resetForTests();

    EXPECT_EQ(gc::gms::choosePolicyScope(), gc::CollectionScope::Full);
}

TEST_F(GmsGenerationalTest, PerformFullGCForcesFullCollection) {
    RunInNewThread([](MemoryState* memory) {
        collectForcing(gc::CollectionScope::Full);
        auto before = mm::GlobalData::Instance().gc().generationalStats();

        gc::gms::setForcedScopeForTests(true, gc::CollectionScope::Eden);
        PerformFullGC(memory);

        auto after = mm::GlobalData::Instance().gc().generationalStats();
        EXPECT_EQ(after.fullCollectionCount, before.fullCollectionCount + 1);
        EXPECT_EQ(after.edenCollectionCount, before.edenCollectionCount);
    });
}

TEST_F(GmsGenerationalTest, RememberedSetOnThreadExitForcesFullCollection) {
    ObjHeader* root = nullptr;
    ObjHeader* young = nullptr;

    RunInNewThread([&](mm::ThreadData& threadData) {
        mm::GlobalsRegistry::Instance().RegisterStorageForGlobal(&threadData, &root);
        mm::AllocateObject(&threadData, typeHolder.typeInfo(), &root);

        collectForcing(gc::CollectionScope::Full);
        ASSERT_TRUE(isOld(root));

        {
            ObjHolder youngHolder;
            mm::AllocateObject(&threadData, typeHolder.typeInfo(), youngHolder.slot());
            young = youngHolder.obj();
            ASSERT_FALSE(isOld(young));
            test_support::Object<Payload>::FromObjHeader(root)->field1 = young;
        }
        ASSERT_FALSE(threadData.gc().impl().barriers_.rememberedSet().empty());
    });

    RunInNewThread([&](mm::ThreadData& threadData) {
        auto before = mm::GlobalData::Instance().gc().generationalStats();
        gc::gms::setForcedScopeForTests(true, gc::CollectionScope::Eden);

        mm::GlobalData::Instance().gcScheduler().scheduleAndWaitFinalized();

        auto after = mm::GlobalData::Instance().gc().generationalStats();
        EXPECT_EQ(after.fullCollectionCount, before.fullCollectionCount + 1);
        EXPECT_EQ(after.edenCollectionCount, before.edenCollectionCount);
        EXPECT_THAT(alive(threadData), testing::Contains(young));
        EXPECT_TRUE(isOld(young));
    });
}

// A young object that becomes unreachable is reclaimed by an Eden collection.
TEST_F(GmsGenerationalTest, UnreachableYoungReclaimedByEden) {
    RunInNewThread([](mm::ThreadData& threadData) {
        ObjHeader* garbage = nullptr;
        {
            ObjHolder holder;
            mm::AllocateObject(&threadData, typeHolder.typeInfo(), holder.slot());
            garbage = holder.obj();
        } // holder released: `garbage` is now unreachable (roots are precise, not conservative).
        ASSERT_THAT(alive(threadData), testing::Contains(garbage));

        collectForcing(gc::CollectionScope::Eden);

        EXPECT_THAT(alive(threadData), testing::Not(testing::Contains(garbage)));
    });
}

// An ownerless young-value store (the C++ RefField helper path) records the slot in the remembered
// set, but if the container dies young the referent must NOT be resurrected/promoted by an Eden
// collection. This is the exact case the drain-time container-age filter guards against.
//
// The filter depends on the allocator being able to resolve a remembered-set slot back to its
// containing object (alloc::HeapLayoutSnapshot::containerOf). Only the custom allocator implements
// this; the legacy/std allocator returns nullptr, so the drain falls back to conservatively keeping
// the referent (safe, but it does resurrect dead-young garbage for one cycle). The precise
// non-resurrection invariant therefore only applies to allocators with interior-pointer resolution, so
// the test skips otherwise (it is exercised for real by gms_gc_custom_test).
TEST_F(GmsGenerationalTest, DeadYoungContainerDoesNotResurrectReferent) {
    bool resolvesInteriorPointers = false;
    RunInNewThread([&](mm::ThreadData& threadData) {
        ObjHeader* referent = nullptr;
        {
            ObjHolder containerHolder;
            mm::AllocateObject(&threadData, typeHolder.typeInfo(), containerHolder.slot());
            ObjHeader* container = containerHolder.obj();

            // Probe the live container to learn whether this allocator resolves interior pointers.
            {
                alloc::HeapLayoutSnapshot snapshot;
                resolvesInteriorPointers = snapshot.containerOf(container) != nullptr;
            }

            ObjHolder referentHolder;
            mm::AllocateObject(&threadData, typeHolder.typeInfo(), referentHolder.slot());
            referent = referentHolder.obj();

            // Ownerless young-value store: the write barrier records &container->field1 into the
            // remembered set, and the drain must reject it by container age.
            auto& rememberedSet = threadData.gc().impl().barriers_.rememberedSet();
            test_support::Object<Payload>::FromObjHeader(container)->field1 = referent;
            ASSERT_FALSE(rememberedSet.empty());
            EXPECT_TRUE(rememberedSet.hasConservativeEntries()) << "ownerless stores must stay conservative";
        } // both holders released: container and referent are now unreachable young garbage.
        ASSERT_FALSE(isOld(referent));

        // Eden drains the remembered set; the slot resolves to a *young* (dead) container, so its
        // referent is filtered out rather than kept and promoted.
        collectForcing(gc::CollectionScope::Eden);

        if (resolvesInteriorPointers) {
            EXPECT_THAT(alive(threadData), testing::Not(testing::Contains(referent)));
        }
    });
    if (!resolvesInteriorPointers) {
        GTEST_SKIP() << "Allocator does not resolve interior pointers, so the remembered-set drain "
                        "conservatively keeps the referent; the old->young filter is covered by "
                        "gms_gc_custom_test.";
    }
}

// A dead old object survives an Eden collection (floating garbage) but is reclaimed by a Full one.
TEST_F(GmsGenerationalTest, DeadOldReclaimedByFullNotEden) {
    RunInNewThread([](mm::ThreadData& threadData) {
        ObjHolder rootHolder;
        mm::AllocateObject(&threadData, typeHolder.typeInfo(), rootHolder.slot());
        ObjHeader* root = rootHolder.obj();

        // Allocate a second object referenced only from root->field1, then promote both to old.
        ObjHeader* victim = nullptr;
        {
            ObjHolder victimHolder;
            mm::AllocateObject(&threadData, typeHolder.typeInfo(), victimHolder.slot());
            victim = victimHolder.obj();
            test_support::Object<Payload>::FromObjHeader(root)->field1 = victim;
        }
        collectForcing(gc::CollectionScope::Full);
        ASSERT_TRUE(isOld(root));
        ASSERT_TRUE(isOld(victim));

        // Drop the only reference to victim; it is now dead old (floating) garbage.
        test_support::Object<Payload>::FromObjHeader(root)->field1 = nullptr;

        // Eden does not revisit old objects, so victim survives as floating garbage.
        collectForcing(gc::CollectionScope::Eden);
        EXPECT_THAT(alive(threadData), testing::Contains(victim));

        // Full re-traces everything and reclaims it.
        collectForcing(gc::CollectionScope::Full);
        EXPECT_THAT(alive(threadData), testing::Not(testing::Contains(victim)));
    });
}

// Sticky-mark integrity across many Eden collections (plan M6 case 4). An object promoted to old by a
// Full collection must stay alive and old across an arbitrary number of subsequent Eden collections:
// Eden never revisits the old generation, so the sticky "old" bit must persist and the object must
// never be reclaimed or demoted. A regression here (e.g. an Eden that clears old marks or re-traces
// old objects) would silently break the whole point of the generational split.
TEST_F(GmsGenerationalTest, StickyOldSurvivesManyEdenCollections) {
    RunInNewThread([](mm::ThreadData& threadData) {
        ObjHolder holder;
        mm::AllocateObject(&threadData, typeHolder.typeInfo(), holder.slot());
        ObjHeader* object = holder.obj();

        // Promote to old.
        collectForcing(gc::CollectionScope::Full);
        ASSERT_TRUE(isOld(object));

        // Many Eden collections must neither reclaim nor demote the old survivor.
        for (int i = 0; i < 16; ++i) {
            collectForcing(gc::CollectionScope::Eden);
            ASSERT_THAT(alive(threadData), testing::Contains(object)) << "Eden collection #" << i << " reclaimed an old survivor";
            ASSERT_TRUE(isOld(object)) << "Eden collection #" << i << " demoted an old survivor";
        }
    });
}

// R2 clean-old-page skip. Many old survivors spanning multiple allocator pages must all be retained
// across repeated Eden collections, while young garbage allocated between cycles is still reclaimed.
// During Eden the allocator skips sweeping pages that hold only old survivors and were not allocated
// into since the last collection (see FixedBlockPage/NextFitPage::Sweep); a bug in the skip predicate
// or its re-report would silently drop an old survivor here or fail to reclaim the young garbage.
TEST_F(GmsGenerationalTest, EdenSkipsManyOldPagesWhileReclaimingYoung) {
    RunInNewThread([](mm::ThreadData& threadData) {
        constexpr int kOld = 500;
        // Root the whole set from a single holder as a `field1` chain (ObjHolders must nest LIFO on the
        // stack, so we cannot keep 500 of them; the chain keeps every node reachable from `head`).
        ObjHolder rootHolder;
        mm::AllocateObject(&threadData, typeHolder.typeInfo(), rootHolder.slot());
        ObjHeader* head = rootHolder.obj();
        std::vector<ObjHeader*> oldObjects{head};
        ObjHeader* prev = head;
        for (int i = 1; i < kOld; ++i) {
            ObjHolder tmp;
            mm::AllocateObject(&threadData, typeHolder.typeInfo(), tmp.slot());
            ObjHeader* node = tmp.obj();
            // Link into the chain before `tmp` releases, so `node` stays reachable via head->...->field1.
            test_support::Object<Payload>::FromObjHeader(prev)->field1 = node;
            oldObjects.push_back(node);
            prev = node;
        }

        // Promote all of them to old; they span many clean-old pages after this Full sweep.
        collectForcing(gc::CollectionScope::Full);
        for (auto* o : oldObjects) ASSERT_TRUE(isOld(o));

        // Repeated Eden cycles: the old pages are skipped (clean-old) yet every survivor is retained,
        // and the young garbage allocated each cycle (on dirty pages that are still swept) is reclaimed.
        for (int cycle = 0; cycle < 8; ++cycle) {
            ObjHeader* garbage = nullptr;
            {
                ObjHolder g;
                mm::AllocateObject(&threadData, typeHolder.typeInfo(), g.slot());
                garbage = g.obj();
            }
            collectForcing(gc::CollectionScope::Eden);
            auto living = alive(threadData);
            for (auto* o : oldObjects) ASSERT_THAT(living, testing::Contains(o)) << "Eden cycle #" << cycle << " reclaimed an old survivor";
            EXPECT_THAT(living, testing::Not(testing::Contains(garbage))) << "Eden cycle #" << cycle << " leaked young garbage";
        }
    });
}

// Extra-object pages must not use the clean-old object-page shortcut. A finalizable object can make
// its ExtraObjectData sweepable during an Eden object sweep without allocating into the extra-object
// page; if that page is skipped, stale metadata survives until a later Full collection.
TEST_F(GmsGenerationalTest, EdenSweepsExtraObjectPageAfterFinalizerStateChange) {
    if (!alloc::heapLayoutResolvesInteriorPointers()) {
        GTEST_SKIP() << "The fixed extra-object page skip exists only in the custom allocator.";
    }

    RunInNewThread([](mm::ThreadData& threadData) {
        mm::ExtraObjectData* extraObject = nullptr;
        {
            ObjHolder holder;
            mm::AllocateObject(&threadData, typeHolderWithFinalizer.typeInfo(), holder.slot());
            extraObject = mm::ExtraObjectData::Get(holder.obj());
            ASSERT_NE(extraObject, nullptr);
        }

        // First Eden: the dead young object is retained for finalization, and the extra-object page is
        // swept as kept, making it look skippable for the next Eden collection.
        collectForcing(gc::CollectionScope::Eden);
        EXPECT_THAT(allocatedExtraObjects(), testing::Contains(extraObject));

        // Second Eden: object sweep observes that finalization completed and marks the ExtraObjectData
        // sweepable. The same collection must sweep the extra-object page instead of reusing the cached
        // kept result from the previous Eden.
        collectForcing(gc::CollectionScope::Eden);
        EXPECT_THAT(allocatedExtraObjects(), testing::Not(testing::Contains(extraObject)));
    });
}

// Eden weak processing, live-old case (plan M6 case 5). A weak reference to a live *old* referent must
// be retained by an Eden collection. Eden does not mark old objects (they carry no mark this cycle),
// so weak processing must treat old survivors as alive; otherwise a weak to a perfectly live old
// object would be spuriously nulled on the first Eden collection. Once the old referent later dies, a
// Full collection must null the weak -- so the weak (and its extra object) are reclaimed by the GC
// rather than by manual teardown, keeping the test allocator-agnostic.
TEST_F(GmsGenerationalTest, EdenRetainsWeakReferenceToOldReferent) {
    RunInNewThread([](mm::ThreadData& threadData) {
        ObjHolder weakHolder;
        test_support::RegularWeakReferenceImpl* weak = nullptr;
        {
            ObjHolder refereeHolder;
            mm::AllocateObject(&threadData, typeHolder.typeInfo(), refereeHolder.slot());
            ObjHeader* referee = refereeHolder.obj();

            // Promote the referee to old; it stays strongly reachable via refereeHolder.
            collectForcing(gc::CollectionScope::Full);
            ASSERT_TRUE(isOld(referee));

            weak = &test_support::InstallWeakReference(threadData, referee, weakHolder.slot());
            ASSERT_EQ(weak->get(), referee);

            collectForcing(gc::CollectionScope::Eden);

            // The old referent is alive, so Eden weak processing must keep the weak intact.
            EXPECT_EQ(weak->get(), referee);
        } // refereeHolder released: the old referent is now unreachable except through the weak.

        // A Full collection reclaims the dead old referent and nulls the weak.
        collectForcing(gc::CollectionScope::Full);
        EXPECT_EQ(weak->get(), nullptr);
    });
}

// gms Finding #3 (runtime seam). Volatile/atomic reference stores must thread the container ("owner")
// to the write barrier just like plain field stores, so the generational remembered set applies its
// source-age filter (shouldRememberOldToYoung) to them too. This is the runtime contract the codegen
// change feeds: an owner-carrying atomic store into an OLD owner records the old->young edge, while an
// atomic store into a YOUNG owner is filtered out and NOT recorded -- the fix that stops young->young
// atomic churn from flooding the fixed-capacity SSB and forcing a Full collection every cycle. (The
// codegen half -- that konanc actually passes the owner into these intrinsics -- is not reachable from
// this C++ model; it is covered end to end by the AtomicReference A/B benchmark.)
TEST_F(GmsGenerationalTest, AtomicStoreThreadsOwnerThroughRememberedSetFilter) {
    RunInNewThread([](mm::ThreadData& threadData) {
        auto& rememberedSet = threadData.gc().impl().barriers_.rememberedSet();

        // Old owner: an owner-carrying atomic old->young store must be recorded in the remembered set.
        ObjHolder oldHolder;
        mm::AllocateObject(&threadData, typeHolder.typeInfo(), oldHolder.slot());
        ObjHeader* oldOwner = oldHolder.obj();
        collectForcing(gc::CollectionScope::Full);
        ASSERT_TRUE(isOld(oldOwner));

        ObjHolder youngHolder;
        mm::AllocateObject(&threadData, typeHolder.typeInfo(), youngHolder.slot());
        ObjHeader* youngValue = youngHolder.obj();
        ASSERT_FALSE(isOld(youngValue));

        auto& oldField = test_support::Object<Payload>::FromObjHeader(oldOwner)->field1;
        size_t before = rememberedSet.size();
        mm::RefAccessor<false>{oldOwner, oldField.ptr()}.storeAtomic(youngValue, std::memory_order_seq_cst);
        EXPECT_EQ(rememberedSet.size(), before + 1) << "old->young atomic store must be remembered";
        EXPECT_FALSE(rememberedSet.hasConservativeEntries()) << "known old-owner atomic store should be exact";
        EXPECT_EQ(oldField.accessor().load(), youngValue) << "atomic store must apply the value";

        // Young owner: an owner-carrying atomic young->young store must be filtered out (SSB not grown).
        ObjHolder youngOwnerHolder;
        mm::AllocateObject(&threadData, typeHolder.typeInfo(), youngOwnerHolder.slot());
        ObjHeader* youngOwner = youngOwnerHolder.obj();
        ASSERT_FALSE(isOld(youngOwner));

        ObjHolder young2Holder;
        mm::AllocateObject(&threadData, typeHolder.typeInfo(), young2Holder.slot());
        ObjHeader* youngValue2 = young2Holder.obj();

        auto& youngField = test_support::Object<Payload>::FromObjHeader(youngOwner)->field1;
        before = rememberedSet.size();
        mm::RefAccessor<false>{youngOwner, youngField.ptr()}.storeAtomic(youngValue2, std::memory_order_seq_cst);
        EXPECT_EQ(rememberedSet.size(), before) << "young->young atomic store must be filtered, not recorded";
        EXPECT_EQ(youngField.accessor().load(), youngValue2) << "atomic store must still apply the value";
    });
}

// Eden weak processing, dead-young case (plan M6 case 5). A weak reference to a young referent that has
// become unreachable must be nulled by an Eden collection, which reclaims dead young objects. The weak
// reference object itself stays rooted and survives; only its referent is cleared.
TEST_F(GmsGenerationalTest, EdenClearsWeakReferenceToDeadYoungReferent) {
    RunInNewThread([](mm::ThreadData& threadData) {
        ObjHolder weakHolder;
        test_support::RegularWeakReferenceImpl* weak = nullptr;
        {
            ObjHolder refereeHolder;
            mm::AllocateObject(&threadData, typeHolder.typeInfo(), refereeHolder.slot());
            ObjHeader* referee = refereeHolder.obj();
            ASSERT_FALSE(isOld(referee));
            weak = &test_support::InstallWeakReference(threadData, referee, weakHolder.slot());
            ASSERT_EQ(weak->get(), referee);
        } // refereeHolder released: the referent is now unreachable except through the weak reference.

        collectForcing(gc::CollectionScope::Eden);

        // The young referent is dead, so Eden must null the weak reference.
        EXPECT_EQ(weak->get(), nullptr);
    });
}
