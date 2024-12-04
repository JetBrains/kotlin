/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "StackTrace.hpp"

#include <cstdlib>
#include <signal.h>
#include <unordered_set>

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "Common.h"
#include "Porting.h"
#include "TestSupport.hpp"

using namespace kotlin;

using testing::Not;

namespace {

// Disable optimizations for these functions to avoid inlining and tail recursion optimization.
template <size_t Capacity = kDynamicCapacity>
OPTNONE StackTrace<Capacity> GetStackTrace1(size_t skipFrames = 0, size_t depth = StackTrace<Capacity>::maxDepth) {
    return StackTrace<Capacity>::current(skipFrames, depth);
}

template <size_t Capacity = kDynamicCapacity>
OPTNONE StackTrace<Capacity> GetStackTrace2(size_t skipFrames = 0, size_t depth = StackTrace<Capacity>::maxDepth) {
    return GetStackTrace1<Capacity>(skipFrames, depth);
}

template <size_t Capacity = kDynamicCapacity>
OPTNONE StackTrace<Capacity> GetStackTrace3(size_t skipFrames = 0, size_t depth = StackTrace<Capacity>::maxDepth) {
    return GetStackTrace2<Capacity>(skipFrames, depth);
}

template <size_t Capacity = kDynamicCapacity>
OPTNONE StackTrace<Capacity> GetDeepStackTrace(size_t depth) {
    if (depth <= 1) {
        return StackTrace<Capacity>::current();
    } else {
        return GetDeepStackTrace<Capacity>(depth - 1);
    }
}

NO_INLINE void AbortWithStackTrace(int) {
    PrintStackTraceStderr();
    std::abort();
}

} // namespace

TEST(StackTraceTest, StackTrace) {
    auto stackTrace = GetStackTrace3();
    auto symbolicStackTrace = GetStackTraceStrings(stackTrace.data());
    ASSERT_GT(symbolicStackTrace.size(), 1ul);
    EXPECT_THAT(symbolicStackTrace[0], testing::HasSubstr("GetStackTrace1"));
    EXPECT_THAT(symbolicStackTrace[1], testing::HasSubstr("GetStackTrace2"));
}

TEST(StackTraceTest, StackTraceWithSkip) {
    constexpr int kSkip = 1;
    auto stackTrace = GetStackTrace3(kSkip);
    auto symbolicStackTrace = GetStackTraceStrings(stackTrace.data());
    ASSERT_GT(symbolicStackTrace.size(), 1ul);
    EXPECT_THAT(symbolicStackTrace[0], testing::HasSubstr("GetStackTrace2"));
    EXPECT_THAT(symbolicStackTrace[1], testing::HasSubstr("GetStackTrace3"));
}

TEST(StackTraceTest, StackTraceWithLimitedDepth) {
    auto stackTrace = GetStackTrace3(/* skipFrames = */ 0, /* depth = */ 2);
    auto symbolicStackTrace = GetStackTraceStrings(stackTrace.data());
    ASSERT_EQ(symbolicStackTrace.size(), 2ul);
    EXPECT_THAT(symbolicStackTrace[0], testing::HasSubstr("GetStackTrace1"));
    EXPECT_THAT(symbolicStackTrace[1], testing::HasSubstr("GetStackTrace2"));

    stackTrace = GetStackTrace3(/* skipFrames = */ 1, /* depth = */ 2);
    symbolicStackTrace = GetStackTraceStrings(stackTrace.data());
    ASSERT_EQ(symbolicStackTrace.size(), 2ul);
    EXPECT_THAT(symbolicStackTrace[0], testing::HasSubstr("GetStackTrace2"));
    EXPECT_THAT(symbolicStackTrace[1], testing::HasSubstr("GetStackTrace3"));
}

TEST(StackTraceTest, StackAllocatedTrace) {
    auto stackTrace = GetStackTrace3<2>();
    auto symbolicStackTrace = GetStackTraceStrings(stackTrace.data());
    ASSERT_EQ(symbolicStackTrace.size(), 2ul);
    EXPECT_THAT(symbolicStackTrace[0], testing::HasSubstr("GetStackTrace1"));
    EXPECT_THAT(symbolicStackTrace[1], testing::HasSubstr("GetStackTrace2"));
}

TEST(StackTraceTest, StackAllocatedTraceWithSkip) {
    constexpr int kSkip = 1;
    auto stackTrace = GetStackTrace3<2>(kSkip);
    auto symbolicStackTrace = GetStackTraceStrings(stackTrace.data());
    ASSERT_EQ(symbolicStackTrace.size(), 2ul);
    EXPECT_THAT(symbolicStackTrace[0], testing::HasSubstr("GetStackTrace2"));
    EXPECT_THAT(symbolicStackTrace[1], testing::HasSubstr("GetStackTrace3"));
}

