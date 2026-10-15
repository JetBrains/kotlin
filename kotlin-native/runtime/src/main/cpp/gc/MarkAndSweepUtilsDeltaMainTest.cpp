/*
 * Copyright 2010-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "gc/MarkAndSweepUtils.hpp"

#if defined(__aarch64__)

#include <array>
#include <cstddef>
#include <cstdint>
#include <unordered_map>
#include <unordered_set>
#include <vector>

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "DeltaMainStackMap.hpp"
#include "DeltaMainStackMapTestSupport.hpp"
#include "ObjectTestSupport.hpp"
#include "TestSupport.hpp"
#include "Utils.hpp"

using namespace kotlin;

namespace {

struct Payload {
    mm::RefField field;

    static constexpr std::array kFields = {
            &Payload::field,
    };
};

test_support::TypeInfoHolder typeHolder{test_support::TypeInfoHolder::ObjectBuilder<Payload>()};

class Object : public test_support::Object<Payload> {
public:
    Object() : test_support::Object<Payload>(typeHolder.typeInfo()) {}
};

// Records every root the walker hands to the marker, in order, so that a root scanned twice is visible.
class RecordingMarkTraits : private Pinned {
public:
    using MarkQueue = std::vector<ObjHeader*>;

    static constexpr auto kAllowHeapToStackRefs = true;

    RecordingMarkTraits() { instance_ = this; }
    ~RecordingMarkTraits() { instance_ = nullptr; }

    const std::vector<ObjHeader*>& enqueued() const { return enqueued_; }
    const std::unordered_set<ObjHeader*>& marked() const { return marked_; }

    static void clear(MarkQueue& queue) noexcept { queue.clear(); }

    static ObjHeader* tryDequeue(MarkQueue& queue) noexcept {
        if (queue.empty()) return nullptr;
        auto top = queue.back();
        queue.pop_back();
        return top;
    }

    static bool tryEnqueue(MarkQueue& queue, ObjHeader* object) noexcept {
        instance_->enqueued_.push_back(object);
        auto result = instance_->marked_.insert(object);
        if (result.second) {
            queue.push_back(object);
        }
        return result.second;
    }

    static bool tryMark(ObjHeader* object) noexcept { return instance_->marked_.insert(object).second; }

    static void processInMark(MarkQueue& markQueue, ObjHeader* object) noexcept {
        gc::internal::processObjectInMark<RecordingMarkTraits>(static_cast<void*>(&markQueue), object);
    }

private:
    static RecordingMarkTraits* instance_;

    std::vector<ObjHeader*> enqueued_;
    std::unordered_set<ObjHeader*> marked_;
};

// static
RecordingMarkTraits* RecordingMarkTraits::instance_ = nullptr;

// A stack frame as AArch64 lays it out for a function whose GC roots live in its locals: the address of
// the stack map record is at fp - 16, the locals are below it, the frame record ([saved fp, return address])
// is at fp. Root slot `i` is at `fp - 48 + 8 * i`, i.e. at the offsets -48, -40, -32, -24.
struct alignas(16) FakeFrame {
    static constexpr size_t kSlots = 4;

    ObjHeader* slots[kSlots];
    uint64_t record;
    uint64_t padding;
    uint64_t savedFp;
    uint64_t returnPc;

    uint64_t* fp() { return &savedFp; }
};
static_assert(sizeof(FakeFrame) % 16 == 0);
static_assert(offsetof(FakeFrame, savedFp) == (FakeFrame::kSlots + 2) * sizeof(ObjHeader*));
static_assert(offsetof(FakeFrame, record) == offsetof(FakeFrame, savedFp) - 16);

constexpr size_t kFunctionSize = 0x100;
constexpr size_t kCallOffset = 0x10;

// The fake pc "inside" frame number `i`: the return address a callee of that frame has stored.
uint64_t* pcInFrame(size_t i) {
    return reinterpret_cast<uint64_t*>(stackMap::test::FakeStackMapSection::code(kFunctionSize * i + kCallOffset));
}

// Frames[0] is the youngest (lowest address), frames[n - 1] the oldest. Frame i was called from frame i + 1.
void linkFrames(FakeFrame* frames, size_t count) {
    for (size_t i = 0; i < count; ++i) {
        for (auto& slot : frames[i].slots) slot = nullptr;
        frames[i].record = 0;
        frames[i].savedFp = i + 1 < count ? reinterpret_cast<uint64_t>(frames[i + 1].fp()) : 0;
        frames[i].returnPc = reinterpret_cast<uint64_t>(pcInFrame(i + 1));
    }
}

class MarkAndSweepUtilsDeltaMainTest : public ::testing::Test {
public:
    ~MarkAndSweepUtilsDeltaMainTest() {
        while (!thread().frameAnchors().empty()) {
            thread().popStackMapAnchor();
        }
        mm::GlobalData::Instance().gc().ClearForTests();
        mm::GlobalData::Instance().allocator().clearForTests();
    }

    mm::ThreadData& thread() { return *memoryInit_.memoryState()->GetThreadData(); }

    // The stack map says that the slots from `liveSet` hold GC roots; bit `i` is the slot at `baseOffset - 8 * i`.
    // By default, all four slots of the frame hold GC roots.
    void declareFrameMap(FakeFrame& frame, size_t frameIndex, uint8_t baseOffset = 0x68, uint8_t liveSet = 0b1111) {
        frame.record = section_.addRecord(kFunctionSize * frameIndex, kFunctionSize,
                stackMap::test::bytes({{baseOffset}, {32}, {liveSet}, {1}, {static_cast<uint8_t>(kCallOffset), 0}, {0}}));
    }

    void pushAnchor(FakeFrame& frame, size_t frameIndex) {
        thread().pushStackMapAnchor(mm::KotlinFrameAnchor(frame.fp(), pcInFrame(frameIndex)));
    }

    void walk() {
        auto handle = gc::GCHandle::create(epoch_++);
        auto stackMap = section_.map();
        gc::collectRootSetFromMapForThread<RecordingMarkTraits>(handle, queue_, stackMap, thread());
        handle.finished();
    }

    // Marks everything reachable from the roots that `walk` has found.
    void markReachable() {
        auto handle = gc::GCHandle::create(epoch_++);
        gc::Mark<RecordingMarkTraits>(handle, queue_);
        handle.finished();
    }

    const std::vector<ObjHeader*>& enqueued() const { return traits_.enqueued(); }
    const std::unordered_set<ObjHeader*>& marked() const { return traits_.marked(); }

    static auto Objects(std::initializer_list<std::reference_wrapper<Object>> objects) {
        std::vector<ObjHeader*> result;
        for (auto& object : objects) result.push_back(object.get().header());
        return testing::UnorderedElementsAreArray(result);
    }

private:
    uint64_t epoch_ = 0;
    kotlin::ScopedMemoryInit memoryInit_;
    RecordingMarkTraits traits_;
    RecordingMarkTraits::MarkQueue queue_;
    stackMap::test::FakeStackMapSection section_;
};

} // namespace

// --- KotlinFrameAnchor::next -------------------------------------------------------------------

TEST(KotlinFrameAnchorTest, NextFollowsTheFrameRecord) {
    FakeFrame frames[2];
    linkFrames(frames, 2);
    mm::KotlinFrameAnchor anchor(frames[0].fp(), pcInFrame(0));

    auto next = anchor.next();

    ASSERT_TRUE(next.has_value());
    EXPECT_EQ(next->fp, frames[1].fp());
    EXPECT_EQ(next->pc, pcInFrame(1));
}

TEST(KotlinFrameAnchorTest, NextOfTheOutermostFrameHasNullFpAndTheSavedPc) {
    FakeFrame frames[1];
    linkFrames(frames, 1);
    mm::KotlinFrameAnchor anchor(frames[0].fp(), pcInFrame(0));

    auto next = anchor.next();

    // The end of the chain is a valid link, not an error.
    ASSERT_TRUE(next.has_value());
    EXPECT_EQ(next->fp, nullptr);
    EXPECT_EQ(next->pc, pcInFrame(1));
}

TEST(KotlinFrameAnchorTest, NextRejectsLinkThatDoesNotGoUp) {
    FakeFrame frames[2];
    linkFrames(frames, 2);
    mm::KotlinFrameAnchor anchor(frames[1].fp(), pcInFrame(1));
    // The saved fp points back to the younger frame.
    frames[1].savedFp = reinterpret_cast<uint64_t>(frames[0].fp());

    EXPECT_FALSE(anchor.next().has_value());
}

TEST(KotlinFrameAnchorTest, NextRejectsLinkToItself) {
    FakeFrame frames[1];
    linkFrames(frames, 1);
    mm::KotlinFrameAnchor anchor(frames[0].fp(), pcInFrame(0));
    frames[0].savedFp = reinterpret_cast<uint64_t>(frames[0].fp());

    EXPECT_FALSE(anchor.next().has_value());
}

TEST(KotlinFrameAnchorTest, NextRejectsMisalignedLink) {
    FakeFrame frames[2];
    linkFrames(frames, 2);
    mm::KotlinFrameAnchor anchor(frames[0].fp(), pcInFrame(0));
    frames[0].savedFp = reinterpret_cast<uint64_t>(frames[1].fp()) + 8;

    EXPECT_FALSE(anchor.next().has_value());
}

TEST(KotlinFrameAnchorTest, MisalignedFpIsRejectedAtConstruction) {
    FakeFrame frames[1];
    linkFrames(frames, 1);

    EXPECT_DEATH(mm::KotlinFrameAnchor(frames[0].fp() + 1, pcInFrame(0)), "not aligned");
}

// --- collectRootSetFromMapForThread ------------------------------------------------------------

TEST_F(MarkAndSweepUtilsDeltaMainTest, NoAnchorsNoRoots) {
    walk();

    EXPECT_THAT(enqueued(), testing::IsEmpty());
}

TEST_F(MarkAndSweepUtilsDeltaMainTest, CollectsTheSlotsListedInTheMapAndSkipsNull) {
    Object a, b;
    FakeFrame frames[1];
    linkFrames(frames, 1);
    frames[0].slots[0] = a.header();
    frames[0].slots[3] = b.header();
    declareFrameMap(frames[0], 0);
    pushAnchor(frames[0], 0);

    walk();

    EXPECT_THAT(marked(), Objects({a, b}));
    EXPECT_EQ(enqueued().size(), 2u);
}

TEST_F(MarkAndSweepUtilsDeltaMainTest, OnlySlotsFromTheMapAreRoots) {
    Object a, notARoot;
    FakeFrame frames[1];
    linkFrames(frames, 1);
    frames[0].slots[0] = a.header();
    frames[0].slots[1] = notARoot.header();
    // The map lists only the first slot, which is at -48.
    declareFrameMap(frames[0], 0, 0x50, 0b1);
    pushAnchor(frames[0], 0);

    walk();

    EXPECT_THAT(marked(), Objects({a}));
}

TEST_F(MarkAndSweepUtilsDeltaMainTest, WalksFromTheAnchorTowardsTheOldestFrame) {
    Object a, b, c;
    FakeFrame frames[3];
    linkFrames(frames, 3);
    frames[0].slots[0] = a.header();
    frames[1].slots[0] = b.header();
    frames[2].slots[0] = c.header();
    for (size_t i = 0; i < 3; ++i) declareFrameMap(frames[i], i);
    pushAnchor(frames[0], 0);

    walk();

    EXPECT_THAT(marked(), Objects({a, b, c}));
    EXPECT_EQ(enqueued().size(), 3u);
}

TEST_F(MarkAndSweepUtilsDeltaMainTest, FramesBelowTheAnchorAreNotScanned) {
    Object younger, anchored;
    FakeFrame frames[2];
    linkFrames(frames, 2);
    frames[0].slots[0] = younger.header();
    frames[1].slots[0] = anchored.header();
    declareFrameMap(frames[0], 0);
    declareFrameMap(frames[1], 1);
    // The anchor is at frame 1: frame 0 is deeper than the anchor, it does not belong to this walk.
    pushAnchor(frames[1], 1);

    walk();

    EXPECT_THAT(marked(), Objects({anchored}));
}

// Kotlin -> runtime C++ -> Kotlin: the frame in the middle has no stack map. The Kotlin frame under it
// must still be scanned.
TEST_F(MarkAndSweepUtilsDeltaMainTest, StepsOverFramesWithoutMap) {
    Object upper, lower, ignored;
    FakeFrame frames[3];
    linkFrames(frames, 3);
    frames[0].slots[0] = upper.header();
    frames[1].slots[0] = ignored.header(); // a runtime frame: whatever is in its slots is not a root
    frames[2].slots[0] = lower.header();
    declareFrameMap(frames[0], 0);
    declareFrameMap(frames[2], 2);
    pushAnchor(frames[0], 0);

    walk();

    EXPECT_THAT(marked(), Objects({upper, lower}));
}

TEST_F(MarkAndSweepUtilsDeltaMainTest, SeveralAnchorsScanEveryFrameOnce) {
    Object a, b, c, d;
    FakeFrame frames[4];
    linkFrames(frames, 4);
    frames[0].slots[0] = a.header();
    frames[1].slots[0] = b.header();
    frames[2].slots[0] = c.header();
    frames[3].slots[0] = d.header();
    for (size_t i = 0; i < 4; ++i) declareFrameMap(frames[i], i);
    // Anchors are pushed oldest first, the last one is the youngest.
    pushAnchor(frames[2], 2);
    pushAnchor(frames[0], 0);

    walk();

    EXPECT_THAT(marked(), Objects({a, b, c, d}));
    // The younger anchor stops where the older one starts: no frame is scanned twice.
    EXPECT_EQ(enqueued().size(), 4u);
}

// An anchor that is pushed twice for the same frame (a nested guard in one function) must not make
// the walker scan that frame twice.
TEST_F(MarkAndSweepUtilsDeltaMainTest, AnchorsAtTheSameFrameScanItOnce) {
    Object a;
    FakeFrame frames[1];
    linkFrames(frames, 1);
    frames[0].slots[0] = a.header();
    declareFrameMap(frames[0], 0);
    pushAnchor(frames[0], 0);
    pushAnchor(frames[0], 0);

    walk();

    // The younger anchor stops at once, because its fp is not below the fp of the older one.
    EXPECT_THAT(marked(), Objects({a}));
    EXPECT_EQ(enqueued().size(), 1u);
}

// The frame of the anchor itself may be a runtime frame (no map): the walk must go on to its caller.
TEST_F(MarkAndSweepUtilsDeltaMainTest, AnchorFrameWithoutMapIsSkipped) {
    Object ignored, caller;
    FakeFrame frames[2];
    linkFrames(frames, 2);
    frames[0].slots[0] = ignored.header();
    frames[1].slots[0] = caller.header();
    declareFrameMap(frames[1], 1);
    pushAnchor(frames[0], 0);

    walk();

    EXPECT_THAT(marked(), Objects({caller}));
}

// A word left in the frame by a dead Kotlin frame can be a valid record address. The record then belongs to
// another function than the pc of the frame, so the frame is not scanned.
TEST_F(MarkAndSweepUtilsDeltaMainTest, FrameWithRecordOfAnotherFunctionIsSkipped) {
    Object ignored, caller;
    FakeFrame frames[3];
    linkFrames(frames, 3);
    frames[0].slots[0] = ignored.header();
    frames[2].slots[0] = caller.header();
    declareFrameMap(frames[1], 1);
    frames[0].record = frames[1].record;
    declareFrameMap(frames[2], 2);
    pushAnchor(frames[0], 0);

    walk();

    EXPECT_THAT(marked(), Objects({caller}));
}

TEST_F(MarkAndSweepUtilsDeltaMainTest, SameObjectInSeveralSlotsIsMarkedOnce) {
    Object a;
    FakeFrame frames[1];
    linkFrames(frames, 1);
    frames[0].slots[0] = a.header();
    frames[0].slots[2] = a.header();
    declareFrameMap(frames[0], 0);
    pushAnchor(frames[0], 0);

    walk();

    EXPECT_THAT(marked(), Objects({a}));
}

TEST_F(MarkAndSweepUtilsDeltaMainTest, ObjectsReachableFromRootsAreMarkedToo) {
    Object root, child;
    root->field = child.header();
    FakeFrame frames[1];
    linkFrames(frames, 1);
    frames[0].slots[0] = root.header();
    declareFrameMap(frames[0], 0);
    pushAnchor(frames[0], 0);

    walk();
    markReachable();

    EXPECT_THAT(marked(), Objects({root, child}));
}

TEST_F(MarkAndSweepUtilsDeltaMainTest, BrokenChainIsAFatalError) {
    Object a;
    FakeFrame frames[2];
    linkFrames(frames, 2);
    frames[0].slots[0] = a.header();
    declareFrameMap(frames[0], 0);
    // The frame record of the anchor frame points at itself.
    frames[0].savedFp = reinterpret_cast<uint64_t>(frames[0].fp());
    pushAnchor(frames[0], 0);

    EXPECT_DEATH(walk(), "Broken frame chain");
}

// The fake frames are on the real stack of the test thread, so the saved fp of the oldest frame can be
// made to point outside of [stackLow, stackHigh). The walker must not follow it.
TEST_F(MarkAndSweepUtilsDeltaMainTest, LinkOutsideOfTheStackIsAFatalError) {
    ASSERT_NE(thread().stackHigh(), nullptr) << "the bounds of the stack are unknown on this platform";
    Object a;
    FakeFrame frames[1];
    linkFrames(frames, 1);
    frames[0].slots[0] = a.header();
    declareFrameMap(frames[0], 0);
    // stackHigh is exclusive, so a record exactly there is already outside. Rounded up to keep the 16-byte alignment.
    frames[0].savedFp = (reinterpret_cast<uintptr_t>(thread().stackHigh()) + 15) & ~uintptr_t{15};
    pushAnchor(frames[0], 0);

    EXPECT_DEATH(walk(), "Broken frame chain");
}

#endif // defined(__aarch64__)
