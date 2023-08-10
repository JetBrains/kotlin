/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#if KONAN_HAS_FOUNDATION_FRAMEWORK

#include "ObjectPtr.hpp"

#import <Foundation/NSObject.h>
#include <functional>
#include <memory>

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "Utils.hpp"

using namespace kotlin;

namespace {

class WithDestructorHook;

using DestructorHook = void(WithDestructorHook*);

class WithDestructorHook : private Pinned {
public:
    explicit WithDestructorHook(std::function<DestructorHook> hook) : hook_(std::move(hook)) {}

    ~WithDestructorHook() { hook_(this); }

private:
    std::function<DestructorHook> hook_;
};

} // namespace

@interface WithDestructorHookObjC : NSObject {
    std::unique_ptr<WithDestructorHook> impl_;
}

@property(readonly) WithDestructorHook* impl;

- (instancetype)initWithDestructorHook:(std::function<DestructorHook>)hook;

@end

@implementation WithDestructorHookObjC

- (instancetype)initWithDestructorHook:(std::function<DestructorHook>)hook {
    if ((self = [super init])) {
        impl_ = std::make_unique<WithDestructorHook>(hook);
    }
    return self;
}

- (WithDestructorHook*)impl {
    return impl_.get();
}

@end

#define EXPECT_CONTAINS(smartPtr, ptr) \
    do { \
        EXPECT_THAT(static_cast<bool>(smartPtr), (ptr) != nil); \
        EXPECT_THAT(*(smartPtr), (ptr)); \
        EXPECT_THAT((smartPtr).get(), (ptr)); \
    } while (false)

TEST(ObjectPtrTest, DefaultCtor) {
    objc_support::object_ptr<WithDestructorHookObjC> obj;
    EXPECT_CONTAINS(obj, nil);
}

TEST(ObjectPtrTest, ObjectCtor) {
    testing::StrictMock<testing::MockFunction<DestructorHook>> destructorHook;

    auto* ptr = [[WithDestructorHookObjC alloc] initWithDestructorHook:destructorHook.AsStdFunction()];
    {
        objc_support::object_ptr<WithDestructorHookObjC> obj(ptr);
        EXPECT_CONTAINS(obj, ptr);
        EXPECT_CALL(destructorHook, Call(ptr.impl));
    }
    testing::Mock::VerifyAndClearExpectations(&destructorHook);
}

TEST(ObjectPtrTest, CopyCtor) {
    testing::StrictMock<testing::MockFunction<DestructorHook>> destructorHook;

    auto* ptr = [[WithDestructorHookObjC alloc] initWithDestructorHook:destructorHook.AsStdFunction()];
    {
        objc_support::object_ptr<WithDestructorHookObjC> obj1(ptr);
        objc_support::object_ptr<WithDestructorHookObjC> obj2(obj1);
        EXPECT_CONTAINS(obj1, ptr);
        EXPECT_CONTAINS(obj2, ptr);

        EXPECT_CALL(destructorHook, Call(ptr.impl)).Times(0);
        obj1.reset();
        testing::Mock::VerifyAndClearExpectations(&destructorHook);
        EXPECT_CONTAINS(obj1, nil);
        EXPECT_CONTAINS(obj2, ptr);

        EXPECT_CALL(destructorHook, Call(ptr.impl));
    }
    testing::Mock::VerifyAndClearExpectations(&destructorHook);
}

TEST(ObjectPtrTest, MoveCtor) {
    testing::StrictMock<testing::MockFunction<DestructorHook>> destructorHook;

    auto* ptr = [[WithDestructorHookObjC alloc] initWithDestructorHook:destructorHook.AsStdFunction()];
    {
        objc_support::object_ptr<WithDestructorHookObjC> obj1(ptr);

        EXPECT_CALL(destructorHook, Call(ptr.impl)).Times(0);
        objc_support::object_ptr<WithDestructorHookObjC> obj2(std::move(obj1));
        testing::Mock::VerifyAndClearExpectations(&destructorHook);
        EXPECT_CONTAINS(obj1, nil);
        EXPECT_CONTAINS(obj2, ptr);

        EXPECT_CALL(destructorHook, Call(ptr.impl));
    }
    testing::Mock::VerifyAndClearExpectations(&destructorHook);
}