TEST(StackTraceTest, StackAllocatedTraceWithLimitedDepth) {
    auto stackTrace = GetStackTrace3<10>(/* skipFrames = */ 0, /* depth = */ 2);
    auto symbolicStackTrace = GetStackTraceStrings(stackTrace.data());
    ASSERT_EQ(symbolicStackTrace.size(), 2ul);
    EXPECT_THAT(symbolicStackTrace[0], testing::HasSubstr("GetStackTrace1"));
    EXPECT_THAT(symbolicStackTrace[1], testing::HasSubstr("GetStackTrace2"));

    stackTrace = GetStackTrace3<10>(/* skipFrames = */ 1, /* depth = */ 2);
    symbolicStackTrace = GetStackTraceStrings(stackTrace.data());
    ASSERT_EQ(symbolicStackTrace.size(), 2ul);
    EXPECT_THAT(symbolicStackTrace[0], testing::HasSubstr("GetStackTrace2"));
    EXPECT_THAT(symbolicStackTrace[1], testing::HasSubstr("GetStackTrace3"));
}

TEST(StackTraceTest, EmptyStackTrace) {
    constexpr size_t kSkip = 1000000;
    auto stackTrace = GetStackTrace1(kSkip);
    EXPECT_EQ(stackTrace.size(), 0ul);
    auto data = stackTrace.data();
    EXPECT_EQ(data.size(), 0ul);
    auto symbolicStackTrace = GetStackTraceStrings(data);
    EXPECT_EQ(stackTrace.size(), 0ul);
    EXPECT_EQ(symbolicStackTrace.size(), 0ul);
}

TEST(StackTraceTest, StackAllocatedEmptyTrace) {
    constexpr size_t kSkip = 1000000;
    auto stackTrace = GetStackTrace1<1>(kSkip);
    EXPECT_EQ(stackTrace.size(), 0ul);
    auto data = stackTrace.data();
    EXPECT_EQ(data.size(), 0ul);
    auto symbolicStackTrace = GetStackTraceStrings(data);
    EXPECT_EQ(stackTrace.size(), 0ul);
    EXPECT_EQ(symbolicStackTrace.size(), 0ul);
}

TEST(StackTraceTest, DeepStackTrace) {
    constexpr size_t knownStackDepth = 150;
    auto stackTrace = GetDeepStackTrace(knownStackDepth);
    auto symbolicStackTrace = GetStackTraceStrings(stackTrace.data());

#if USE_GCC_UNWIND || USE_WINAPI_UNWIND
    EXPECT_GE(stackTrace.size(), knownStackDepth);
    size_t lastKnownIndex = knownStackDepth - 1;
#else
    // For platforms where the libc unwind is used (e.g. MacOS) the size of a collected trace is limited (see StackTrace::maxDepth).
    EXPECT_EQ(stackTrace.size(), StackTrace<>::maxDepth);
    size_t lastKnownIndex = StackTrace<>::maxDepth - 1;
#endif

    ASSERT_GT(symbolicStackTrace.size(), lastKnownIndex);
    EXPECT_THAT(symbolicStackTrace[0], testing::HasSubstr("GetDeepStackTrace"));
    EXPECT_THAT(symbolicStackTrace[lastKnownIndex], testing::HasSubstr("GetDeepStackTrace"));
}

TEST(StackTraceTest, StackAllocatedDeepTrace) {
    constexpr size_t knownStackDepth = 100;
    constexpr size_t capacity = 10;
    auto stackTrace = GetDeepStackTrace<capacity>(knownStackDepth);
    auto symbolicStackTrace = GetStackTraceStrings(stackTrace.data());

    EXPECT_EQ(StackTrace<capacity>::maxDepth, capacity);
    EXPECT_EQ(stackTrace.size(), capacity);

    ASSERT_EQ(symbolicStackTrace.size(), capacity);
    EXPECT_THAT(symbolicStackTrace[0], testing::HasSubstr("GetDeepStackTrace"));
    EXPECT_THAT(symbolicStackTrace[capacity - 1], testing::HasSubstr("GetDeepStackTrace"));
}

TEST(StackTraceTest, StackAllocatedDeepTraceWithEnoughCapacity) {
    constexpr size_t knownStackDepth = 100;
    constexpr size_t capacity = 150;
    auto stackTrace = GetDeepStackTrace<capacity>(knownStackDepth);

#if USE_GCC_UNWIND || USE_WINAPI_UNWIND
    EXPECT_GE(stackTrace.size(), knownStackDepth);
#else
    // For platforms where the libc unwind is used (e.g. MacOS) the size of a collected trace is limited (see StackTrace::maxDepth).
    EXPECT_EQ(stackTrace.size(), StackTrace<capacity>::maxDepth);
#endif
}

