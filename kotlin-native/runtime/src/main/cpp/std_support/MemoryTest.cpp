/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "std_support/Memory.hpp"

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "StdAllocatorTestSupport.hpp"
#include "KAssert.h"
#include "Utils.hpp"

using namespace kotlin;

namespace {

class EmptyClass {};

class Class {
public:
    explicit Class(int32_t x) : x_(x) {}

    int32_t x() const { return x_; }

private:
    int32_t x_;
};
static_assert(sizeof(Class) > sizeof(EmptyClass));

class ClassThrows {
public:
    explicit ClassThrows(int32_t x) : x_(x) { throw 13; }

    int32_t x() const { return x_; }

private:
    int32_t x_;
};

class DerivedClass : public Class {
public:
    DerivedClass(int32_t x, int32_t y) : Class(x), y_(y) {}

    int32_t y() const { return y_; }

private:
    int32_t y_;
};

class MockClass : private Pinned {
public:
    class Mocker : private Pinned {
    public:
        Mocker() noexcept {
            RuntimeAssert(instance_ == nullptr, "Only one MockClass::Mocker at a time allowed");
            instance_ = this;
        }

        ~Mocker() {
            RuntimeAssert(instance_ == this, "MockClass::Mocker::instance_ is broken.");
            instance_ = nullptr;
        }

        MOCK_METHOD(void, ctor, (MockClass*, int));
        MOCK_METHOD(void, dtor, (MockClass*), (noexcept));

    private:
        friend class MockClass;

        static Mocker* instance_;
    };

    explicit MockClass(int x) { Mocker::instance_->ctor(this, x); }

    ~MockClass() noexcept { Mocker::instance_->dtor(this); }

    int32_t x() const { return x_; }

private:
    int32_t x_;
};
static_assert(sizeof(MockClass) > sizeof(EmptyClass));

// static
MockClass::Mocker* MockClass::Mocker::instance_ = nullptr;

} // namespace

TEST(StdSupportMemoryTest, AllocatorNew) {
    testing::StrictMock<test_support::MockAllocatorCore> allocatorCore;
    testing::StrictMock<MockClass::Mocker> mocker;

    MockClass* expectedPtr = reinterpret_cast<MockClass*>(13);

    {
        testing::InSequence s;
        EXPECT_CALL(allocatorCore, allocate(sizeof(MockClass))).WillOnce(testing::Return(expectedPtr));
        EXPECT_CALL(mocker, ctor(expectedPtr, 42));
    }
    auto* ptr = std_support::allocator_new<MockClass>(test_support::MakeAllocator<MockClass>(allocatorCore), 42);
    EXPECT_THAT(ptr, expectedPtr);
}

TEST(StdSupportMemoryTest, AllocatorNewThrows) {
    testing::StrictMock<test_support::MockAllocatorCore> allocatorCore;
    testing::StrictMock<MockClass::Mocker> mocker;

    MockClass* expectedPtr = reinterpret_cast<MockClass*>(13);

    {
        testing::InSequence s;
        EXPECT_CALL(allocatorCore, allocate(sizeof(MockClass))).WillOnce(testing::Return(expectedPtr));
        EXPECT_CALL(mocker, ctor(expectedPtr, 42)).WillOnce([] { throw 17; });
        EXPECT_CALL(allocatorCore, deallocate(expectedPtr, sizeof(MockClass)));
    }
    EXPECT_THROW(std_support::allocator_new<MockClass>(test_support::MakeAllocator<MockClass>(allocatorCore), 42), int);
}

TEST(StdSupportMemoryTest, AllocatorNewWrongType) {
    testing::StrictMock<test_support::MockAllocatorCore> allocatorCore;
    testing::StrictMock<MockClass::Mocker> mocker;

    MockClass* expectedPtr = reinterpret_cast<MockClass*>(13);

    {
        testing::InSequence s;
        EXPECT_CALL(allocatorCore, allocate(sizeof(MockClass))).WillOnce(testing::Return(expectedPtr));
        EXPECT_CALL(mocker, ctor(expectedPtr, 42));
    }
    auto* ptr = std_support::allocator_new<MockClass>(test_support::MakeAllocator<EmptyClass>(allocatorCore), 42);
    EXPECT_THAT(ptr, expectedPtr);
}