TEST(ObjectPtrTest, CopyAssignmentIntoEmpty) {
    testing::StrictMock<testing::MockFunction<DestructorHook>> destructorHook;

    auto* ptr = [[WithDestructorHookObjC alloc] initWithDestructorHook:destructorHook.AsStdFunction()];
    {
        objc_support::object_ptr<WithDestructorHookObjC> obj2;
        {
            objc_support::object_ptr<WithDestructorHookObjC> obj1(ptr);
            obj2 = obj1;
            EXPECT_CONTAINS(obj1, ptr);
            EXPECT_CONTAINS(obj2, ptr);

            EXPECT_CALL(destructorHook, Call(ptr.impl)).Times(0);
        }
        testing::Mock::VerifyAndClearExpectations(&destructorHook);

        EXPECT_CONTAINS(obj2, ptr);

        EXPECT_CALL(destructorHook, Call(ptr.impl));
    }
    testing::Mock::VerifyAndClearExpectations(&destructorHook);
}

TEST(ObjectPtrTest, CopyAssignmentIntoSet) {
    testing::StrictMock<testing::MockFunction<DestructorHook>> destructorHook;

    auto* ptr1 = [[WithDestructorHookObjC alloc] initWithDestructorHook:destructorHook.AsStdFunction()];
    auto* ptr2 = [[WithDestructorHookObjC alloc] initWithDestructorHook:destructorHook.AsStdFunction()];
    {
        objc_support::object_ptr<WithDestructorHookObjC> obj2(ptr2);
        {
            objc_support::object_ptr<WithDestructorHookObjC> obj1(ptr1);

            EXPECT_CALL(destructorHook, Call(ptr2.impl));
            obj2 = obj1;
            testing::Mock::VerifyAndClearExpectations(&destructorHook);

            EXPECT_CONTAINS(obj1, ptr1);
            EXPECT_CONTAINS(obj2, ptr1);

            EXPECT_CALL(destructorHook, Call(ptr1.impl)).Times(0);
        }
        testing::Mock::VerifyAndClearExpectations(&destructorHook);

        EXPECT_CONTAINS(obj2, ptr1);

        EXPECT_CALL(destructorHook, Call(ptr1.impl));
    }
    testing::Mock::VerifyAndClearExpectations(&destructorHook);
}

TEST(ObjectPtrTest, MoveAssignmentIntoEmpty) {
    testing::StrictMock<testing::MockFunction<DestructorHook>> destructorHook;

    auto* ptr = [[WithDestructorHookObjC alloc] initWithDestructorHook:destructorHook.AsStdFunction()];
    {
        objc_support::object_ptr<WithDestructorHookObjC> obj2;
        {
            EXPECT_CALL(destructorHook, Call(ptr.impl)).Times(0);

            objc_support::object_ptr<WithDestructorHookObjC> obj1(ptr);
            obj2 = std::move(obj1);
            EXPECT_CONTAINS(obj1, nil);
            EXPECT_CONTAINS(obj2, ptr);
        }
        testing::Mock::VerifyAndClearExpectations(&destructorHook);

        EXPECT_CONTAINS(obj2, ptr);

        EXPECT_CALL(destructorHook, Call(ptr.impl));
    }
    testing::Mock::VerifyAndClearExpectations(&destructorHook);
}

TEST(ObjectPtrTest, MoveAssignmentIntoSet) {
    testing::StrictMock<testing::MockFunction<DestructorHook>> destructorHook;

    auto* ptr1 = [[WithDestructorHookObjC alloc] initWithDestructorHook:destructorHook.AsStdFunction()];
    auto* ptr2 = [[WithDestructorHookObjC alloc] initWithDestructorHook:destructorHook.AsStdFunction()];
    {
        objc_support::object_ptr<WithDestructorHookObjC> obj2(ptr2);
        {
            objc_support::object_ptr<WithDestructorHookObjC> obj1(ptr1);

            EXPECT_CALL(destructorHook, Call(ptr2.impl));
            EXPECT_CALL(destructorHook, Call(ptr1.impl)).Times(0);
            obj2 = std::move(obj1);
            testing::Mock::VerifyAndClearExpectations(&destructorHook);

            EXPECT_CONTAINS(obj1, nil);
            EXPECT_CONTAINS(obj2, ptr1);
        }
        testing::Mock::VerifyAndClearExpectations(&destructorHook);

        EXPECT_CONTAINS(obj2, ptr1);

        EXPECT_CALL(destructorHook, Call(ptr1.impl));
    }
    testing::Mock::VerifyAndClearExpectations(&destructorHook);
}

