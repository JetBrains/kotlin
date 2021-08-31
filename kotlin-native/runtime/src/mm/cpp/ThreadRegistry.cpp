/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ThreadRegistry.hpp"

#include "GlobalData.hpp"
#include "Porting.h"
#include "ThreadData.hpp"
#include "ThreadState.hpp"

using namespace kotlin;

// static
mm::ThreadRegistry& mm::ThreadRegistry::Instance() noexcept {
    return mm::GlobalData::Instance().threadRegistry();
}

mm::ThreadRegistry::Node* mm::ThreadRegistry::RegisterCurrentThread() noexcept {
    auto* threadDataNode = list_.Emplace(konan::currentThreadId());
    AssertThreadState(threadDataNode->Get(), ThreadState::kNative);
    Node*& currentDataNode = currentThreadDataNode_;
    RuntimeAssert(!IsCurrentThreadRegistered(), "This thread already had some data assigned to it.");
    currentDataNode = threadDataNode;
    return threadDataNode;
}

void mm::ThreadRegistry::Unregister(Node* threadDataNode) noexcept {
    AssertThreadState(threadDataNode->Get(), ThreadState::kNative);
    list_.Erase(threadDataNode);
    // Do not touch `currentThreadData_` as TLS may already have been deallocated.
}

mm::ThreadRegistry::Iterable mm::ThreadRegistry::LockForIter() noexcept {
    return list_.LockForIter();
}

std::unique_lock<mm::ThreadRegistry::Mutex> mm::ThreadRegistry::Lock() noexcept {
    return list_.Lock();
}

ALWAYS_INLINE mm::ThreadData* mm::ThreadRegistry::CurrentThreadData() const noexcept {
    auto* threadDataNode = CurrentThreadDataNode();
    return threadDataNode ? threadDataNode->Get() : nullptr;
}

mm::ThreadRegistry::ThreadRegistry() = default;
mm::ThreadRegistry::~ThreadRegistry() = default;

// static
THREAD_LOCAL_VARIABLE mm::ThreadRegistry::Node* mm::ThreadRegistry::currentThreadDataNode_ = nullptr;
