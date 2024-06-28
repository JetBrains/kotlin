/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ManuallyScoped.hpp"

#include "gmock/gmock.h"
#include "gtest/gtest.h"

using namespace kotlin;

using ::testing::_;

namespace {

struct NonTrivialCtor {
    int x = 123;
};

static_assert(!std::is_trivially_constructible_v<NonTrivialCtor>);
static_assert(std::is_trivially_destructible_v<NonTrivialCtor>);
static_assert(std::is_trivially_constructible_v<ManuallyScoped<NonTrivialCtor>>);
static_assert(std::is_trivially_destructible_v<ManuallyScoped<NonTrivialCtor>>);

class NonTrivialDtor : private Pinned {
public:
    explicit NonTrivialDtor(std::function<void(NonTrivialDtor*)> dtorHook) : dtorHook_(std::move(dtorHook)) {}

    ~NonTrivialDtor() { dtorHook_(this); }

private:
    std::function<void(NonTrivialDtor*)> dtorHook_;
};

static_assert(!std::is_trivially_constructible_v<NonTrivialDtor>);
static_assert(!std::is_trivially_destructible_v<NonTrivialDtor>);
static_assert(std::is_trivially_constructible_v<ManuallyScoped<NonTrivialDtor>>);
static_assert(std::is_trivially_destructible_v<ManuallyScoped<NonTrivialDtor>>);

} // namespace

class ManuallyScopedTest : public testing::Test {
public:
    testing::MockFunction<void(NonTrivialDtor*)>& dtorHook() noexcept { return dtorHook_; }

private:
    testing::StrictMock<testing::MockFunction<void(NonTrivialDtor*)>> dtorHook_;
};

TEST_F(ManuallyScopedTest, NonTrivialCtor) {
    ManuallyScoped<NonTrivialCtor> instance;
    instance.construct();
    EXPECT_THAT(instance->x, 123);
}

TEST_F(ManuallyScopedTest, NonTrivialDtor) {
    ManuallyScoped<NonTrivialDtor> instance;
    instance.construct(dtorHook().AsStdFunction());
    EXPECT_CALL(dtorHook(), Call(&*instance));
    instance.destroy();
}

TEST_F(ManuallyScopedTest, NonTrivialDtorLeak) {
    ManuallyScoped<NonTrivialDtor> instance;
    instance.construct(dtorHook().AsStdFunction());
    EXPECT_CALL(dtorHook(), Call(_)).Times(0);
}
