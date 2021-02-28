/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_MM_THREAD_REGISTRY_H
#define RUNTIME_MM_THREAD_REGISTRY_H

#include <pthread.h>

#include "SingleLockList.hpp"
#include "Utils.hpp"

namespace kotlin {
namespace mm {

class ThreadData;

class ThreadRegistry final : private Pinned {
public:
    using Node = SingleLockList<ThreadData>::Node;
    using Iterable = SingleLockList<ThreadData>::Iterable;

    static ThreadRegistry& Instance() noexcept;

    Node* RegisterCurrentThread() noexcept;

    // `ThreadData` associated with `threadDataNode` cannot be used after this call.
    void Unregister(Node* threadDataNode) noexcept;

    // Locks `ThreadRegistry` for safe iteration.
    Iterable Iter() noexcept;

    // Try not to use these methods very often, as (1) thread local access can be slow on some platforms,
    // (2) TLS gets deallocated before our thread destruction hooks run.
    // Using this after `Unregister` for the thread has been called is undefined behaviour.
    ALWAYS_INLINE ThreadData* CurrentThreadData() const noexcept;
    Node* CurrentThreadDataNode() const noexcept { return currentThreadDataNode_; }

private:
    friend class GlobalData;

    ThreadRegistry();
    ~ThreadRegistry();

    static thread_local Node* currentThreadDataNode_;

    SingleLockList<ThreadData> list_;
};

} // namespace mm
} // namespace kotlin

#endif // RUNTIME_MM_THREAD_REGISTRY_H
