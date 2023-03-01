/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "SafePointTracker.hpp"

#include "gmock/gmock.h"
#include "gtest/gtest.h"

using namespace kotlin;

// These tests require a stack trace to contain call site addresses but
// on Windows a trace contains function addresses instead.
// So skip these tests on Windows.
#if (__MINGW32__ || __MINGW64__)
#define SKIP_ON_WINDOWS() \
    do { \
        GTEST_SKIP() << "Skip on Windows"; \
    } while (false)
#else
#define SKIP_ON_WINDOWS() \
    do { \
    } while (false)
#endif

TEST(SafePointTrackerTest, RegisterSafePoints) {
    SKIP_ON_WINDOWS();
    []() OPTNONE {
        gcScheduler::internal::SafePointTracker<> tracker;

        for (size_t i = 0; i < 10; i++) {
            bool registered1 = tracker.registerCurrentSafePoint(0);
            bool registered2 = tracker.registerCurrentSafePoint(0);

            bool expected = (i == 0);

            EXPECT_THAT(registered1, expected);
            EXPECT_THAT(registered2, expected);
        }
    }();
}

template <size_t SafePointStackSize>
OPTNONE bool registerCurrentSafePoint(gcScheduler::internal::SafePointTracker<SafePointStackSize>& tracker) {
    return tracker.registerCurrentSafePoint(0);
}

TEST(SafePointTrackerTest, TrackTopFramesOnly) {
    SKIP_ON_WINDOWS();
    []() OPTNONE {
        gcScheduler::internal::SafePointTracker<16> longTracker;
        gcScheduler::internal::SafePointTracker<1> shortTracker;

        bool longRegistered1 = registerCurrentSafePoint(longTracker);
        bool longRegistered2 = registerCurrentSafePoint(longTracker);

        EXPECT_THAT(longRegistered1, true);
        EXPECT_THAT(longRegistered2, true);

        bool shortRegistered1 = registerCurrentSafePoint(shortTracker);
        bool shortRegistered2 = registerCurrentSafePoint(shortTracker);

        EXPECT_THAT(shortRegistered1, true);
        EXPECT_THAT(shortRegistered2, false);
    }();
}

TEST(SafePointTrackerTest, CleanOnSizeLimit) {
    SKIP_ON_WINDOWS();
    []() OPTNONE {
        gcScheduler::internal::SafePointTracker<> tracker(2);

        ASSERT_THAT(tracker.size(), 0);
        ASSERT_THAT(tracker.maxSize(), 2);

        for (size_t i = 0; i < 3; i++) {
            bool registered1 = tracker.registerCurrentSafePoint(0);

            EXPECT_THAT(registered1, true);
            EXPECT_THAT(tracker.size(), 1);

            bool registered2 = tracker.registerCurrentSafePoint(0);

            EXPECT_THAT(registered2, true);
            EXPECT_THAT(tracker.size(), 2);
        }
    }();
}