TEST(StdSupportMemoryTest, AllocatorDelete) {
    testing::StrictMock<test_support::MockAllocatorCore> allocatorCore;
    testing::StrictMock<MockClass::Mocker> mocker;

    MockClass* expectedPtr = reinterpret_cast<MockClass*>(13);

    {
        testing::InSequence s;
        EXPECT_CALL(mocker, dtor(expectedPtr));
        EXPECT_CALL(allocatorCore, deallocate(expectedPtr, sizeof(MockClass)));
    }
    std_support::allocator_delete(test_support::MakeAllocator<MockClass>(allocatorCore), expectedPtr);
}

TEST(StdSupportMemoryTest, AllocatorDeleteWrongType) {
    testing::StrictMock<test_support::MockAllocatorCore> allocatorCore;
    testing::StrictMock<MockClass::Mocker> mocker;

    MockClass* expectedPtr = reinterpret_cast<MockClass*>(13);

    {
        testing::InSequence s;
        EXPECT_CALL(mocker, dtor(expectedPtr));
        EXPECT_CALL(allocatorCore, deallocate(expectedPtr, sizeof(MockClass)));
    }
    std_support::allocator_delete(test_support::MakeAllocator<EmptyClass>(allocatorCore), expectedPtr);
}

TEST(StdSupportMemoryTest, AllocateUnique) {
    testing::StrictMock<test_support::MockAllocatorCore> allocatorCore;
    testing::StrictMock<MockClass::Mocker> mocker;

    MockClass* expectedPtr = reinterpret_cast<MockClass*>(13);

    {
        testing::InSequence s;
        EXPECT_CALL(allocatorCore, allocate(sizeof(MockClass))).WillOnce(testing::Return(expectedPtr));
        EXPECT_CALL(mocker, ctor(expectedPtr, 42));
    }
    auto ptr = std_support::allocate_unique<MockClass>(test_support::MakeAllocator<MockClass>(allocatorCore), 42);
    EXPECT_THAT(ptr.get(), expectedPtr);

    {
        testing::InSequence s;
        EXPECT_CALL(mocker, dtor(expectedPtr));
        EXPECT_CALL(allocatorCore, deallocate(expectedPtr, sizeof(MockClass)));
    }
    ptr.reset();
}

TEST(StdSupportMemoryTest, AllocateUniqueThrows) {
    testing::StrictMock<test_support::MockAllocatorCore> allocatorCore;
    testing::StrictMock<MockClass::Mocker> mocker;

    MockClass* expectedPtr = reinterpret_cast<MockClass*>(13);

    {
        testing::InSequence s;
        EXPECT_CALL(allocatorCore, allocate(sizeof(MockClass))).WillOnce(testing::Return(expectedPtr));
        EXPECT_CALL(mocker, ctor(expectedPtr, 42)).WillOnce([] { throw 17; });
        EXPECT_CALL(allocatorCore, deallocate(expectedPtr, sizeof(MockClass)));
    }
    EXPECT_THROW(std_support::allocate_unique<MockClass>(test_support::MakeAllocator<MockClass>(allocatorCore), 42), int);
}

TEST(StdSupportMemoryTest, AllocateUniqueWrongType) {
    testing::StrictMock<test_support::MockAllocatorCore> allocatorCore;
    testing::StrictMock<MockClass::Mocker> mocker;

    MockClass* expectedPtr = reinterpret_cast<MockClass*>(13);

    {
        testing::InSequence s;
        EXPECT_CALL(allocatorCore, allocate(sizeof(MockClass))).WillOnce(testing::Return(expectedPtr));
        EXPECT_CALL(mocker, ctor(expectedPtr, 42));
    }
    auto ptr = std_support::allocate_unique<MockClass>(test_support::MakeAllocator<EmptyClass>(allocatorCore), 42);
    EXPECT_THAT(ptr.get(), expectedPtr);

    {
        testing::InSequence s;
        EXPECT_CALL(mocker, dtor(expectedPtr));
        EXPECT_CALL(allocatorCore, deallocate(expectedPtr, sizeof(MockClass)));
    }
    ptr.reset();
}

TEST(StdSupportMemoryTest, NullptrUnique) {
    testing::StrictMock<test_support::MockAllocatorCore> allocatorCore;
    auto allocator = test_support::MakeAllocator<int>(allocatorCore);

    std::unique_ptr<int, std_support::allocator_deleter<int, decltype(allocator)>> ptr =
            std_support::nullptr_unique<int>(allocator);
    EXPECT_THAT(ptr.get(), nullptr);
}
