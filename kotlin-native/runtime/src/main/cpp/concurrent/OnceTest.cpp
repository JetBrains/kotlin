/*
 * Copyright 2010-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "concurrent/Once.hpp"

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "ScopedThread.hpp"
#include "TestSupport.hpp"

using namespace kotlin;

template <typename T>
struct OnceTest : public ::testing::Test {
    testing::StrictMock<testing::MockFunction<T()>> mockComputer_;
};

template <int kSize>
struct LargeValue {
    LargeValue() {
        for (int i = 0; i < kSize; ++i) {
            data_[i] = i;
        }
    }
    LargeValue(const LargeValue& that) {
        for (int i = 0; i < kSize; ++i) {
            data_[i] = that.data_[i];
        }
    }
    bool operator==(const LargeValue& other) const {
        for (int i = 0; i < kSize; ++i) {
            if (data_[i] != other.data_[i]) return false;
        }
        return true;
    }
    std::array<int, kSize> data_;
};

using ValueTypes = testing::Types<int, LargeValue<1000>>;
TYPED_TEST_SUITE(OnceTest, ValueTypes);

TYPED_TEST(OnceTest, SingleThread) {
    using T = TypeParam;

    OnceComputable<T> onceComputable;
    EXPECT_THAT(onceComputable.tryGetValue(), nullptr);

    EXPECT_CALL(this->mockComputer_, Call()).WillOnce(testing::Return(T{}));

    onceComputable = [this] { return this->mockComputer_.Call(); };
    onceComputable = [this] { return this->mockComputer_.Call(); };

    testing::Mock::VerifyAndClearExpectations(&this->mockComputer_);

    EXPECT_THAT(*onceComputable.tryGetValue(), T{});
}

TYPED_TEST(OnceTest, Concurrent) {
    using T = TypeParam;

    EXPECT_CALL(this->mockComputer_, Call()).WillOnce(testing::Return(T{}));

    OnceComputable<T> onceComputable;
    EXPECT_THAT(onceComputable.tryGetValue(), nullptr);

    std::atomic ready = false;
    std::vector<ScopedThread> threads;
    for (int i = 0; i < kDefaultThreadCount; ++i) {
        threads.emplace_back([&]() noexcept {
            while (!ready) std::this_thread::yield();
            onceComputable = [this] { return this->mockComputer_.Call(); };
        });
    }

    ready = true;
    threads.clear();

    testing::Mock::VerifyAndClearExpectations(&this->mockComputer_);

    EXPECT_THAT(*onceComputable.tryGetValue(), T{});
}