TEST(ObjectPtrTest, SwapEmptyEmpty) {
    objc_support::object_ptr<WithDestructorHookObjC> obj1;
    objc_support::object_ptr<WithDestructorHookObjC> obj2;
    obj1.swap(obj2);
    EXPECT_CONTAINS(obj1, nil);
    EXPECT_CONTAINS(obj2, nil);
}

TEST(ObjectPtrTest, SwapEmptySet) {
    testing::StrictMock<testing::MockFunction<DestructorHook>> destructorHook;

    auto* ptr2 = [[WithDestructorHookObjC alloc] initWithDestructorHook:destructorHook.AsStdFunction()];
    objc_support::object_ptr<WithDestructorHookObjC> obj1;
    objc_support::object_ptr<WithDestructorHookObjC> obj2(ptr2);
    obj1.swap(obj2);
    EXPECT_CONTAINS(obj1, ptr2);
    EXPECT_CONTAINS(obj2, nil);

    EXPECT_CALL(destructorHook, Call(ptr2.impl));
}

TEST(ObjectPtrTest, SwapSetEmpty) {
    testing::StrictMock<testing::MockFunction<DestructorHook>> destructorHook;

    auto* ptr1 = [[WithDestructorHookObjC alloc] initWithDestructorHook:destructorHook.AsStdFunction()];
    objc_support::object_ptr<WithDestructorHookObjC> obj1(ptr1);
    objc_support::object_ptr<WithDestructorHookObjC> obj2;
    obj1.swap(obj2);
    EXPECT_CONTAINS(obj1, nil);
    EXPECT_CONTAINS(obj2, ptr1);

    EXPECT_CALL(destructorHook, Call(ptr1.impl));
}

TEST(ObjectPtrTest, SwapSetSet) {
    testing::StrictMock<testing::MockFunction<DestructorHook>> destructorHook;

    auto* ptr1 = [[WithDestructorHookObjC alloc] initWithDestructorHook:destructorHook.AsStdFunction()];
    auto* ptr2 = [[WithDestructorHookObjC alloc] initWithDestructorHook:destructorHook.AsStdFunction()];
    objc_support::object_ptr<WithDestructorHookObjC> obj1(ptr1);
    objc_support::object_ptr<WithDestructorHookObjC> obj2(ptr2);
    obj1.swap(obj2);
    EXPECT_CONTAINS(obj1, ptr2);
    EXPECT_CONTAINS(obj2, ptr1);

    EXPECT_CALL(destructorHook, Call(ptr1.impl));
    EXPECT_CALL(destructorHook, Call(ptr2.impl));
}

TEST(ObjectPtrTest, ResetEmptyFromEmpty) {
    objc_support::object_ptr<WithDestructorHookObjC> obj;
    obj.reset();
    EXPECT_CONTAINS(obj, nil);
}

TEST(ObjectPtrTest, ResetEmptyFromSet) {
    testing::StrictMock<testing::MockFunction<DestructorHook>> destructorHook;

    auto* ptr = [[WithDestructorHookObjC alloc] initWithDestructorHook:destructorHook.AsStdFunction()];
    objc_support::object_ptr<WithDestructorHookObjC> obj(ptr);

    EXPECT_CALL(destructorHook, Call(ptr.impl));
    obj.reset();
    testing::Mock::VerifyAndClearExpectations(&destructorHook);
    EXPECT_CONTAINS(obj, nil);
}

TEST(ObjectPtrTest, ResetObjectFromEmpty) {
    testing::StrictMock<testing::MockFunction<DestructorHook>> destructorHook;

    auto* ptr = [[WithDestructorHookObjC alloc] initWithDestructorHook:destructorHook.AsStdFunction()];
    objc_support::object_ptr<WithDestructorHookObjC> obj;

    obj.reset(ptr);
    EXPECT_CONTAINS(obj, ptr);

    EXPECT_CALL(destructorHook, Call(ptr.impl));
}