TEST(StackTraceTest, Iteration) {
    auto stackTrace = GetStackTrace2();

    std::vector<void*> actualAddresses;
    for (auto addr : stackTrace) {
        actualAddresses.push_back(addr);
    }

    EXPECT_GT(actualAddresses.size(), 0ul);
    EXPECT_EQ(actualAddresses.size(), stackTrace.size());

    auto symbolicStackTrace = GetStackTraceStrings(std_support::span<void*>(actualAddresses.data(), actualAddresses.size()));
    EXPECT_THAT(symbolicStackTrace[0], testing::HasSubstr("GetStackTrace1"));
    EXPECT_THAT(symbolicStackTrace[1], testing::HasSubstr("GetStackTrace2"));
}

TEST(StackTraceTest, StackAllocatedIteration) {
    auto stackTrace = GetStackTrace2<2>();

    std::vector<void*> actualAddresses;
    for (auto addr : stackTrace) {
        actualAddresses.push_back(addr);
    }

    EXPECT_EQ(actualAddresses.size(), 2ul);
    EXPECT_EQ(actualAddresses.size(), stackTrace.size());

    auto symbolicStackTrace = GetStackTraceStrings(std_support::span<void*>(actualAddresses.data(), actualAddresses.size()));
    EXPECT_THAT(symbolicStackTrace[0], testing::HasSubstr("GetStackTrace1"));
    EXPECT_THAT(symbolicStackTrace[1], testing::HasSubstr("GetStackTrace2"));
}

TEST(StackTraceTest, IndexedAccess) {
    auto stackTrace = GetStackTrace2();

    std::vector<void*> actualAddresses;
    for (size_t i = 0; i < stackTrace.size(); i++) {
        actualAddresses.push_back(stackTrace[i]);
    }

    EXPECT_GT(actualAddresses.size(), 0ul);
    auto symbolicStackTrace = GetStackTraceStrings(std_support::span<void*>(actualAddresses.data(), actualAddresses.size()));
    EXPECT_THAT(symbolicStackTrace[0], testing::HasSubstr("GetStackTrace1"));
    EXPECT_THAT(symbolicStackTrace[1], testing::HasSubstr("GetStackTrace2"));
}

TEST(StackTraceTest, StackAllocatedIndexedAccess) {
    auto stackTrace = GetStackTrace2<2>();

    std::vector<void*> actualAddresses;
    for (size_t i = 0; i < stackTrace.size(); i++) {
        actualAddresses.push_back(stackTrace[i]);
    }

    EXPECT_EQ(actualAddresses.size(), 2ul);
    auto symbolicStackTrace = GetStackTraceStrings(std_support::span<void*>(actualAddresses.data(), actualAddresses.size()));
    EXPECT_THAT(symbolicStackTrace[0], testing::HasSubstr("GetStackTrace1"));
    EXPECT_THAT(symbolicStackTrace[1], testing::HasSubstr("GetStackTrace2"));
}

TEST(StackTraceTest, IndexedAccessAndIteration) {
    auto stackTrace = GetStackTrace2();

    size_t i = 0;
    for (auto addr : stackTrace) {
        EXPECT_EQ(addr, stackTrace[i]);
        i++;
    }
    EXPECT_EQ(stackTrace.size(), i);
}

TEST(StackTraceTest, StackAllocatedIndexedAccessAndIteration) {
    auto stackTrace = GetStackTrace2<2>();

    size_t i = 0;
    for (auto addr : stackTrace) {
        EXPECT_EQ(addr, stackTrace[i]);
        i++;
    }
    EXPECT_EQ(stackTrace.size(), i);
    EXPECT_EQ(stackTrace.size(), 2ul);
}

#define EXPECT_TRACES_EQ(trace1, trace2) do { \
    EXPECT_TRUE((trace1) == (trace2));        \
    EXPECT_FALSE((trace1) != (trace2));       \
} while(false)

#define EXPECT_TRACES_NE(trace1, trace2) do { \
    EXPECT_FALSE((trace1) == (trace2));       \
    EXPECT_TRUE((trace1) != (trace2));        \
} while(false)

