/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ObjectTraversal.hpp"

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "Types.h"
#include "Utils.hpp"

using namespace kotlin;

using ::testing::_;

namespace {

template <size_t Count>
class Object : private Pinned {
public:
    Object() {
        header_.typeInfoOrMeta_ = &type_;
        type_.typeInfo_ = &type_;
        type_.objOffsetsCount_ = Count;
        type_.objOffsets_ = fieldOffsets_.data();
        for (size_t i = 0; i < Count; ++i) {
            fieldOffsets_[i] = reinterpret_cast<uintptr_t>(&fields_[i]) - reinterpret_cast<uintptr_t>(&header_);
        }
    }

    ObjHeader* header() { return &header_; }

    ObjHeader*& operator[](size_t index) { return fields_[index]; }

private:
    ObjHeader header_;
    TypeInfo type_;
    std::array<int32_t, Count> fieldOffsets_;
    std::array<ObjHeader*, Count> fields_{};
};

template <size_t Count>
class Array : private Pinned {
public:
    Array() {
        header_.typeInfoOrMeta_ = const_cast<TypeInfo*>(theArrayTypeInfo);
        header_.count_ = Count;
    }

    ObjHeader* header() { return header_.obj(); }

    ObjHeader*& operator[](size_t index) { return fields_[index]; }

private:
    ArrayHeader header_;
    std::array<ObjHeader*, Count> fields_{};
};

struct CallableWithExceptions {
    void operator()(ObjHeader*) noexcept(false) {}
    void operator()(ObjHeader**) noexcept(false) {}
};

struct CallableWithoutExceptions {
    void operator()(ObjHeader*) noexcept {}
    void operator()(ObjHeader**) noexcept {}
};

} // namespace

TEST(ObjectTraversalTest, TraverseFieldsExceptions) {
    static_assert(
            noexcept(traverseObjectFields(std::declval<ObjHeader*>(), std::declval<CallableWithoutExceptions>())),
            "Callable is noexcept, so traverse is noexcept");
    static_assert(
            !noexcept(traverseObjectFields(std::declval<ObjHeader*>(), std::declval<CallableWithExceptions>())),
            "Callable is noexcept(false), so traverse is noexcept(false)");
}

TEST(ObjectTraversalTest, TraverseEmptyObjectFields) {
    Object<0> object;
    testing::StrictMock<testing::MockFunction<void(ObjHeader**)>> process;

    EXPECT_CALL(process, Call(_)).Times(0);
    traverseObjectFields(object.header(), [&process](ObjHeader** field) { process.Call(field); });
}

TEST(ObjectTraversalTest, TraverseObjectFields) {
    ObjHeader field1;
    ObjHeader field3;
    Object<3> object;
    object[0] = &field1;
    object[2] = &field3;
    testing::StrictMock<testing::MockFunction<void(ObjHeader**)>> process;

    EXPECT_CALL(process, Call(&object[0]));
    EXPECT_CALL(process, Call(&object[1]));
    EXPECT_CALL(process, Call(&object[2]));
    traverseObjectFields(object.header(), [&process](ObjHeader** field) { process.Call(field); });
}

TEST(ObjectTraversalTest, TraverseObjectFieldsWithException) {
    constexpr int kException = 1;

    ObjHeader field1;
    ObjHeader field2;
    ObjHeader field3;
    Object<3> object;
    object[0] = &field1;
    object[1] = &field2;
    object[2] = &field3;
    testing::StrictMock<testing::MockFunction<void(ObjHeader**)>> process;

    EXPECT_CALL(process, Call(&object[0]));
    EXPECT_CALL(process, Call(&object[1])).WillOnce([]() { throw kException; });
    EXPECT_CALL(process, Call(&object[2])).Times(0);
    try {
        traverseObjectFields(object.header(), [&process](ObjHeader** field) { process.Call(field); });
    } catch (int exception) {
        EXPECT_THAT(exception, kException);
    } catch (...) {
        EXPECT_TRUE(false);
    }
}

TEST(ObjectTraversalTest, TraverseEmptyArrayFields) {
    Array<0> array;
    testing::StrictMock<testing::MockFunction<void(ObjHeader**)>> process;

    EXPECT_CALL(process, Call(_)).Times(0);
    traverseObjectFields(array.header(), [&process](ObjHeader** field) { process.Call(field); });
}

TEST(ObjectTraversalTest, TraverseArrayFields) {
    ObjHeader element1;
    ObjHeader element3;
    Array<3> array;
    array[0] = &element1;
    array[2] = &element3;
    testing::StrictMock<testing::MockFunction<void(ObjHeader**)>> process;

    EXPECT_CALL(process, Call(&array[0]));
    EXPECT_CALL(process, Call(&array[1]));
    EXPECT_CALL(process, Call(&array[2]));
    traverseObjectFields(array.header(), [&process](ObjHeader** field) { process.Call(field); });
}

