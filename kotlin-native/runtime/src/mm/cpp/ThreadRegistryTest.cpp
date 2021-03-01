/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ThreadRegistry.hpp"

#include <pthread.h>
#include <thread>

#include "gtest/gtest.h"

#include "ThreadData.hpp"

using namespace kotlin;

TEST(ThreadRegistryTest, RegisterCurrentThread) {
    std::thread t([]() {
        class ScopedRegistration {
        public:
            ScopedRegistration() : node(mm::ThreadRegistry::Instance().RegisterCurrentThread()) {}
            ~ScopedRegistration() { mm::ThreadRegistry::Instance().Unregister(node); }
            mm::ThreadRegistry::Node* node;
        } registration;

        auto* threadData = registration.node->Get();
        EXPECT_EQ(pthread_self(), threadData->threadId());
        EXPECT_EQ(threadData, mm::ThreadRegistry::Instance().CurrentThreadData());
    });
    t.join();
}
