/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "IntrusiveList.hpp"

#include <forward_list>
#include <type_traits>

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "Utils.hpp"
#include "std_support/List.hpp"

using namespace kotlin;

namespace {

class Element {
public:
    explicit Element(int value) : value_(value) {}

    Element(const Element&) = default;
    Element(Element&&) = default;
    Element& operator=(const Element&) = default;
    Element& operator=(Element&&) = default;

    int& operator*() { return value_; }
    const int& operator*() const { return value_; }

    bool operator==(const Element& rhs) const { return value_ == rhs.value_; }

    bool operator!=(const Element& rhs) const { return !(*this == rhs); }

private:
    int value_;
};

class Node : private Pinned {
public:
    explicit Node(int value) : value_(value) {}

    int& operator*() { return value_; }
    const int& operator*() const { return value_; }

    void clearNext() noexcept { next_ = nullptr; }

private:
    friend struct DefaultIntrusiveForwardListTraits<Node>;

    Node* next() const noexcept { return next_; }
    void setNext(Node* next) noexcept {
        RuntimeAssert(next, "next cannot be nullptr");
        next_ = next;
    }
    bool trySetNext(Node* next) noexcept {
        RuntimeAssert(next, "next cannot be nullptr");
        if (next_) return false;
        next_ = next;
        return true;
    }

    int value_;
    // Use non-null marker to make sure inserting into the list properly updates this value.
    Node* next_ = reinterpret_cast<Node*>(0x1);
};

template <typename List>
std_support::list<typename List::value_type> create(std::initializer_list<int> list) {
    std_support::list<typename List::value_type> result;
    for (auto x : list) {
        result.emplace_back(x);
    }
    return result;
}

MATCHER_P(isEmpty, expected, (expected == !negation) ? "is empty" : "is not empty") {
    bool actual = arg.empty();
    *result_listener << (actual ? "is empty" : "is not empty");
    return expected == actual;
}

MATCHER_P(hasSize, expected, "") {
    size_t actual = std::distance(arg.begin(), arg.end());
    *result_listener << "of size " << actual;
    return expected == actual;
}

MATCHER_P(derefsTo, expected, "") {
    const auto& actual = *arg;
    *result_listener << "derefs to " << testing::PrintToString(actual);
    return expected == actual;
}

template <typename List, typename... Args>
auto elementsAre(Args... args) {
    return testing::AllOf(
        isEmpty(sizeof...(args) == 0),
        hasSize(sizeof...(args)),
        testing::ElementsAre(derefsTo(args)...)
    );
}

#define EXPECT_ELEMENTS_ARE(list, ...) \
    EXPECT_THAT(list, ::elementsAre<decltype(list)>(__VA_ARGS__));

} // namespace

TEST(IntrusiveForwardListTest, CTAD) {
    auto values = create<intrusive_forward_list<Node>>({1, 2, 3, 4});
    intrusive_forward_list list(values.begin(), values.end());
    static_assert(std::is_same_v<decltype(list)::value_type, Node>);
}

// Testing that operations on `intrusive_forward_list` give the same results as those on `std::forward_list`.
template <typename T>
class ForwardListTest : public testing::Test {};

using ForwardListTestTypes = testing::Types<std::forward_list<Element>, intrusive_forward_list<Node>>;
struct ForwardListTestNames {
    template <typename T>
    static std::string GetName(int) {
        if constexpr (std::is_same_v<T, std::forward_list<Element>>) {
            return "forward_list";
        } else if constexpr (std::is_same_v<T, intrusive_forward_list<Node>>) {
            return "intrusive_forward_list";
        } else {
            return "unknown";
        }
    }
};
TYPED_TEST_SUITE(ForwardListTest, ForwardListTestTypes, ForwardListTestNames);

TYPED_TEST(ForwardListTest, DefaultCtor) {
    using List = TypeParam;
    List list;
    EXPECT_ELEMENTS_ARE(list);
}

