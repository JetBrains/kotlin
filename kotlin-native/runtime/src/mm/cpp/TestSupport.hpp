/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "../../main/cpp/TestSupport.hpp"

#include <thread>

#include "ThreadData.hpp"
#include "ThreadRegistry.hpp"

namespace kotlin {
namespace mm {

template <typename F>
void RunInNewThread(F f) {
    std::thread([&f]() {
        class ScopedRegistration : private kotlin::Pinned {
        public:
            ScopedRegistration() : node_(mm::ThreadRegistry::Instance().RegisterCurrentThread()) {}

            ~ScopedRegistration() { mm::ThreadRegistry::Instance().Unregister(node_); }

            mm::ThreadData& threadData() { return *node_->Get(); }

        private:
            mm::ThreadRegistry::Node* node_;
        } registration;

        f(registration.threadData());
    }).join();
}

} // namespace mm
} // namespace kotlin
