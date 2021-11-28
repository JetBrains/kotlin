/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ShadowStack.hpp"

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "Memory.h"
#include "Types.h"
#include "Utils.hpp"

using namespace kotlin;

namespace {

template <size_t ParametersCount, size_t LocalsCount>
class StackEntry : private Pinned {
public:
    static_assert(ParametersCount + LocalsCount > 0, "Must have at least 1 object on stack");

    explicit StackEntry(mm::ShadowStack& shadowStack) : shadowStack_(shadowStack), value_(make_unique<ObjHeader>()) {
        // Fill `locals_` with some values.
        for (size_t i = 0; i < LocalsCount; ++i) {
            (*this)[i] = value_.get() + i;
        }

        shadowStack_.EnterFrame(data_.data(), ParametersCount, kTotalCount);
    }

    ~StackEntry() { shadowStack_.LeaveFrame(data_.data(), ParametersCount, kTotalCount); }

    ObjHeader*& operator[](size_t index) { return data_[kFrameOverlayCount + ParametersCount + index]; }

private:
    mm::ShadowStack& shadowStack_;
    KStdUniquePtr<ObjHeader> value_;

    // The following is what the compiler creates on the stack.
    static inline constexpr int kFrameOverlayCount = sizeof(FrameOverlay) / sizeof(ObjHeader**);
    static inline constexpr int kTotalCount = kFrameOverlayCount + ParametersCount + LocalsCount;
    std::array<ObjHeader*, kTotalCount> data_;
};

KStdVector<ObjHeader*> Collect(mm::ShadowStack& shadowStack) {
    KStdVector<ObjHeader*> result;
    for (ObjHeader* local : shadowStack) {
        result.push_back(local);
    }
    return result;
}

} // namespace

TEST(ShadowStackTest, Empty) {
    mm::ShadowStack shadowStack;

    auto actual = Collect(shadowStack);

    EXPECT_THAT(actual, testing::IsEmpty());
}

TEST(ShadowStackTest, OneLocal) {
    mm::ShadowStack shadowStack;
    StackEntry<0, 1> frame1(shadowStack);

    auto actual = Collect(shadowStack);

    EXPECT_THAT(actual, testing::ElementsAre(frame1[0]));
}

TEST(ShadowStackTest, ThreeLocals) {
    mm::ShadowStack shadowStack;
    StackEntry<0, 3> frame1(shadowStack);

    auto actual = Collect(shadowStack);

    EXPECT_THAT(actual, testing::ElementsAre(frame1[0], frame1[1], frame1[2]));
}

TEST(ShadowStackTest, OneParameter) {
    mm::ShadowStack shadowStack;
    StackEntry<1, 0> frame1(shadowStack);

    auto actual = Collect(shadowStack);

    EXPECT_THAT(actual, testing::IsEmpty());
}

TEST(ShadowStackTest, ThreeLocalsAndOneParameter) {
    mm::ShadowStack shadowStack;
    StackEntry<1, 3> frame1(shadowStack);

    auto actual = Collect(shadowStack);

    EXPECT_THAT(actual, testing::ElementsAre(frame1[0], frame1[1], frame1[2]));
}

TEST(ShadowStackTest, TwoStackFrames) {
    mm::ShadowStack shadowStack;
    StackEntry<1, 3> frame1(shadowStack);
    StackEntry<1, 3> frame2(shadowStack);

    auto actual = Collect(shadowStack);

    EXPECT_THAT(actual, testing::ElementsAre(frame2[0], frame2[1], frame2[2], frame1[0], frame1[1], frame1[2]));
}

TEST(ShadowStackTest, ManyStackFrames) {
    mm::ShadowStack shadowStack;
    StackEntry<0, 3> frame1(shadowStack);
    StackEntry<1, 0> frame2(shadowStack);
    StackEntry<3, 1> frame3(shadowStack);
    StackEntry<3, 3> frame4(shadowStack);

    auto actual = Collect(shadowStack);

    EXPECT_THAT(actual, testing::ElementsAre(frame4[0], frame4[1], frame4[2], frame3[0], frame1[0], frame1[1], frame1[2]));
}
