/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "StdAllocatorTestSupport.hpp"

#include "gmock/gmock.h"
#include "gtest/gtest.h"

using namespace kotlin;

namespace {

struct EmptyClass {};

struct Class {
    int32_t x;
};
static_assert(sizeof(Class) > sizeof(EmptyClass));

} // namespace

TEST(StdAllocatorTestSupportTest, MockAllocate) {
    testing::StrictMock<test_support::MockAllocatorCore> allocatorCore;
    auto allocator = test_support::MakeAllocator<Class>(allocatorCore);

    auto* expectedPtr = reinterpret_cast<Class*>(13);
    EXPECT_CALL(allocatorCore, allocate(2 * sizeof(Class))).WillOnce(testing::Return(expectedPtr));
    auto* ptr = std::allocator_traits<decltype(allocator)>::allocate(allocator, 2);
    EXPECT_THAT(ptr, expectedPtr);
}

TEST(StdAllocatorTestSupportTest, MockDeallocate) {
    testing::StrictMock<test_support::MockAllocatorCore> allocatorCore;
    auto allocator = test_support::MakeAllocator<Class>(allocatorCore);

    auto* ptr = reinterpret_cast<Class*>(13);
    EXPECT_CALL(allocatorCore, deallocate(ptr, 2 * sizeof(Class)));
    std::allocator_traits<decltype(allocator)>::deallocate(allocator, ptr, 2);
}

TEST(StdAllocatorTestSupportTest, MockAdjustType) {
    testing::StrictMock<test_support::MockAllocatorCore> allocatorCore;
    auto initial = test_support::MakeAllocator<EmptyClass>(allocatorCore);

    using Allocator = std::allocator_traits<decltype(initial)>::template rebind_alloc<Class>;
    using Traits = std::allocator_traits<decltype(initial)>::template rebind_traits<Class>;
    Allocator allocator = Allocator(initial);

    auto* expectedPtr = reinterpret_cast<Class*>(13);
    EXPECT_CALL(allocatorCore, allocate(2 * sizeof(Class))).WillOnce(testing::Return(expectedPtr));
    auto* ptr = Traits::allocate(allocator, 2);
    EXPECT_THAT(ptr, expectedPtr);
    testing::Mock::VerifyAndClearExpectations(&allocatorCore);

    EXPECT_CALL(allocatorCore, deallocate(ptr, 2 * sizeof(Class)));
    Traits::deallocate(allocator, ptr, 2);
}

TEST(StdAllocatorTestSupportTest, Spy) {
    test_support::SpyAllocatorCore allocatorCore;
    auto allocator = test_support::MakeAllocator<Class>(allocatorCore);

    EXPECT_CALL(allocatorCore, allocate(2 * sizeof(Class)));
    auto* ptr1 = std::allocator_traits<decltype(allocator)>::allocate(allocator, 2);
    testing::Mock::VerifyAndClearExpectations(&allocatorCore);

    using Allocator = std::allocator_traits<decltype(allocator)>::template rebind_alloc<EmptyClass>;
    using Traits = std::allocator_traits<decltype(allocator)>::template rebind_traits<EmptyClass>;
    Allocator b = Allocator(allocator);

    EXPECT_CALL(allocatorCore, allocate(2 * sizeof(EmptyClass)));
    auto* ptr2 = Traits::allocate(b, 2);
    testing::Mock::VerifyAndClearExpectations(&allocatorCore);

    EXPECT_CALL(allocatorCore, deallocate(ptr1, 2 * sizeof(Class)));
    std::allocator_traits<decltype(allocator)>::deallocate(allocator, ptr1, 2);
    testing::Mock::VerifyAndClearExpectations(&allocatorCore);

    EXPECT_CALL(allocatorCore, deallocate(ptr2, 2 * sizeof(EmptyClass)));
    Traits::deallocate(b, ptr2, 2);
    testing::Mock::VerifyAndClearExpectations(&allocatorCore);
}
