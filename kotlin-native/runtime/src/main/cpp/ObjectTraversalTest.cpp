/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ObjectTraversal.hpp"

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "ObjectTestSupport.hpp"
#include "Types.h"
#include "Utils.hpp"

using namespace kotlin;

using ::testing::_;

namespace {

struct CallableWithExceptions {
    void operator()(ObjHeader*) noexcept(false) {}
    void operator()(ObjHeader**) noexcept(false) {}
};

struct CallableWithoutExceptions {
    void operator()(ObjHeader*) noexcept {}
    void operator()(ObjHeader**) noexcept {}
};

struct EmptyPayload {
    using Field = ObjHeader* EmptyPayload::*;
    static constexpr std::array<Field, 0> kFields{};
};

struct Payload {
    ObjHeader* field1;
    ObjHeader* field2;
    ObjHeader* field3;

    static constexpr std::array kFields{
            &Payload::field1,
            &Payload::field2,
            &Payload::field3,
    };
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
    test_support::TypeInfoHolder type{test_support::TypeInfoHolder::ObjectBuilder<EmptyPayload>()};
    test_support::Object<EmptyPayload> object(type.typeInfo());
    testing::StrictMock<testing::MockFunction<void(ObjHeader**)>> process;

    EXPECT_CALL(process, Call(_)).Times(0);
    traverseObjectFields(object.header(), [&process](ObjHeader** field) { process.Call(field); });
}

TEST(ObjectTraversalTest, TraverseObjectFields) {
    test_support::TypeInfoHolder type{test_support::TypeInfoHolder::ObjectBuilder<Payload>()};
    ObjHeader field1;
    ObjHeader field3;
    test_support::Object<Payload> object(type.typeInfo());
    object->field1 = &field1;
    object->field3 = &field3;
    testing::StrictMock<testing::MockFunction<void(ObjHeader**)>> process;

    EXPECT_CALL(process, Call(&object->field1));
    EXPECT_CALL(process, Call(&object->field2));
    EXPECT_CALL(process, Call(&object->field3));
    traverseObjectFields(object.header(), [&process](ObjHeader** field) { process.Call(field); });
}

TEST(ObjectTraversalTest, TraverseObjectFieldsWithException) {
    constexpr int kException = 1;

    test_support::TypeInfoHolder type{test_support::TypeInfoHolder::ObjectBuilder<Payload>()};
    ObjHeader field1;
    ObjHeader field2;
    ObjHeader field3;
    test_support::Object<Payload> object(type.typeInfo());
    object->field1 = &field1;
    object->field2 = &field2;
    object->field3 = &field3;
    testing::StrictMock<testing::MockFunction<void(ObjHeader**)>> process;

    EXPECT_CALL(process, Call(&object->field1));
    EXPECT_CALL(process, Call(&object->field2)).WillOnce([]() { throw kException; });
    EXPECT_CALL(process, Call(&object->field3)).Times(0);
    try {
        traverseObjectFields(object.header(), [&process](ObjHeader** field) { process.Call(field); });
    } catch (int exception) {
        EXPECT_THAT(exception, kException);
    } catch (...) {
        EXPECT_TRUE(false);
    }
}

TEST(ObjectTraversalTest, TraverseEmptyArrayFields) {
    test_support::ObjectArray<0> array;
    testing::StrictMock<testing::MockFunction<void(ObjHeader**)>> process;

    EXPECT_CALL(process, Call(_)).Times(0);
    traverseObjectFields(array.header(), [&process](ObjHeader** field) { process.Call(field); });
}

TEST(ObjectTraversalTest, TraverseArrayFields) {
    ObjHeader element1;
    ObjHeader element3;
    test_support::ObjectArray<3> array;
    array.elements()[0] = &element1;
    array.elements()[2] = &element3;
    testing::StrictMock<testing::MockFunction<void(ObjHeader**)>> process;

    EXPECT_CALL(process, Call(&array.elements()[0]));
    EXPECT_CALL(process, Call(&array.elements()[1]));
    EXPECT_CALL(process, Call(&array.elements()[2]));
    traverseObjectFields(array.header(), [&process](ObjHeader** field) { process.Call(field); });
}

TEST(ObjectTraversalTest, TraverseArrayFieldsWithException) {
    constexpr int kException = 1;

    ObjHeader element1;
    ObjHeader element2;
    ObjHeader element3;
    test_support::ObjectArray<3> array;
    array.elements()[0] = &element1;
    array.elements()[1] = &element2;
    array.elements()[2] = &element3;
    testing::StrictMock<testing::MockFunction<void(ObjHeader**)>> process;

    EXPECT_CALL(process, Call(&array.elements()[0]));
    EXPECT_CALL(process, Call(&array.elements()[1])).WillOnce([]() { throw kException; });
    EXPECT_CALL(process, Call(&array.elements()[2])).Times(0);
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
    test_support::TypeInfoHolder type{test_support::TypeInfoHolder::ObjectBuilder<EmptyPayload>()};
    test_support::Object<EmptyPayload> object(type.typeInfo());
    testing::StrictMock<testing::MockFunction<void(ObjHeader*)>> process;

    EXPECT_CALL(process, Call(_)).Times(0);
    traverseReferredObjects(object.header(), [&process](ObjHeader* field) { process.Call(field); });
}

TEST(ObjectTraversalTest, TraverseObjectRefs) {
    test_support::TypeInfoHolder type{test_support::TypeInfoHolder::ObjectBuilder<Payload>()};
    ObjHeader field1;
    ObjHeader field3;
    test_support::Object<Payload> object(type.typeInfo());
    object->field1 = &field1;
    object->field3 = &field3;
    testing::StrictMock<testing::MockFunction<void(ObjHeader*)>> process;

    EXPECT_CALL(process, Call(&field1));
    EXPECT_CALL(process, Call(&field3));
    traverseReferredObjects(object.header(), [&process](ObjHeader* field) { process.Call(field); });
}

TEST(ObjectTraversalTest, TraverseObjectRefsWithException) {
    constexpr int kException = 1;

    test_support::TypeInfoHolder type{test_support::TypeInfoHolder::ObjectBuilder<Payload>()};
    ObjHeader field1;
    ObjHeader field2;
    ObjHeader field3;
    test_support::Object<Payload> object(type.typeInfo());
    object->field1 = &field1;
    object->field2 = &field2;
    object->field3 = &field3;
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
    test_support::ObjectArray<0> array;
    testing::StrictMock<testing::MockFunction<void(ObjHeader*)>> process;

    EXPECT_CALL(process, Call(_)).Times(0);
    traverseReferredObjects(array.header(), [&process](ObjHeader* field) { process.Call(field); });
}

TEST(ObjectTraversalTest, TraverseArrayRefs) {
    ObjHeader element1;
    ObjHeader element3;
    test_support::ObjectArray<3> array;
    array.elements()[0] = &element1;
    array.elements()[2] = &element3;
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
    test_support::ObjectArray<3> array;
    array.elements()[0] = &element1;
    array.elements()[1] = &element2;
    array.elements()[2] = &element3;
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
