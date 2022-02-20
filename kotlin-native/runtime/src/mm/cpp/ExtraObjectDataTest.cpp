/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ExtraObjectData.hpp"

#include <atomic>

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "ObjectTestSupport.hpp"
#include "ScopedThread.hpp"
#include "TestSupport.hpp"

using namespace kotlin;

namespace {

struct EmptyPayload {
    using Field = ObjHeader* EmptyPayload::*;
    static constexpr std::array<Field, 0> kFields{};
};

class ExtraObjectDataTest : public testing::Test {
public:
    ExtraObjectDataTest() {}

    ~ExtraObjectDataTest() {
        mm::GlobalsRegistry::Instance().ClearForTests();
        mm::GlobalData::Instance().extraObjectDataFactory().ClearForTests();
        mm::GlobalData::Instance().gc().ClearForTests();
    }
};

} // namespace

TEST_F(ExtraObjectDataTest, Install) {
    ScopedMemoryInit init;
    test_support::TypeInfoHolder type{test_support::TypeInfoHolder::ObjectBuilder<EmptyPayload>()};
    test_support::Object<EmptyPayload> object(type.typeInfo());
    auto* typeInfo = object.header()->type_info();

    ASSERT_FALSE(object.header()->has_meta_object());

    auto& extraData = mm::ExtraObjectData::Install(object.header());

    EXPECT_TRUE(object.header()->has_meta_object());
    EXPECT_THAT(object.header()->meta_object(), extraData.AsMetaObjHeader());
    EXPECT_THAT(object.header()->type_info(), typeInfo);
    EXPECT_FALSE(extraData.HasWeakReferenceCounter());
    EXPECT_THAT(extraData.GetBaseObject(), object.header());

    extraData.Uninstall();
    mm::GlobalData::Instance().threadRegistry().CurrentThreadData()->ClearForTests();

    EXPECT_FALSE(object.header()->has_meta_object());
    EXPECT_THAT(object.header()->type_info(), typeInfo);
}

TEST_F(ExtraObjectDataTest, ConcurrentInstall) {
    ScopedMemoryInit init;
    test_support::TypeInfoHolder type{test_support::TypeInfoHolder::ObjectBuilder<EmptyPayload>()};
    test_support::Object<EmptyPayload> object(type.typeInfo());

    constexpr int kThreadCount = kDefaultThreadCount;

    std::atomic<bool> canStart(false);
    std::atomic<int> readyCount(0);
    std::vector<ScopedThread> threads;
    std::vector<mm::ExtraObjectData*> actual(kThreadCount, nullptr);

    for (int i = 0; i < kThreadCount; ++i) {
        threads.emplace_back([i, &actual, &object, &canStart, &readyCount]() {
            ScopedMemoryInit init;
            ++readyCount;
            while (!canStart) {
            }
            auto& extraData = mm::ExtraObjectData::Install(object.header());
            actual[i] = &extraData;
            mm::GlobalData::Instance().threadRegistry().CurrentThreadData()->Publish();
        });
    }

    while (readyCount < kThreadCount) {
    }
    canStart = true;
    threads.clear();

    std::vector<mm::ExtraObjectData*> expected(kThreadCount, actual[0]);

    EXPECT_THAT(actual, testing::ElementsAreArray(expected));
}
