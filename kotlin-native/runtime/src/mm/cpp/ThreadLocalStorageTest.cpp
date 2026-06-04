/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ThreadLocalStorage.hpp"

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "Types.h"

using namespace kotlin;

namespace {

struct Key {};

} // namespace

TEST(ThreadLocalStorageTest, Lookup) {
    Key key1;
    Key key2;
    mm::ThreadLocalStorage tls;

    ObjHeader** location1 = tls.LookupOrRegister(&key1, 1, 0);
    ObjHeader** location2 = tls.LookupOrRegister(&key2, 2, 0);
    ObjHeader** location3 = tls.LookupOrRegister(&key2, 2, 1);

    // Locations are not nulls.
    EXPECT_NE(location1, nullptr);
    EXPECT_NE(location2, nullptr);
    EXPECT_NE(location3, nullptr);

    // All three are different.
    EXPECT_NE(location1, location2);
    EXPECT_NE(location1, location3);
    EXPECT_NE(location2, location3);

    // All three can be written into.
    *location1 = nullptr;
    *location2 = nullptr;
    *location3 = nullptr;
}

TEST(ThreadLocalStorageTest, SlotsAreNullInitialized) {
    Key key1;
    mm::ThreadLocalStorage tls;

    EXPECT_EQ(*tls.LookupOrRegister(&key1, 2, 0), nullptr);
    EXPECT_EQ(*tls.LookupOrRegister(&key1, 2, 1), nullptr);
}

TEST(ThreadLocalStorageTest, Iterate) {
    Key key1;
    Key key2;
    mm::ThreadLocalStorage tls;

    std::vector<ObjHeader**> expected;
    expected.push_back(tls.LookupOrRegister(&key1, 1, 0));
    expected.push_back(tls.LookupOrRegister(&key2, 2, 0));
    expected.push_back(tls.LookupOrRegister(&key2, 2, 1));

    std::vector<ObjHeader**> actual;
    for (auto& item : tls) {
        actual.push_back(&item);
    }

    // Blocks of distinct keys are not ordered relative to each other.
    EXPECT_THAT(actual, testing::UnorderedElementsAreArray(expected));
}

TEST(ThreadLocalStorageTest, StableAddressesAcrossRegistrations) {
    Key key1;
    Key key2;
    mm::ThreadLocalStorage tls;

    ObjHeader** location1 = tls.LookupOrRegister(&key1, 1, 0);
    // Registering another key must not move the previously handed out slot.
    ObjHeader** location2 = tls.LookupOrRegister(&key2, 1, 0);
    EXPECT_EQ(location1, tls.LookupOrRegister(&key1, 1, 0));
    EXPECT_NE(location1, location2);
}

TEST(ThreadLocalStorageTest, NoRecords) {
    mm::ThreadLocalStorage tls;

    std::vector<ObjHeader**> actual;
    for (auto& item : tls) {
        actual.push_back(&item);
    }

    EXPECT_THAT(actual, testing::IsEmpty());
}

TEST(ThreadLocalStorageTest, ClearEmpty) {
    mm::ThreadLocalStorage tls;

    tls.Clear();

    std::vector<ObjHeader**> actual;
    for (auto& item : tls) {
        actual.push_back(&item);
    }

    EXPECT_THAT(actual, testing::IsEmpty());
}

TEST(ThreadLocalStorageTest, ClearNonEmpty) {
    Key key1;
    mm::ThreadLocalStorage tls;

    tls.LookupOrRegister(&key1, 1, 0);

    tls.Clear();

    std::vector<ObjHeader**> actual;
    for (auto& item : tls) {
        actual.push_back(&item);
    }

    EXPECT_THAT(actual, testing::IsEmpty());
}

TEST(ThreadLocalStorageTest, ReRegisterAfterClear) {
    Key key1;
    mm::ThreadLocalStorage tls;

    tls.LookupOrRegister(&key1, 1, 0);
    tls.Clear();

    // After clearing, the key can be looked up again, allocating fresh storage.
    ObjHeader** location = tls.LookupOrRegister(&key1, 1, 0);
    EXPECT_NE(location, nullptr);
    *location = nullptr;
}

TEST(ThreadLocalStorageTest, LookupCaching) {
    Key key1;
    Key key2;
    mm::ThreadLocalStorage tls;

    ObjHeader** location1 = tls.LookupOrRegister(&key1, 1, 0);
    ObjHeader** location2 = tls.LookupOrRegister(&key2, 1, 0);

    // Lookup same stuff again in different order.
    EXPECT_EQ(location1, tls.LookupOrRegister(&key1, 1, 0));
    EXPECT_EQ(location2, tls.LookupOrRegister(&key2, 1, 0));
    EXPECT_EQ(location2, tls.LookupOrRegister(&key2, 1, 0));
    EXPECT_EQ(location1, tls.LookupOrRegister(&key1, 1, 0));
}
