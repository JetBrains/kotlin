/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ThreadRegistry.hpp"

#include "gtest/gtest.h"

#include "ScopedThread.hpp"
#include "ThreadData.hpp"

using namespace kotlin;

TEST(ThreadRegistryTest, RegisterCurrentThread) {
    ScopedThread([]() {
        class ScopedRegistration {
        public:
            ScopedRegistration() : node(mm::ThreadRegistry::Instance().RegisterCurrentThread()) {}
            ~ScopedRegistration() { mm::ThreadRegistry::Instance().Unregister(node); }
            mm::ThreadRegistry::Node* node;
        } registration;

        auto* threadData = registration.node->Get();
        EXPECT_EQ(konan::currentThreadId(), threadData->threadId());
        EXPECT_EQ(threadData, mm::ThreadRegistry::Instance().CurrentThreadData());
    });
}
