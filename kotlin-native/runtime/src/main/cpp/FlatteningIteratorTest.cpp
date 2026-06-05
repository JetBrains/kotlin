/*
 * Copyright 2010-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "FlatteningIterator.hpp"

#include <map>
#include <type_traits>
#include <vector>

#include "gmock/gmock.h"
#include "gtest/gtest.h"

using namespace kotlin;

namespace {

template <typename Outer, typename Project = Identity>
std::vector<int> Collect(Outer& outer, Project project = Project()) {
    std::vector<int> result;
    FlatteningIterator<decltype(outer.begin()), Project> it(outer.begin(), outer.end(), project);
    FlatteningIterator<decltype(outer.begin()), Project> end(outer.end(), outer.end(), project);
    for (; it != end; ++it) {
        result.push_back(*it);
    }
    return result;
}

} // namespace

TEST(FlatteningIteratorTest, EmptyOuter) {
    std::vector<std::vector<int>> outer;
    EXPECT_THAT(Collect(outer), testing::IsEmpty());
}

TEST(FlatteningIteratorTest, AllInnerEmpty) {
    std::vector<std::vector<int>> outer = {{}, {}, {}};
    EXPECT_THAT(Collect(outer), testing::IsEmpty());
}

TEST(FlatteningIteratorTest, InterleavedEmptyInner) {
    std::vector<std::vector<int>> outer = {{}, {1, 2}, {}, {3}, {}};
    EXPECT_THAT(Collect(outer), testing::ElementsAre(1, 2, 3));
}

TEST(FlatteningIteratorTest, MultipleRanges) {
    std::vector<std::vector<int>> outer = {{1, 2}, {3}, {4, 5, 6}};
    EXPECT_THAT(Collect(outer), testing::ElementsAre(1, 2, 3, 4, 5, 6));
}

TEST(FlatteningIteratorTest, FlattenMapValues) {
    std::map<int, std::vector<int>> outer = {{0, {1, 2}}, {1, {}}, {2, {3}}};
    // `std::map` orders keys, so values come out in key order.
    EXPECT_THAT(Collect(outer, SecondOfPair{}), testing::ElementsAre(1, 2, 3));
}

TEST(FlatteningIteratorTest, YieldsMutableReference) {
    std::vector<std::vector<int>> outer = {{1, 2}, {3}};

    FlatteningIterator<decltype(outer.begin())> it(outer.begin(), outer.end());
    FlatteningIterator<decltype(outer.begin())> end(outer.end(), outer.end());
    for (; it != end; ++it) {
        *it += 10;
    }

    EXPECT_THAT(outer[0], testing::ElementsAre(11, 12));
    EXPECT_THAT(outer[1], testing::ElementsAre(13));
}

TEST(FlatteningIteratorTest, CopyAssignable) {
    // Some callers store the iterator in a `union` and copy-assign it, so this must hold.
    using MapIterator = FlatteningIterator<std::map<int, std::vector<int>>::iterator, SecondOfPair>;
    static_assert(std::is_copy_assignable_v<MapIterator>, "FlatteningIterator must be copy-assignable");

    using VectorIterator = FlatteningIterator<std::vector<std::vector<int>>::iterator>;
    static_assert(std::is_copy_assignable_v<VectorIterator>, "FlatteningIterator must be copy-assignable");
}
