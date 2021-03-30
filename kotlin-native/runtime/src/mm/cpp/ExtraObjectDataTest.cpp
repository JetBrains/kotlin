/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ExtraObjectData.hpp"

#include <atomic>
#include <thread>

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "ObjectTestSupport.hpp"
#include "TestSupport.hpp"

using namespace kotlin;

namespace {

struct EmptyPayload {
    using Field = ObjHeader* EmptyPayload::*;
    static constexpr std::array<Field, 0> kFields{};
};

} // namespace

TEST(ExtraObjectDataTest, Install) {
    test_support::TypeInfoHolder type{test_support::TypeInfoHolder::ObjectBuilder<EmptyPayload>()};
    test_support::Object<EmptyPayload> object(type.typeInfo());
    auto* typeInfo = object.header()->type_info();

    ASSERT_FALSE(object.header()->has_meta_object());

    auto& extraData = mm::ExtraObjectData::Install(object.header());

    EXPECT_TRUE(object.header()->has_meta_object());
    EXPECT_THAT(object.header()->meta_object(), extraData.AsMetaObjHeader());
    EXPECT_THAT(object.header()->type_info(), typeInfo);

    mm::ExtraObjectData::Uninstall(object.header());

    EXPECT_FALSE(object.header()->has_meta_object());
    EXPECT_THAT(object.header()->type_info(), typeInfo);
}

TEST(ExtraObjectDataTest, ConcurrentInstall) {
    test_support::TypeInfoHolder type{test_support::TypeInfoHolder::ObjectBuilder<EmptyPayload>()};
    test_support::Object<EmptyPayload> object(type.typeInfo());

    constexpr int kThreadCount = kDefaultThreadCount;

    std::atomic<bool> canStart(false);
    std::atomic<int> readyCount(0);
    std::vector<std::thread> threads;
    std::vector<mm::ExtraObjectData*> actual(kThreadCount, nullptr);

    for (int i = 0; i < kThreadCount; ++i) {
        threads.emplace_back([i, &actual, &object, &canStart, &readyCount]() {
            ++readyCount;
            while (!canStart) {
            }
            auto& extraData = mm::ExtraObjectData::Install(object.header());
            actual[i] = &extraData;
        });
    }

    while (readyCount < kThreadCount) {
    }
    canStart = true;

    for (auto& t : threads) {
        t.join();
    }

    std::vector<mm::ExtraObjectData*> expected(kThreadCount, actual[0]);

    EXPECT_THAT(actual, testing::ElementsAreArray(expected));

    mm::ExtraObjectData::Uninstall(object.header());
}