TYPED_TEST(ForwardListTest, RangeCtor) {
    using List = TypeParam;
    auto values = create<List>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    EXPECT_ELEMENTS_ARE(list, 1, 2, 3, 4);
}

TYPED_TEST(ForwardListTest, RangeCtorEmpty) {
    using List = TypeParam;
    auto values = create<List>({});
    List list(values.begin(), values.end());
    EXPECT_ELEMENTS_ARE(list);
}

TYPED_TEST(ForwardListTest, MoveCtor) {
    using List = TypeParam;
    auto values = create<List>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    List otherList(std::move(list));
    EXPECT_ELEMENTS_ARE(list);
    EXPECT_ELEMENTS_ARE(otherList, 1, 2, 3, 4);
}

TYPED_TEST(ForwardListTest, MoveAssignment) {
    using List = TypeParam;
    auto values = create<List>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    auto otherValues = create<List>({5, 6, 7, 8});
    List otherList(otherValues.begin(), otherValues.end());
    otherList = std::move(list);
    EXPECT_ELEMENTS_ARE(list);
    EXPECT_ELEMENTS_ARE(otherList, 1, 2, 3, 4);
}

TYPED_TEST(ForwardListTest, MutableIterator) {
    using List = TypeParam;
    auto values = create<List>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    for (auto& x : list) {
        *x += 5;
    }
    EXPECT_ELEMENTS_ARE(list, 6, 7, 8, 9);
}

TYPED_TEST(ForwardListTest, MutableIteratorEmpty) {
    using List = TypeParam;
    List list;
    for (auto& x : list) {
        *x += 5;
    }
    EXPECT_ELEMENTS_ARE(list);
}

TYPED_TEST(ForwardListTest, BeforeBeginIterator) {
    using List = TypeParam;
    auto values = create<List>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    auto it = list.before_begin();
    ++it;
    EXPECT_THAT(it, list.begin());
}

TYPED_TEST(ForwardListTest, BeforeBeginIteratorEmpty) {
    using List = TypeParam;
    List list;
    auto it = list.before_begin();
    ++it;
    EXPECT_THAT(it, list.end());
}

TYPED_TEST(ForwardListTest, MoveAssignmentIntoEmpty) {
    using List = TypeParam;
    auto values = create<List>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    List otherList;
    otherList = std::move(list);
    EXPECT_ELEMENTS_ARE(list);
    EXPECT_ELEMENTS_ARE(otherList, 1, 2, 3, 4);
}

TYPED_TEST(ForwardListTest, MoveAssignmentFromEmpty) {
    using List = TypeParam;
    List list;
    auto otherValues = create<List>({5, 6, 7, 8});
    List otherList(otherValues.begin(), otherValues.end());
    otherList = std::move(list);
    EXPECT_ELEMENTS_ARE(list);
    EXPECT_ELEMENTS_ARE(otherList);
}

TYPED_TEST(ForwardListTest, Swap) {
    using List = TypeParam;
    auto values = create<List>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    auto otherValues = create<List>({5, 6, 7, 8});
    List otherList(otherValues.begin(), otherValues.end());
    using std::swap;
    swap(list, otherList);
    EXPECT_ELEMENTS_ARE(list, 5, 6, 7, 8);
    EXPECT_ELEMENTS_ARE(otherList, 1, 2, 3, 4);
}

TYPED_TEST(ForwardListTest, SwapFirstEmpty) {
    using List = TypeParam;
    List list;
    auto otherValues = create<List>({5, 6, 7, 8});
    List otherList(otherValues.begin(), otherValues.end());
    using std::swap;
    swap(list, otherList);
    EXPECT_ELEMENTS_ARE(list, 5, 6, 7, 8);
    EXPECT_ELEMENTS_ARE(otherList);
}

