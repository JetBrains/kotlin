/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_MM_THREAD_DATA_H
#define RUNTIME_MM_THREAD_DATA_H

#include <atomic>

#include "GlobalData.hpp"
#include "GlobalsRegistry.hpp"
#include "GC.hpp"
#include "GCScheduler.hpp"
#include "ObjectFactory.hpp"
#include "ShadowStack.hpp"
#include "StableRefRegistry.hpp"
#include "ThreadLocalStorage.hpp"
#include "Types.h"
#include "Utils.hpp"
#include "ThreadSuspension.hpp"

struct ObjHeader;

namespace kotlin {
namespace mm {

// `ThreadData` is supposed to be thread local singleton.
// Pin it in memory to prevent accidental copying.
class ThreadData final : private Pinned {
public:
    explicit ThreadData(int threadId) noexcept :
        threadId_(threadId),
        globalsThreadQueue_(GlobalsRegistry::Instance()),
        stableRefThreadQueue_(StableRefRegistry::Instance()),
        gcScheduler_(GlobalData::Instance().gcScheduler().NewThreadData()),
        gc_(GlobalData::Instance().gc(), *this),
        objectFactoryThreadQueue_(GlobalData::Instance().objectFactory(), gc_),
        suspensionData_(ThreadState::kNative) {}

    ~ThreadData() = default;

    int threadId() const noexcept { return threadId_; }

    GlobalsRegistry::ThreadQueue& globalsThreadQueue() noexcept { return globalsThreadQueue_; }

    ThreadLocalStorage& tls() noexcept { return tls_; }

    StableRefRegistry::ThreadQueue& stableRefThreadQueue() noexcept { return stableRefThreadQueue_; }

    ThreadState state() noexcept { return suspensionData_.state(); }

    ThreadState setState(ThreadState state) noexcept { return suspensionData_.setState(state); }

    ObjectFactory<gc::GC>::ThreadQueue& objectFactoryThreadQueue() noexcept { return objectFactoryThreadQueue_; }

    ShadowStack& shadowStack() noexcept { return shadowStack_; }

    KStdVector<std::pair<ObjHeader**, ObjHeader*>>& initializingSingletons() noexcept { return initializingSingletons_; }

    gc::GCScheduler::ThreadData& gcScheduler() noexcept { return gcScheduler_; }

    gc::GC::ThreadData& gc() noexcept { return gc_; }

    ThreadSuspensionData& suspensionData() { return suspensionData_; }

    void Publish() noexcept {
        // TODO: These use separate locks, which is inefficient.
        globalsThreadQueue_.Publish();
        stableRefThreadQueue_.Publish();
        objectFactoryThreadQueue_.Publish();
    }

    void ClearForTests() noexcept {
        globalsThreadQueue_.ClearForTests();
        stableRefThreadQueue_.ClearForTests();
        objectFactoryThreadQueue_.ClearForTests();
    }

private:
    const int threadId_;
    GlobalsRegistry::ThreadQueue globalsThreadQueue_;
    ThreadLocalStorage tls_;
    StableRefRegistry::ThreadQueue stableRefThreadQueue_;
    ShadowStack shadowStack_;
    gc::GCScheduler::ThreadData gcScheduler_;
    gc::GC::ThreadData gc_;
    ObjectFactory<gc::GC>::ThreadQueue objectFactoryThreadQueue_;
    KStdVector<std::pair<ObjHeader**, ObjHeader*>> initializingSingletons_;
    ThreadSuspensionData suspensionData_;
};

} // namespace mm
} // namespace kotlin

#endif // RUNTIME_MM_THREAD_DATA_H
