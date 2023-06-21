/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "Allocator.hpp"

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "std_support/Memory.hpp"

using namespace kotlin;

using testing::_;

namespace {

class MockAllocator {
public:
    MOCK_METHOD(void*, Alloc, (size_t));
};

class MockAllocatorWrapper {
public:
    MockAllocator& operator*() { return *mock_; }

    void* Alloc(size_t size) { return mock_->Alloc(size); }

private:
    std_support::unique_ptr<testing::StrictMock<MockAllocator>> mock_ = std_support::make_unique<testing::StrictMock<MockAllocator>>();
};

class MockGC {
public:
    MOCK_METHOD(void, OnOOM, (size_t));
};

} // namespace

TEST(AllocatorWithGCTest, AllocateWithoutOOM) {
    constexpr size_t size = 256;
    void* nonNull = reinterpret_cast<void*>(1);
    MockAllocatorWrapper baseAllocator;
    testing::StrictMock<MockGC> gc;
    {
        testing::InSequence seq;
        EXPECT_CALL(*baseAllocator, Alloc(size)).WillOnce(testing::Return(nonNull));
        EXPECT_CALL(gc, OnOOM(_)).Times(0);
    }
    gc::AllocatorWithGC<MockAllocatorWrapper, MockGC> allocator(std::move(baseAllocator), gc);
    void* ptr = allocator.Alloc(size);
    EXPECT_THAT(ptr, nonNull);
}

TEST(AllocatorWithGCTest, AllocateWithFixableOOM) {
    constexpr size_t size = 256;
    void* nonNull = reinterpret_cast<void*>(1);
    MockAllocatorWrapper baseAllocator;
    testing::StrictMock<MockGC> gc;
    {
        testing::InSequence seq;
        EXPECT_CALL(*baseAllocator, Alloc(size)).WillOnce(testing::Return(nullptr));
        EXPECT_CALL(gc, OnOOM(size));
        EXPECT_CALL(*baseAllocator, Alloc(size)).WillOnce(testing::Return(nonNull));
    }
    gc::AllocatorWithGC<MockAllocatorWrapper, MockGC> allocator(std::move(baseAllocator), gc);
    void* ptr = allocator.Alloc(size);
    EXPECT_THAT(ptr, nonNull);
}

TEST(AllocatorWithGCTest, AllocateWithUnfixableOOM) {
    constexpr size_t size = 256;
    MockAllocatorWrapper baseAllocator;
    testing::StrictMock<MockGC> gc;
    {
        testing::InSequence seq;
        EXPECT_CALL(*baseAllocator, Alloc(size)).WillOnce(testing::Return(nullptr));
        EXPECT_CALL(gc, OnOOM(size));
        EXPECT_CALL(*baseAllocator, Alloc(size)).WillOnce(testing::Return(nullptr));
    }
    gc::AllocatorWithGC<MockAllocatorWrapper, MockGC> allocator(std::move(baseAllocator), gc);
    void* ptr = allocator.Alloc(size);
    EXPECT_THAT(ptr, nullptr);
}