TYPED_TEST(ForwardListTest, SwapSecondEmpty) {
    using List = TypeParam;
    auto values = create<List>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    List otherList;
    using std::swap;
    swap(list, otherList);
    EXPECT_ELEMENTS_ARE(list);
    EXPECT_ELEMENTS_ARE(otherList, 1, 2, 3, 4);
}

TYPED_TEST(ForwardListTest, SwapBothEmpty) {
    using List = TypeParam;
    List list;
    List otherList;
    using std::swap;
    swap(list, otherList);
    EXPECT_ELEMENTS_ARE(list);
    EXPECT_ELEMENTS_ARE(otherList);
}

TYPED_TEST(ForwardListTest, Assign) {
    using List = TypeParam;
    auto values = create<List>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    auto newValues = create<List>({5, 6, 7, 8});
    list.assign(newValues.begin(), newValues.end());
    EXPECT_ELEMENTS_ARE(list, 5, 6, 7, 8);
}

TYPED_TEST(ForwardListTest, AssignFromEmpty) {
    using List = TypeParam;
    auto values = create<List>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    auto newValues = create<List>({});
    list.assign(newValues.begin(), newValues.end());
    EXPECT_ELEMENTS_ARE(list);
}

TYPED_TEST(ForwardListTest, AssignIntoEmpty) {
    using List = TypeParam;
    List list;
    auto newValues = create<List>({5, 6, 7, 8});
    list.assign(newValues.begin(), newValues.end());
    EXPECT_ELEMENTS_ARE(list, 5, 6, 7, 8);
}

TYPED_TEST(ForwardListTest, Front) {
    using List = TypeParam;
    auto values = create<List>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    EXPECT_THAT(*list.front(), 1);
    const List& constList = list;
    EXPECT_THAT(*constList.front(), 1);
}

TYPED_TEST(ForwardListTest, Clear) {
    using List = TypeParam;
    auto values = create<List>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    list.clear();
    EXPECT_ELEMENTS_ARE(list);
}

TYPED_TEST(ForwardListTest, ClearEmpty) {
    using List = TypeParam;
    List list;
    list.clear();
    EXPECT_ELEMENTS_ARE(list);
}

TYPED_TEST(ForwardListTest, PushFront) {
    using List = TypeParam;
    auto values = create<List>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    typename List::value_type value(5);
    list.push_front(value);
    EXPECT_ELEMENTS_ARE(list, 5, 1, 2, 3, 4);
}

TYPED_TEST(ForwardListTest, PushFrontEmpty) {
    using List = TypeParam;
    List list;
    typename List::value_type value(5);
    list.push_front(value);
    EXPECT_ELEMENTS_ARE(list, 5);
}

TYPED_TEST(ForwardListTest, PopFront) {
    using List = TypeParam;
    auto values = create<List>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    list.pop_front();
    EXPECT_ELEMENTS_ARE(list, 2, 3, 4);
}

TYPED_TEST(ForwardListTest, PopFrontIntoEmpty) {
    using List = TypeParam;
    auto values = create<List>({1});
    List list(values.begin(), values.end());
    list.pop_front();
    EXPECT_ELEMENTS_ARE(list);
}

TYPED_TEST(ForwardListTest, Remove) {
    using List = TypeParam;
    auto values = create<List>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    auto& value = *std::next(list.begin(), 2);
    ASSERT_THAT(*value, 3);
    list.remove(value);
    EXPECT_ELEMENTS_ARE(list, 1, 2, 4);
}

TYPED_TEST(ForwardListTest, RemoveMissing) {
    using List = TypeParam;
    auto values = create<List>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    typename List::value_type value(5);
    list.remove(value);
    EXPECT_ELEMENTS_ARE(list, 1, 2, 3, 4);
}

TYPED_TEST(ForwardListTest, RemoveAll) {
    using List = TypeParam;
    auto values = create<List>({1});
    List list(values.begin(), values.end());
    auto& value = list.front();
    list.remove(value);
    EXPECT_ELEMENTS_ARE(list);
}

