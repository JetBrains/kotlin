/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ThreadLocalStorage.hpp"

#include "gmock/gmock.h"
#include "gtest/gtest.h"

using namespace kotlin;

namespace {

struct Key {};

} // namespace

TEST(ThreadLocalStorageTest, Lookup) {
    Key key1;
    Key key2;
    mm::ThreadLocalStorage tls;

    tls.AddRecord(&key1, 1);
    tls.AddRecord(&key2, 2);
    tls.Commit();

    ObjHeader** location1 = tls.Lookup(&key1, 0);
    ObjHeader** location2 = tls.Lookup(&key2, 0);
    ObjHeader** location3 = tls.Lookup(&key2, 1);

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

TEST(ThreadLocalStorageTest, Iterate) {
    Key key1;
    Key key2;
    mm::ThreadLocalStorage tls;

    tls.AddRecord(&key1, 1);
    tls.AddRecord(&key2, 2);
    tls.Commit();

    std::vector<ObjHeader**> expected;
    expected.push_back(tls.Lookup(&key1, 0));
    expected.push_back(tls.Lookup(&key2, 0));
    expected.push_back(tls.Lookup(&key2, 1));

    std::vector<ObjHeader**> actual;
    for (auto item : tls) {
        actual.push_back(item);
    }

    EXPECT_THAT(actual, testing::ElementsAreArray(expected));
}

TEST(ThreadLocalStorageTest, AddRecordEmpty) {
    Key key1;
    Key key2;
    Key key3;
    mm::ThreadLocalStorage tls;

    tls.AddRecord(&key1, 1);
    tls.AddRecord(&key2, 0);
    tls.AddRecord(&key3, 2);
    tls.Commit();

    std::vector<ObjHeader**> expected;
    expected.push_back(tls.Lookup(&key1, 0));
    expected.push_back(tls.Lookup(&key3, 0));
    expected.push_back(tls.Lookup(&key3, 1));

    std::vector<ObjHeader**> actual;
    for (auto item : tls) {
        actual.push_back(item);
    }

    EXPECT_THAT(actual, testing::ElementsAreArray(expected));
}

TEST(ThreadLocalStorageTest, AddRecordSameSize) {
    Key key1;
    mm::ThreadLocalStorage tls;

    tls.AddRecord(&key1, 1);
    tls.AddRecord(&key1, 1);
    tls.Commit();

    std::vector<ObjHeader**> expected;
    expected.push_back(tls.Lookup(&key1, 0));

    std::vector<ObjHeader**> actual;
    for (auto item : tls) {
        actual.push_back(item);
    }

    EXPECT_THAT(actual, testing::ElementsAreArray(expected));
}

TEST(ThreadLocalStorageTest, NoRecords) {
    mm::ThreadLocalStorage tls;

    tls.Commit();

    std::vector<ObjHeader**> actual;
    for (auto item : tls) {
        actual.push_back(item);
    }

    EXPECT_THAT(actual, testing::IsEmpty());
}

TEST(ThreadLocalStorageTest, ClearEmpty) {
    mm::ThreadLocalStorage tls;

    tls.Commit();

    tls.Clear();

    std::vector<ObjHeader**> actual;
    for (auto item : tls) {
        actual.push_back(item);
    }

    EXPECT_THAT(actual, testing::IsEmpty());
}

TEST(ThreadLocalStorageTest, ClearNonEmpty) {
    Key key1;
    mm::ThreadLocalStorage tls;

    tls.AddRecord(&key1, 1);
    tls.Commit();

    tls.Clear();

    std::vector<ObjHeader**> actual;
    for (auto item : tls) {
        actual.push_back(item);
    }

    EXPECT_THAT(actual, testing::IsEmpty());
}

TEST(ThreadLocalStorageTest, LookupCaching) {
    Key key1;
    Key key2;
    mm::ThreadLocalStorage tls;

    tls.AddRecord(&key1, 1);
    tls.AddRecord(&key2, 1);
    tls.Commit();

    ObjHeader** location1 = tls.Lookup(&key1, 0);
    ObjHeader** location2 = tls.Lookup(&key2, 0);

    // Lookup same stuff again in different order.
    EXPECT_EQ(location1, tls.Lookup(&key1, 0));
    EXPECT_EQ(location2, tls.Lookup(&key2, 0));
    EXPECT_EQ(location2, tls.Lookup(&key2, 0));
    EXPECT_EQ(location1, tls.Lookup(&key1, 0));
}
