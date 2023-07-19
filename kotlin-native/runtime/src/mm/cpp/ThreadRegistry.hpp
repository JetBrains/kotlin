/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_MM_THREAD_REGISTRY_H
#define RUNTIME_MM_THREAD_REGISTRY_H

#include <pthread.h>

#include "Common.h"
#include "SingleLockList.hpp"
#include "Utils.hpp"

namespace kotlin {
namespace mm {

class ThreadData;

class ThreadRegistry final : private Pinned {
public:
    using Mutex = std::recursive_mutex;
    using Node = SingleLockList<ThreadData, Mutex>::Node;
    using Iterable = SingleLockList<ThreadData, Mutex>::Iterable;

    static ThreadRegistry& Instance() noexcept;

    Node* RegisterCurrentThread() noexcept;

    // `ThreadData` associated with `threadDataNode` cannot be used after this call.
    void Unregister(Node* threadDataNode) noexcept;

    // Locks `ThreadRegistry` for safe iteration.
    Iterable LockForIter() noexcept;

    std::unique_lock<Mutex> Lock() noexcept;

    // Try not to use these methods very often, as (1) thread local access can be slow on some platforms,
    // (2) TLS gets deallocated before our thread destruction hooks run.
    // Using this after `Unregister` for the thread has been called is undefined behaviour.
    // Using this by a thread which is not attached to the Kotlin runtime is undefined behaviour.
    ALWAYS_INLINE ThreadData* CurrentThreadData() const noexcept;
    Node* CurrentThreadDataNode() const noexcept {
        RuntimeAssert(currentThreadDataNode_ != nullptr, "Thread is not attached to the runtime");
        return currentThreadDataNode_;
    }
    Node* CurrentThreadDataNodeOrNull() const noexcept { return currentThreadDataNode_; }

    bool IsCurrentThreadRegistered() const noexcept { return currentThreadDataNode_ != nullptr; }

    static void ClearCurrentThreadData() { currentThreadDataNode_ = nullptr; }

    template <typename F>
    void waitAllThreads(F&& f) noexcept {
        // Disable new threads coming and going.
        auto iter = LockForIter();
        while (!std::all_of(iter.begin(), iter.end(), std::forward<F>(f))) {
            std::this_thread::yield();
        }
    }

private:
    friend class GlobalData;

    ThreadRegistry();
    ~ThreadRegistry();

    static THREAD_LOCAL_VARIABLE Node* currentThreadDataNode_ __attribute__((annotate("current_thread_tlv")));

    SingleLockList<ThreadData, Mutex> list_;
};

} // namespace mm
} // namespace kotlin

#endif // RUNTIME_MM_THREAD_REGISTRY_H