TYPED_TEST(ForwardListTest, RemoveEmpty) {
    using List = TypeParam;
    List list;
    typename List::value_type value(5);
    list.remove(value);
    EXPECT_ELEMENTS_ARE(list);
}

TYPED_TEST(ForwardListTest, RemoveIf) {
    using List = TypeParam;
    auto values = create<List>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    list.remove_if([](const auto& x) { return *x == 3; });
    EXPECT_ELEMENTS_ARE(list, 1, 2, 4);
}

TYPED_TEST(ForwardListTest, RemoveIfMissing) {
    using List = TypeParam;
    auto values = create<List>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    list.remove_if([](const auto& x) { return *x == 5; });
    EXPECT_ELEMENTS_ARE(list, 1, 2, 3, 4);
}

TYPED_TEST(ForwardListTest, RemoveIfAll) {
    using List = TypeParam;
    auto values = create<List>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    list.remove_if([](const auto& x) { return true; });
    EXPECT_ELEMENTS_ARE(list);
}

TYPED_TEST(ForwardListTest, RemoveIfEmpty) {
    using List = TypeParam;
    List list;
    list.remove_if([](const auto& x) { return *x == 3; });
    EXPECT_ELEMENTS_ARE(list);
}

TYPED_TEST(ForwardListTest, RemoveIfAllEmpty) {
    using List = TypeParam;
    List list;
    list.remove_if([](const auto& x) { return true; });
    EXPECT_ELEMENTS_ARE(list);
}

TYPED_TEST(ForwardListTest, InsertAfter) {
    using List = TypeParam;
    auto values = create<List>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    auto it = std::next(list.begin(), 2);
    ASSERT_THAT(**it, 3);
    typename List::value_type value(5);
    auto result = list.insert_after(it, value);
    EXPECT_THAT(result, std::next(list.begin(), 3));
    EXPECT_ELEMENTS_ARE(list, 1, 2, 3, 5, 4);
}

TYPED_TEST(ForwardListTest, InsertAfterFront) {
    using List = TypeParam;
    auto values = create<List>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    auto it = list.before_begin();
    typename List::value_type value(5);
    auto result = list.insert_after(it, value);
    EXPECT_THAT(result, list.begin());
    EXPECT_ELEMENTS_ARE(list, 5, 1, 2, 3, 4);
}

TYPED_TEST(ForwardListTest, InsertAfterFrontEmpty) {
    using List = TypeParam;
    List list;
    auto it = list.before_begin();
    typename List::value_type value(5);
    auto result = list.insert_after(it, value);
    EXPECT_THAT(result, list.begin());
    EXPECT_ELEMENTS_ARE(list, 5);
}

TYPED_TEST(ForwardListTest, InsertAfterRange) {
    using List = TypeParam;
    auto values = create<List>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    auto it = std::next(list.begin(), 2);
    ASSERT_THAT(**it, 3);
    auto otherValues = create<List>({5, 6, 7, 8});
    auto result = list.insert_after(it, otherValues.begin(), otherValues.end());
    EXPECT_THAT(result, std::next(list.begin(), 6));
    EXPECT_ELEMENTS_ARE(list, 1, 2, 3, 5, 6, 7, 8, 4);
}

TYPED_TEST(ForwardListTest, InsertAfterEmptyRange) {
    using List = TypeParam;
    auto values = create<List>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    auto it = std::next(list.begin(), 2);
    ASSERT_THAT(**it, 3);
    auto otherValues = create<List>({});
    auto result = list.insert_after(it, otherValues.begin(), otherValues.end());
    EXPECT_THAT(result, std::next(list.begin(), 2));
    EXPECT_ELEMENTS_ARE(list, 1, 2, 3, 4);
}