TEST(ObjectPtrTest, ResetObjectFromSet) {
    testing::StrictMock<testing::MockFunction<DestructorHook>> destructorHook;

    auto* ptr1 = [[WithDestructorHookObjC alloc] initWithDestructorHook:destructorHook.AsStdFunction()];
    auto* ptr2 = [[WithDestructorHookObjC alloc] initWithDestructorHook:destructorHook.AsStdFunction()];
    objc_support::object_ptr<WithDestructorHookObjC> obj(ptr1);

    EXPECT_CALL(destructorHook, Call(ptr1.impl));
    obj.reset(ptr2);
    testing::Mock::VerifyAndClearExpectations(&destructorHook);
    EXPECT_CONTAINS(obj, ptr2);

    EXPECT_CALL(destructorHook, Call(ptr2.impl));
}

#define EXPECT_EQUAL(expr1, expr2) \
    do { \
        EXPECT_TRUE((expr1) == (expr2)); \
        EXPECT_FALSE((expr1) != (expr2)); \
        EXPECT_FALSE((expr1) < (expr2)); \
        EXPECT_TRUE((expr1) <= (expr2)); \
        EXPECT_FALSE((expr1) > (expr2)); \
        EXPECT_TRUE((expr1) >= (expr2)); \
    } while (false)

#define EXPECT_LESS(expr1, expr2) \
    do { \
        EXPECT_FALSE((expr1) == (expr2)); \
        EXPECT_TRUE((expr1) != (expr2)); \
        EXPECT_TRUE((expr1) < (expr2)); \
        EXPECT_TRUE((expr1) <= (expr2)); \
        EXPECT_FALSE((expr1) > (expr2)); \
        EXPECT_FALSE((expr1) >= (expr2)); \
    } while (false)

#define EXPECT_GREATER(expr1, expr2) \
    do { \
        EXPECT_FALSE((expr1) == (expr2)); \
        EXPECT_TRUE((expr1) != (expr2)); \
        EXPECT_FALSE((expr1) < (expr2)); \
        EXPECT_FALSE((expr1) <= (expr2)); \
        EXPECT_TRUE((expr1) > (expr2)); \
        EXPECT_TRUE((expr1) >= (expr2)); \
    } while (false)

TEST(ObjectPtrTest, ComparisonsEmpty) {
    testing::StrictMock<testing::MockFunction<DestructorHook>> destructorHook;

    auto* ptr = [[WithDestructorHookObjC alloc] initWithDestructorHook:destructorHook.AsStdFunction()];
    objc_support::object_ptr<WithDestructorHookObjC> empty;
    objc_support::object_ptr<WithDestructorHookObjC> reset(ptr);
    EXPECT_CALL(destructorHook, Call(ptr.impl));
    reset.reset();
    testing::Mock::VerifyAndClearExpectations(&destructorHook);
    objc_support::object_ptr<NSObject> emptyOtherType;

    EXPECT_EQUAL(empty, empty);
    EXPECT_EQUAL(empty, objc_support::object_ptr<WithDestructorHookObjC>());
    EXPECT_EQUAL(empty, reset);
    EXPECT_EQUAL(empty, emptyOtherType);
    EXPECT_EQUAL(reset, empty);
    EXPECT_EQUAL(reset, reset);
    EXPECT_EQUAL(reset, emptyOtherType);
}