TEST(ObjectTraversalTest, TraverseArrayFieldsWithException) {
    constexpr int kException = 1;

    ObjHeader element1;
    ObjHeader element2;
    ObjHeader element3;
    Array<3> array;
    array[0] = &element1;
    array[1] = &element2;
    array[2] = &element3;
    testing::StrictMock<testing::MockFunction<void(ObjHeader**)>> process;

    EXPECT_CALL(process, Call(&array[0]));
    EXPECT_CALL(process, Call(&array[1])).WillOnce([]() { throw kException; });
    EXPECT_CALL(process, Call(&array[2])).Times(0);
    try {
        traverseObjectFields(array.header(), [&process](ObjHeader** field) { process.Call(field); });
    } catch (int exception) {
        EXPECT_THAT(exception, kException);
    } catch (...) {
        EXPECT_TRUE(false);
    }
}

TEST(ObjectTraversalTest, TraverseRefsExceptions) {
    static_assert(
            noexcept(traverseReferredObjects(std::declval<ObjHeader*>(), std::declval<CallableWithoutExceptions>())),
            "Callable is noexcept, so traverse is noexcept");
    static_assert(
            !noexcept(traverseReferredObjects(std::declval<ObjHeader*>(), std::declval<CallableWithExceptions>())),
            "Callable is noexcept(false), so traverse is noexcept(false)");
}

TEST(ObjectTraversalTest, TraverseEmptyObjectRefs) {
    Object<0> object;
    testing::StrictMock<testing::MockFunction<void(ObjHeader*)>> process;

    EXPECT_CALL(process, Call(_)).Times(0);
    traverseReferredObjects(object.header(), [&process](ObjHeader* field) { process.Call(field); });
}

TEST(ObjectTraversalTest, TraverseObjectRefs) {
    ObjHeader field1;
    ObjHeader field3;
    Object<3> object;
    object[0] = &field1;
    object[2] = &field3;
    testing::StrictMock<testing::MockFunction<void(ObjHeader*)>> process;

    EXPECT_CALL(process, Call(&field1));
    EXPECT_CALL(process, Call(&field3));
    traverseReferredObjects(object.header(), [&process](ObjHeader* field) { process.Call(field); });
}

TEST(ObjectTraversalTest, TraverseObjectRefsWithException) {
    constexpr int kException = 1;

    ObjHeader field1;
    ObjHeader field2;
    ObjHeader field3;
    Object<3> object;
    object[0] = &field1;
    object[1] = &field2;
    object[2] = &field3;
    testing::StrictMock<testing::MockFunction<void(ObjHeader*)>> process;

    EXPECT_CALL(process, Call(&field1));
    EXPECT_CALL(process, Call(&field2)).WillOnce([]() { throw kException; });
    EXPECT_CALL(process, Call(&field3)).Times(0);
    try {
        traverseReferredObjects(object.header(), [&process](ObjHeader* field) { process.Call(field); });
    } catch (int exception) {
        EXPECT_THAT(exception, kException);
    } catch (...) {
        EXPECT_TRUE(false);
    }
}

TEST(ObjectTraversalTest, TraverseEmptyArrayRefs) {
    Array<0> array;
    testing::StrictMock<testing::MockFunction<void(ObjHeader*)>> process;

    EXPECT_CALL(process, Call(_)).Times(0);
    traverseReferredObjects(array.header(), [&process](ObjHeader* field) { process.Call(field); });
}

TEST(ObjectTraversalTest, TraverseArrayRefs) {
    ObjHeader element1;
    ObjHeader element3;
    Array<3> array;
    array[0] = &element1;
    array[2] = &element3;
    testing::StrictMock<testing::MockFunction<void(ObjHeader*)>> process;

    EXPECT_CALL(process, Call(&element1));
    EXPECT_CALL(process, Call(&element3));
    traverseReferredObjects(array.header(), [&process](ObjHeader* field) { process.Call(field); });
}

TEST(ObjectTraversalTest, TraverseArrayRefsWithException) {
    constexpr int kException = 1;

    ObjHeader element1;
    ObjHeader element2;
    ObjHeader element3;
    Array<3> array;
    array[0] = &element1;
    array[1] = &element2;
    array[2] = &element3;
    testing::StrictMock<testing::MockFunction<void(ObjHeader*)>> process;

    EXPECT_CALL(process, Call(&element1));
    EXPECT_CALL(process, Call(&element2)).WillOnce([]() { throw kException; });
    EXPECT_CALL(process, Call(&element3)).Times(0);
    try {
        traverseReferredObjects(array.header(), [&process](ObjHeader* field) { process.Call(field); });
    } catch (int exception) {
        EXPECT_THAT(exception, kException);
    } catch (...) {
        EXPECT_TRUE(false);
    }
}
