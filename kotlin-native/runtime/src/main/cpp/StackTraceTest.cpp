/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "StackTrace.hpp"

#include <signal.h>

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "Common.h"
#include "Porting.h"
#include "TestSupport.hpp"

#include <iostream>

using namespace kotlin;

namespace {

template <typename Allocator = KonanAllocator<void*>>
NO_INLINE StackTrace<Allocator> GetStackTrace1(int skipFrames,
                                               int maxDepth = std::numeric_limits<int>::max(),
                                               const Allocator& allocator = Allocator()) {
    return StackTrace<Allocator>::current(skipFrames, maxDepth, allocator);
}

template <typename Allocator = KonanAllocator<void*>>
NO_INLINE StackTrace<Allocator> GetStackTrace2(int skipFrames,
                                               int maxDepth = std::numeric_limits<int>::max(),
                                               const Allocator& allocator = Allocator()) {
    return GetStackTrace1(skipFrames, maxDepth, allocator);
}

template <typename Allocator = KonanAllocator<void*>>
NO_INLINE StackTrace<Allocator> GetStackTrace3(int skipFrames,
                                               int maxDepth = std::numeric_limits<int>::max(),
                                               const Allocator& allocator = Allocator()) {
    return GetStackTrace2(skipFrames, maxDepth, allocator);
}

NO_INLINE void AbortWithStackTrace(int) {
    PrintStackTraceStderr();
    konan::abort();
}

} // namespace

TEST(StackTraceTest, StackTrace) {
    // TODO: Consider incorporating extra skipping to `GetCurrentStackTrace` on windows.
#if KONAN_WINDOWS
    constexpr int kSkip = 1;
#else
    constexpr int kSkip = 0;
#endif
    auto stackTrace = GetStackTrace2(kSkip);
    auto symbolicStackTrace = GetStackTraceStrings(stackTrace.data());
    ASSERT_GT(symbolicStackTrace.size(), 0ul);
    EXPECT_THAT(symbolicStackTrace[0], testing::HasSubstr("GetStackTrace1"));
}

TEST(StackTraceTest, StackTraceWithSkip) {
    // TODO: Consider incorporating extra skipping to `GetCurrentStackTrace` on windows.
#if KONAN_WINDOWS
    constexpr int kSkip = 2;
#else
    constexpr int kSkip = 1;
#endif
    auto stackTrace = GetStackTrace2(kSkip);
    auto symbolicStackTrace = GetStackTraceStrings(stackTrace.data());
    ASSERT_GT(symbolicStackTrace.size(), 0ul);
    EXPECT_THAT(symbolicStackTrace[0], testing::HasSubstr("GetStackTrace2"));
}

TEST(StackTraceTest, StackTraceWithMaxDepth) {
    // TODO: Consider incorporating extra skipping to `GetCurrentStackTrace` on windows.
#if KONAN_WINDOWS
    constexpr int kSkip = 1;
#else
    constexpr int kSkip = 0;
#endif
    auto stackTrace = GetStackTrace3(kSkip, 2);
    auto symbolicStackTrace = GetStackTraceStrings(stackTrace.data());
    ASSERT_EQ(symbolicStackTrace.size(), 2ul);
    EXPECT_THAT(symbolicStackTrace[0], testing::HasSubstr("GetStackTrace1"));
    EXPECT_THAT(symbolicStackTrace[1], testing::HasSubstr("GetStackTrace2"));
}

TEST(StackTraceTest, StackTraceWithSkipAndMaxDepth) {
    // TODO: Consider incorporating extra skipping to `GetCurrentStackTrace` on windows.
#if KONAN_WINDOWS
    constexpr int kSkip = 2;
#else
    constexpr int kSkip = 1;
#endif
    auto stackTrace = GetStackTrace3(kSkip, 2);
    auto symbolicStackTrace = GetStackTraceStrings(stackTrace.data());
    ASSERT_EQ(symbolicStackTrace.size(), 2ul);
    EXPECT_THAT(symbolicStackTrace[0], testing::HasSubstr("GetStackTrace2"));
    EXPECT_THAT(symbolicStackTrace[1], testing::HasSubstr("GetStackTrace3"));
}

TEST(StackTraceTest, StackAllocatedTrace) {
    // TODO: Consider incorporating extra skipping to `GetCurrentStackTrace` on windows.
#if KONAN_WINDOWS
    constexpr int kSkip = 1;
#else
    constexpr int kSkip = 0;
#endif

    StackBuffer<void*, 1> buffer;
    auto stackTrace = GetStackTrace2(kSkip, 1, buffer.allocator());
    auto symbolicStackTrace = GetStackTraceStrings(stackTrace.data());
    ASSERT_EQ(symbolicStackTrace.size(), 1ul);
    EXPECT_THAT(symbolicStackTrace[0], testing::HasSubstr("GetStackTrace1"));
}

TEST(StackTraceTest, StackAllocatedTraceWithSkip) {
    // TODO: Consider incorporating extra skipping to `GetCurrentStackTrace` on windows.
#if KONAN_WINDOWS
    constexpr int kSkip = 2;
#else
    constexpr int kSkip = 1;
#endif

    StackBuffer<void*, 1> buffer;
    auto stackTrace = GetStackTrace2(kSkip, 1, buffer.allocator());
    auto symbolicStackTrace = GetStackTraceStrings(stackTrace.data());
    ASSERT_EQ(symbolicStackTrace.size(), 1ul);
    EXPECT_THAT(symbolicStackTrace[0], testing::HasSubstr("GetStackTrace2"));
}

TEST(StackTraceTest, FailedStackAllocatedTrace) {
    // TODO: Consider incorporating extra skipping to `GetCurrentStackTrace` on windows.
#if KONAN_WINDOWS
    constexpr int kSkip = 1;
#else
    constexpr int kSkip = 0;
#endif

    StackBuffer<void*, 1> buffer;
    EXPECT_THROW(GetStackTrace2(kSkip, 2, buffer.allocator()), std::bad_array_new_length);
}

TEST(StackTraceTest, FailedStackAllocatedTraceWithSkip) {
    // TODO: Consider incorporating extra skipping to `GetCurrentStackTrace` on windows.
#if KONAN_WINDOWS
    constexpr int kSkip = 2;
#else
    constexpr int kSkip = 1;
#endif

    StackBuffer<void*, 1> buffer;
    EXPECT_THROW(GetStackTrace2(kSkip, 2, buffer.allocator()), std::bad_array_new_length);
}

TEST(StackTraceDeathTest, PrintStackTrace) {
    EXPECT_DEATH(
            { AbortWithStackTrace(0); },
            testing::AllOf(
                    testing::HasSubstr("AbortWithStackTrace"), testing::HasSubstr("StackTraceDeathTest_PrintStackTrace_Test"),
                    testing::Not(testing::HasSubstr("PrintStackTraceStderr"))));
}

TEST(StackTraceDeathTest, PrintStackTraceInSignalHandler) {
    EXPECT_DEATH(
            {
                signal(SIGINT, &AbortWithStackTrace);
                raise(SIGINT);
            },
            testing::AllOf(
                    testing::HasSubstr("AbortWithStackTrace"),
                    testing::HasSubstr("StackTraceDeathTest_PrintStackTraceInSignalHandler_Test"),
                    testing::Not(testing::HasSubstr("PrintStackTraceStderr"))));
}