TYPED_TEST(ForwardListTest, InsertAfterRangeFront) {
    using List = TypeParam;
    auto values = create<List>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    auto it = list.before_begin();
    auto otherValues = create<List>({5, 6, 7, 8});
    auto result = list.insert_after(it, otherValues.begin(), otherValues.end());
    EXPECT_THAT(result, std::next(list.begin(), 3));
    EXPECT_ELEMENTS_ARE(list, 5, 6, 7, 8, 1, 2, 3, 4);
}

TYPED_TEST(ForwardListTest, InsertAfterEmptyRangeFront) {
    using List = TypeParam;
    auto values = create<List>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    auto it = list.before_begin();
    auto otherValues = create<List>({});
    auto result = list.insert_after(it, otherValues.begin(), otherValues.end());
    EXPECT_THAT(result, list.before_begin());
    EXPECT_ELEMENTS_ARE(list, 1, 2, 3, 4);
}

TYPED_TEST(ForwardListTest, InsertAfterRangeFrontEmpty) {
    using List = TypeParam;
    List list;
    auto it = list.before_begin();
    auto otherValues = create<List>({5, 6, 7, 8});
    auto result = list.insert_after(it, otherValues.begin(), otherValues.end());
    EXPECT_THAT(result, std::next(list.begin(), 3));
    EXPECT_ELEMENTS_ARE(list, 5, 6, 7, 8);
}

TYPED_TEST(ForwardListTest, InsertAfterEmptyRangeFrontEmpty) {
    using List = TypeParam;
    List list;
    auto it = list.before_begin();
    auto otherValues = create<List>({});
    auto result = list.insert_after(it, otherValues.begin(), otherValues.end());
    EXPECT_THAT(result, list.before_begin());
    EXPECT_ELEMENTS_ARE(list);
}

TYPED_TEST(ForwardListTest, EraseAfter) {
    using List = TypeParam;
    auto values = create<List>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    auto it = std::next(list.begin(), 1);
    ASSERT_THAT(**it, 2);
    auto result = list.erase_after(it);
    EXPECT_THAT(result, std::next(list.begin(), 2));
    EXPECT_ELEMENTS_ARE(list, 1, 2, 4);
}

TYPED_TEST(ForwardListTest, EraseAfterNearEnd) {
    using List = TypeParam;
    auto values = create<List>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    auto it = std::next(list.begin(), 2);
    ASSERT_THAT(**it, 3);
    auto result = list.erase_after(it);
    EXPECT_THAT(result, list.end());
    EXPECT_ELEMENTS_ARE(list, 1, 2, 3);
}

TYPED_TEST(ForwardListTest, EraseAfterFront) {
    using List = TypeParam;
    auto values = create<List>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    auto it = list.before_begin();
    auto result = list.erase_after(it);
    EXPECT_THAT(result, list.begin());
    EXPECT_ELEMENTS_ARE(list, 2, 3, 4);
}

TYPED_TEST(ForwardListTest, EraseAfterToEmpty) {
    using List = TypeParam;
    auto values = create<List>({1});
    List list(values.begin(), values.end());
    auto it = list.before_begin();
    auto result = list.erase_after(it);
    EXPECT_THAT(result, list.end());
    EXPECT_ELEMENTS_ARE(list);
}

TYPED_TEST(ForwardListTest, EraseAfterRange) {
    using List = TypeParam;
    auto values = create<List>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    auto it = std::next(list.begin(), 2);
    ASSERT_THAT(**it, 3);
    auto result = list.erase_after(list.begin(), it);
    EXPECT_THAT(result, std::next(list.begin(), 1));
    EXPECT_ELEMENTS_ARE(list, 1, 3, 4);
}

TYPED_TEST(ForwardListTest, EraseAfterRangeToEnd) {
    using List = TypeParam;
    auto values = create<List>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    auto result = list.erase_after(list.begin(), list.end());
    EXPECT_THAT(result, list.end());
    EXPECT_ELEMENTS_ARE(list, 1);
}

