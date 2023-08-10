/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#if KONAN_HAS_FOUNDATION_FRAMEWORK

#include "NSNotificationSubscription.hpp"

#import <Foundation/NSNotification.h>
#import <Foundation/NSString.h>
#include <memory>

#include "gmock/gmock.h"
#include "gtest/gtest.h"

using namespace kotlin;

using testing::_;

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

@interface Kotlin_objc_support_NSNotificationSubscriptionTest : NSObject {
    NSNotificationCenter* center_;
    std::function<void()> handler_;
}

- (instancetype)initWithNotificationCenter:(NSNotificationCenter*)center
                                      name:(NSNotificationName)name
                                   handler:(std::function<void()>)handler;

- (void)reset;

- (void)onNotification:(NSNotification*)notification;

@end

@implementation Kotlin_objc_support_NSNotificationSubscriptionTest

- (instancetype)initWithNotificationCenter:(NSNotificationCenter*)center
                                      name:(NSNotificationName)name
                                   handler:(std::function<void()>)handler {
    if ((self = [super init])) {
        center_ = center;
        handler_ = std::move(handler);

        [center_ addObserver:self selector:@selector(onNotification:) name:name object:nil];
    }
    return self;
}

- (void)reset {
    [center_ removeObserver:self];
}

- (void)onNotification:(NSNotification*)notification {
    handler_();
}

@end

class NSNotificationSubscriptionTest : public testing::Test {
public:
    objc_support::NSNotificationSubscription subscribe(const char* name, std::function<void()> handler) noexcept {
        return objc_support::NSNotificationSubscription(center_, [NSString stringWithUTF8String:name], std::move(handler));
    }

    objc_support::object_ptr<Kotlin_objc_support_NSNotificationSubscriptionTest> subscribeOther(
            const char* name, std::function<void()> handler) noexcept {
        return objc_support::object_ptr<Kotlin_objc_support_NSNotificationSubscriptionTest>(
                [[Kotlin_objc_support_NSNotificationSubscriptionTest alloc] initWithNotificationCenter:center_
                                                                                                  name:[NSString stringWithUTF8String:name]
                                                                                               handler:std::move(handler)]);
    }

    void post(const char* name) noexcept { [center_ postNotificationName:[NSString stringWithUTF8String:name] object:nil]; }

private:
    NSNotificationCenter* center_ = [[NSNotificationCenter alloc] init];
};

TEST_F(NSNotificationSubscriptionTest, Subscribed) {
    constexpr const char* name = "NOTIFICATION_NAME";
    testing::StrictMock<testing::MockFunction<void()>> handler;

    auto subscription = subscribe(name, handler.AsStdFunction());
    EXPECT_TRUE(subscription.subscribed());
    EXPECT_TRUE(subscription);

    subscription.reset();
    EXPECT_FALSE(subscription.subscribed());
    EXPECT_FALSE(subscription);
}

TEST_F(NSNotificationSubscriptionTest, Post) {
    constexpr const char* name = "NOTIFICATION_NAME";
    testing::StrictMock<testing::MockFunction<void()>> handler;

    auto subscription = subscribe(name, handler.AsStdFunction());

    EXPECT_CALL(handler, Call());
    post(name);
    testing::Mock::VerifyAndClearExpectations(&handler);
}

TEST_F(NSNotificationSubscriptionTest, PostWrongName) {
    constexpr const char* name = "NOTIFICATION_NAME";
    constexpr const char* wrongName = "NOTIFICATION_NAME_WRONG";
    testing::StrictMock<testing::MockFunction<void()>> handler;

    auto subscription = subscribe(name, handler.AsStdFunction());

    EXPECT_CALL(handler, Call()).Times(0);
    post(wrongName);
    testing::Mock::VerifyAndClearExpectations(&handler);
}

TEST_F(NSNotificationSubscriptionTest, PostAfterReset) {
    constexpr const char* name = "NOTIFICATION_NAME";
    testing::StrictMock<testing::MockFunction<void()>> handler;

    auto subscription = subscribe(name, handler.AsStdFunction());
    subscription.reset();

    EXPECT_CALL(handler, Call()).Times(0);
    post(name);
    testing::Mock::VerifyAndClearExpectations(&handler);
}

TEST_F(NSNotificationSubscriptionTest, PostAfterDtor) {
    constexpr const char* name = "NOTIFICATION_NAME";
    testing::StrictMock<testing::MockFunction<void()>> handler;

    {
        // Create and destroy subscription object.
        subscribe(name, handler.AsStdFunction());
    }

    EXPECT_CALL(handler, Call()).Times(0);
    post(name);
    testing::Mock::VerifyAndClearExpectations(&handler);
}

TEST_F(NSNotificationSubscriptionTest, MultipleSubscribers) {
    constexpr const char* name = "NOTIFICATION_NAME";
    testing::StrictMock<testing::MockFunction<void()>> handler1;
    testing::StrictMock<testing::MockFunction<void()>> handler2;
    testing::StrictMock<testing::MockFunction<void()>> handler3;
    testing::StrictMock<testing::MockFunction<void()>> handler4;

    auto subscription1 = subscribeOther(name, handler1.AsStdFunction());
    auto subscription2 = subscribe(name, handler2.AsStdFunction());
    auto subscription3 = subscribe(name, handler3.AsStdFunction());
    auto subscription4 = subscribeOther(name, handler4.AsStdFunction());

    EXPECT_CALL(handler1, Call());
    EXPECT_CALL(handler2, Call());
    EXPECT_CALL(handler3, Call());
    EXPECT_CALL(handler4, Call());
    post(name);
    testing::Mock::VerifyAndClearExpectations(&handler1);
    testing::Mock::VerifyAndClearExpectations(&handler2);
    testing::Mock::VerifyAndClearExpectations(&handler3);
    testing::Mock::VerifyAndClearExpectations(&handler4);

    subscription3.reset();

    EXPECT_CALL(handler1, Call());
    EXPECT_CALL(handler2, Call());
    EXPECT_CALL(handler4, Call());
    post(name);
    testing::Mock::VerifyAndClearExpectations(&handler1);
    testing::Mock::VerifyAndClearExpectations(&handler2);
    testing::Mock::VerifyAndClearExpectations(&handler4);

    [*subscription4 reset];
    subscription4.reset();

    EXPECT_CALL(handler1, Call());
    EXPECT_CALL(handler2, Call());
    post(name);
    testing::Mock::VerifyAndClearExpectations(&handler1);
    testing::Mock::VerifyAndClearExpectations(&handler2);

    subscription2.reset();

    EXPECT_CALL(handler1, Call());
    post(name);
    testing::Mock::VerifyAndClearExpectations(&handler1);

    // Make sure to unsubscribe.
    [*subscription1 reset];
}

TEST_F(NSNotificationSubscriptionTest, DestroysHandler) {
    constexpr const char* name = "NOTIFICATION_NAME";

    testing::StrictMock<testing::MockFunction<DestructorHook>> destructorHook;

    EXPECT_CALL(destructorHook, Call(_)).Times(0);
    auto subscription =
            subscribe(name, [withDestructorHook = std::make_shared<WithDestructorHook>(destructorHook.AsStdFunction())] {});
    post(name);
    testing::Mock::VerifyAndClearExpectations(&destructorHook);

    EXPECT_CALL(destructorHook, Call(_));
    subscription.reset();
    testing::Mock::VerifyAndClearExpectations(&destructorHook);
}

#endif