TEST(StackTraceTest, EqualsAndHash) {
    std::hash<StackTrace<>> hasher;

    StackTrace<> empty1, empty2;
    EXPECT_TRACES_EQ(empty1, empty2);
    EXPECT_EQ(hasher(empty1), hasher(empty2));

    auto trace1 = GetStackTrace2(/* skipFrames = */ 0, /* depth = */ 2);
    EXPECT_TRACES_NE(trace1, empty2);

    auto trace2 = GetStackTrace2(/* skipFrames = */ 0, /* depth = */ 2);
    EXPECT_TRACES_EQ(trace1, trace2);
    EXPECT_EQ(hasher(trace1), hasher(trace2));

    auto traceWithSkip = GetStackTrace2(/* skipFrames = */ 1, /* depth = */ 2);
    EXPECT_TRACES_NE(trace1, traceWithSkip);

    auto anotherTrace = GetStackTrace3();
    EXPECT_TRACES_NE(trace1, anotherTrace);

    trace1 = StackTrace<>::TestSupport::constructFrom(
            { reinterpret_cast<void*>(42), reinterpret_cast<void*>(43) });
    trace2 = StackTrace<>::TestSupport::constructFrom(
            { reinterpret_cast<void*>(44), reinterpret_cast<void*>(45) });
    EXPECT_NE(hasher(trace1), hasher(trace2));
}

TEST(StackTraceTest, StackAllocatedEqualsAndHash) {
    constexpr size_t capacity = 10;
    std::hash<StackTrace<capacity>> hasher;

    StackTrace<capacity> empty1, empty2;
    EXPECT_TRACES_EQ(empty1, empty2);
    EXPECT_EQ(hasher(empty1), hasher(empty2));

    auto trace1 = GetStackTrace2<capacity>(/* skipFrames = */ 0, /* depth = */ 2);;
    EXPECT_TRACES_NE(trace1, empty2);

    auto trace2 = GetStackTrace2<capacity>(/* skipFrames = */ 0, /* depth = */ 2);;
    EXPECT_TRACES_EQ(trace1, trace2);
    EXPECT_EQ(hasher(trace1), hasher(trace2));

    auto traceWithSkip = GetStackTrace2<capacity>(/* skipFrames = */ 1, /* depth = */ 2);
    EXPECT_TRACES_NE(trace1, traceWithSkip);

    auto anotherTrace = GetStackTrace3<capacity>();
    EXPECT_TRACES_NE(trace1, anotherTrace);

    trace1 = StackTrace<capacity>::TestSupport::constructFrom(
            { reinterpret_cast<void*>(42), reinterpret_cast<void*>(43) });
    trace2 = StackTrace<capacity>::TestSupport::constructFrom(
            { reinterpret_cast<void*>(44), reinterpret_cast<void*>(45) });
    EXPECT_NE(hasher(trace1), hasher(trace2));
}

TEST(StackTraceTest, StoreInHashSet) {
    constexpr size_t capacity = 10;
    std::unordered_set<StackTrace<capacity>> set;
    StackTrace<capacity> empty;
    StackTrace<capacity> trace1 = GetStackTrace1<capacity>();
    StackTrace<capacity> trace2 = GetStackTrace2<capacity>();
    EXPECT_THAT(set.find(empty), set.end());
    EXPECT_THAT(set.find(trace1), set.end());
    EXPECT_THAT(set.find(trace2), set.end());

    set.insert(empty);
    EXPECT_THAT(set.find(empty), Not(set.end()));
    EXPECT_THAT(set.find(trace1), set.end());
    EXPECT_THAT(set.find(trace2), set.end());

    set.insert(trace1);
    EXPECT_THAT(set.find(empty), Not(set.end()));
    EXPECT_THAT(set.find(trace1), Not(set.end()));
    EXPECT_THAT(set.find(trace2), set.end());

    set.insert(trace2);
    EXPECT_THAT(set.find(empty), Not(set.end()));
    EXPECT_THAT(set.find(trace1), Not(set.end()));
    EXPECT_THAT(set.find(trace2), Not(set.end()));
}

TEST(StackTraceTest, StackAllocatedStoreInHashSet) {
    std::unordered_set<StackTrace<>> set;
    StackTrace<> empty;
    StackTrace<> trace1 = GetStackTrace1();
    StackTrace<> trace2 = GetStackTrace2();
    EXPECT_THAT(set.find(empty), set.end());
    EXPECT_THAT(set.find(trace1), set.end());
    EXPECT_THAT(set.find(trace2), set.end());

    set.insert(empty);
    EXPECT_THAT(set.find(empty), Not(set.end()));
    EXPECT_THAT(set.find(trace1), set.end());
    EXPECT_THAT(set.find(trace2), set.end());

    set.insert(trace1);
    EXPECT_THAT(set.find(empty), Not(set.end()));
    EXPECT_THAT(set.find(trace1), Not(set.end()));
    EXPECT_THAT(set.find(trace2), set.end());

    set.insert(trace2);
    EXPECT_THAT(set.find(empty), Not(set.end()));
    EXPECT_THAT(set.find(trace1), Not(set.end()));
    EXPECT_THAT(set.find(trace2), Not(set.end()));
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