TYPED_TEST(ForwardListTest, EraseAfterEmptyRange) {
    using List = TypeParam;
    auto values = create<List>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    auto result = list.erase_after(std::next(list.begin(), 1), std::next(list.begin(), 2));
    EXPECT_THAT(result, std::next(list.begin(), 2));
    EXPECT_ELEMENTS_ARE(list, 1, 2, 3, 4);
}

TYPED_TEST(ForwardListTest, EraseAfterRangeFront) {
    using List = TypeParam;
    auto values = create<List>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    auto it = std::next(list.begin(), 2);
    ASSERT_THAT(**it, 3);
    auto result = list.erase_after(list.before_begin(), it);
    EXPECT_THAT(result, list.begin());
    EXPECT_ELEMENTS_ARE(list, 3, 4);
}

TYPED_TEST(ForwardListTest, EraseAfterRangeFrontToEnd) {
    using List = TypeParam;
    auto values = create<List>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    auto result = list.erase_after(list.before_begin(), list.end());
    EXPECT_THAT(result, list.end());
    EXPECT_ELEMENTS_ARE(list);
}

TYPED_TEST(ForwardListTest, EraseAfterEmptyRangeFront) {
    using List = TypeParam;
    auto values = create<List>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    auto result = list.erase_after(list.before_begin(), list.begin());
    EXPECT_THAT(result, list.begin());
    EXPECT_ELEMENTS_ARE(list, 1, 2, 3, 4);
}

TEST(IntrusiveForwardListTest, TryPushFrontSuccess) {
    using List = intrusive_forward_list<Node>;
    auto values = create<List>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    typename List::value_type value(5);
    value.clearNext();
    auto result = list.try_push_front(value);
    EXPECT_TRUE(result);
    EXPECT_ELEMENTS_ARE(list, 5, 1, 2, 3, 4);
}

TEST(IntrusiveForwardListTest, TryPushFrontFailure) {
    using List = intrusive_forward_list<Node>;
    auto values = create<List>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    typename List::value_type value(5);
    auto result = list.try_push_front(value);
    EXPECT_FALSE(result);
    EXPECT_ELEMENTS_ARE(list, 1, 2, 3, 4);
}

TEST(IntrusiveForwardListTest, TryPushFrontEmptySuccess) {
    using List = intrusive_forward_list<Node>;
    List list;
    typename List::value_type value(5);
    value.clearNext();
    auto result = list.try_push_front(value);
    EXPECT_TRUE(result);
    EXPECT_ELEMENTS_ARE(list, 5);
}

TEST(IntrusiveForwardListTest, TryPushFrontEmptyFailure) {
    using List = intrusive_forward_list<Node>;
    List list;
    typename List::value_type value(5);
    auto result = list.try_push_front(value);
    EXPECT_FALSE(result);
    EXPECT_ELEMENTS_ARE(list);
}

TEST(IntrusiveForwardListTest, TryPopFront) {
    using List = intrusive_forward_list<Node>;
    auto values = create<List>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    auto& front = list.front();
    auto result = list.try_pop_front();
    EXPECT_THAT(result, &front);
    EXPECT_ELEMENTS_ARE(list, 2, 3, 4);
}

TEST(IntrusiveForwardListTest, TryPopFrontIntoEmpty) {
    using List = intrusive_forward_list<Node>;
    auto values = create<List>({1});
    List list(values.begin(), values.end());
    auto& front = list.front();
    auto result = list.try_pop_front();
    EXPECT_THAT(result, &front);
    EXPECT_ELEMENTS_ARE(list);
}

TEST(IntrusiveForwardListTest, TryPopFrontFromEmpty) {
    using List = intrusive_forward_list<Node>;
    auto values = create<List>({});
    List list(values.begin(), values.end());
    auto result = list.try_pop_front();
    EXPECT_THAT(result, nullptr);
    EXPECT_ELEMENTS_ARE(list);
}