TEST(ObjectPtrTest, ComparisonsEmptySet) {
    testing::StrictMock<testing::MockFunction<DestructorHook>> destructorHook;

    auto* ptr1 = [[WithDestructorHookObjC alloc] initWithDestructorHook:destructorHook.AsStdFunction()];
    auto* ptr2 = [[WithDestructorHookObjC alloc] initWithDestructorHook:destructorHook.AsStdFunction()];
    auto* ptr3 = [[WithDestructorHookObjC alloc] initWithDestructorHook:destructorHook.AsStdFunction()];
    objc_support::object_ptr<WithDestructorHookObjC> obj(ptr1);
    objc_support::object_ptr<WithDestructorHookObjC> empty;
    objc_support::object_ptr<WithDestructorHookObjC> reset(ptr2);
    EXPECT_CALL(destructorHook, Call(ptr2.impl));
    reset.reset();
    testing::Mock::VerifyAndClearExpectations(&destructorHook);
    objc_support::object_ptr<NSObject> emptyOtherType;
    objc_support::object_ptr<WithDestructorHookObjC> objOtherType(ptr3);

    EXPECT_GREATER(obj, empty);
    EXPECT_GREATER(obj, reset);
    EXPECT_GREATER(obj, emptyOtherType);
    EXPECT_LESS(empty, obj);
    EXPECT_LESS(empty, objOtherType);
    EXPECT_LESS(reset, obj);
    EXPECT_LESS(reset, objOtherType);
    EXPECT_GREATER(objOtherType, empty);
    EXPECT_GREATER(objOtherType, reset);

    EXPECT_CALL(destructorHook, Call(ptr1.impl));
    EXPECT_CALL(destructorHook, Call(ptr3.impl));
}

TEST(ObjectPtrTest, ComparisonsSetSet) {
    testing::StrictMock<testing::MockFunction<DestructorHook>> destructorHook;

    auto* ptr1 = [[WithDestructorHookObjC alloc] initWithDestructorHook:destructorHook.AsStdFunction()];
    auto* ptr2 = [[WithDestructorHookObjC alloc] initWithDestructorHook:destructorHook.AsStdFunction()];
    if (ptr2 < ptr1) {
        std::swap(ptr1, ptr2);
    }
    ASSERT_LT(ptr1, ptr2);

    objc_support::object_ptr<WithDestructorHookObjC> obj1(ptr1);
    objc_support::object_ptr<WithDestructorHookObjC> obj2(ptr2);
    objc_support::object_ptr<WithDestructorHookObjC> obj3(obj1);
    objc_support::object_ptr<NSObject> objOtherType([ptr1 retain]);

    EXPECT_EQUAL(obj1, obj1);
    EXPECT_LESS(obj1, obj2);
    EXPECT_EQUAL(obj1, obj3);
    EXPECT_EQUAL(obj1, objOtherType);
    EXPECT_GREATER(obj2, obj1);
    EXPECT_EQUAL(obj2, obj2);
    EXPECT_GREATER(obj2, obj3);
    EXPECT_GREATER(obj2, objOtherType);
    EXPECT_EQUAL(obj3, obj1);
    EXPECT_LESS(obj3, obj2);
    EXPECT_EQUAL(obj3, obj3);
    EXPECT_EQUAL(obj3, objOtherType);
    EXPECT_EQUAL(objOtherType, obj1);
    EXPECT_LESS(objOtherType, obj2);
    EXPECT_EQUAL(objOtherType, obj3);
    EXPECT_EQUAL(objOtherType, objOtherType);

    EXPECT_CALL(destructorHook, Call(ptr1.impl));
    EXPECT_CALL(destructorHook, Call(ptr2.impl));
}

TEST(ObjectPtrTest, Hashing) {
    testing::StrictMock<testing::MockFunction<DestructorHook>> destructorHook;

    auto* ptr1 = [[WithDestructorHookObjC alloc] initWithDestructorHook:destructorHook.AsStdFunction()];
    auto* ptr2 = [[WithDestructorHookObjC alloc] initWithDestructorHook:destructorHook.AsStdFunction()];
    objc_support::object_ptr<WithDestructorHookObjC> obj(ptr1);
    objc_support::object_ptr<WithDestructorHookObjC> empty;
    objc_support::object_ptr<WithDestructorHookObjC> reset(ptr2);
    EXPECT_CALL(destructorHook, Call(ptr2.impl));
    reset.reset();
    testing::Mock::VerifyAndClearExpectations(&destructorHook);

    using Hash = std::hash<objc_support::object_ptr<WithDestructorHookObjC>>;
    using HashImpl = std::hash<NSObject*>;

    EXPECT_THAT(Hash()(obj), HashImpl()(ptr1));
    EXPECT_THAT(Hash()(empty), HashImpl()(nullptr));
    EXPECT_THAT(Hash()(reset), HashImpl()(nullptr));

    EXPECT_CALL(destructorHook, Call(ptr1.impl));
}

#endif