template<typename List>
typename List::iterator before_end(List& list) {
    auto cur = list.before_begin();
    auto next = list.begin();
    while (next != list.end()) {
        ++cur;
        ++next;
    }
    return cur;
}

TEST(IntrusiveForwardListTest, SpliceAfterBeforeBeginAll) {
    using List = intrusive_forward_list<Node>;
    auto values1 = create<List>({1, 2, 3, 4});
    auto values2 = create<List>({11, 12, 13, 14});
    List l1(values1.begin(), values1.end());
    List l2(values2.begin(), values2.end());

    auto spliced = l1.splice_after(l1.before_begin(), l2.before_begin(), l2.end(), values2.size());
    EXPECT_THAT(spliced, values2.size());
    EXPECT_ELEMENTS_ARE(l1, 11, 12, 13, 14, 1, 2, 3, 4);
    EXPECT_ELEMENTS_ARE(l2);
}

TEST(IntrusiveForwardListTest, SpliceAfterBeforeEndAll) {
    using List = intrusive_forward_list<Node>;
    auto values1 = create<List>({1, 2, 3, 4});
    auto values2 = create<List>({11, 12, 13, 14});
    List l1(values1.begin(), values1.end());
    List l2(values2.begin(), values2.end());

    auto spliced = l1.splice_after(before_end(l1), l2.before_begin(), l2.end(), values2.size());
    EXPECT_THAT(spliced, values2.size());
    EXPECT_ELEMENTS_ARE(l1, 1, 2, 3, 4, 11, 12, 13, 14);
    EXPECT_ELEMENTS_ARE(l2);
}

TEST(IntrusiveForwardListTest, SpliceAfterMidMidHalf) {
    using List = intrusive_forward_list<Node>;
    auto values1 = create<List>({1, 2, 3, 4});
    auto values2 = create<List>({11, 12, 13, 14});
    List l1(values1.begin(), values1.end());
    List l2(values2.begin(), values2.end());

    auto spliced = l1.splice_after(std::next(l1.begin()), l2.begin(), l2.end(), 2);
    EXPECT_THAT(spliced, 2);
    EXPECT_ELEMENTS_ARE(l1, 1, 2, 12, 13, 3, 4);
    EXPECT_ELEMENTS_ARE(l2, 11, 14);
}

TEST(IntrusiveForwardListTest, SpliceAfterBeforeBeginOwnTail) {
    using List = intrusive_forward_list<Node>;
    auto values = create<List>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    auto spliced = list.splice_after(list.before_begin(), std::next(list.begin()), list.end(), 2);
    EXPECT_THAT(spliced, 2);
    EXPECT_ELEMENTS_ARE(list, 3, 4, 1, 2);
}

TEST(IntrusiveForwardListTest, SpliceAfterLessThanAvailable) {
    using List = intrusive_forward_list<Node>;
    auto values = create<List>({1, 2, 3, 4});
    List from(values.begin(), values.end());
    List to;
    auto spliced = to.splice_after(to.before_begin(), from.before_begin(), from.end(), 2);
    EXPECT_THAT(spliced, 2);
    EXPECT_ELEMENTS_ARE(to, 1, 2);
    EXPECT_ELEMENTS_ARE(from, 3, 4);
}

TEST(IntrusiveForwardListTest, SpliceAfterMoreThanAvailable) {
    using List = intrusive_forward_list<Node>;
    auto values = create<List>({1, 2, 3, 4});
    List from(values.begin(), values.end());
    List to;
    auto spliced = to.splice_after(to.before_begin(), from.before_begin(), std::next(from.begin(), 2), 4);
    EXPECT_THAT(spliced, 2);
    EXPECT_ELEMENTS_ARE(to, 1, 2);
    EXPECT_ELEMENTS_ARE(from, 3, 4);
}
