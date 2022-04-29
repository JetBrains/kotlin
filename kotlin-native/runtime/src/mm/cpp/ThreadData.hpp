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
#include "ExtraObjectDataFactory.hpp"
#include "ShadowStack.hpp"
#include "StableRefRegistry.hpp"
#include "ThreadLocalStorage.hpp"
#include "Utils.hpp"
#include "ThreadSuspension.hpp"
#include "std_support/Vector.hpp"

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
        extraObjectDataThreadQueue_(ExtraObjectDataFactory::Instance()),
        gc_(GlobalData::Instance().gc(), *this),
        suspensionData_(ThreadState::kNative) {}

    ~ThreadData() = default;

    int threadId() const noexcept { return threadId_; }

    GlobalsRegistry::ThreadQueue& globalsThreadQueue() noexcept { return globalsThreadQueue_; }

    ThreadLocalStorage& tls() noexcept { return tls_; }

    StableRefRegistry::ThreadQueue& stableRefThreadQueue() noexcept { return stableRefThreadQueue_; }

    ExtraObjectDataFactory::ThreadQueue& extraObjectDataThreadQueue() noexcept { return extraObjectDataThreadQueue_; }

    ThreadState state() noexcept { return suspensionData_.state(); }

    ThreadState setState(ThreadState state) noexcept { return suspensionData_.setState(state); }

    ShadowStack& shadowStack() noexcept { return shadowStack_; }

    std_support::vector<std::pair<ObjHeader**, ObjHeader*>>& initializingSingletons() noexcept { return initializingSingletons_; }

    gc::GC::ThreadData& gc() noexcept { return gc_; }

    ThreadSuspensionData& suspensionData() { return suspensionData_; }

    void Publish() noexcept {
        // TODO: These use separate locks, which is inefficient.
        globalsThreadQueue_.Publish();
        stableRefThreadQueue_.Publish();
        extraObjectDataThreadQueue_.Publish();
        gc_.Publish();
    }

    void ClearForTests() noexcept {
        globalsThreadQueue_.ClearForTests();
        stableRefThreadQueue_.ClearForTests();
        extraObjectDataThreadQueue_.ClearForTests();
        gc_.ClearForTests();
    }

private:
    const int threadId_;
    GlobalsRegistry::ThreadQueue globalsThreadQueue_;
    ThreadLocalStorage tls_;
    StableRefRegistry::ThreadQueue stableRefThreadQueue_;
    ExtraObjectDataFactory::ThreadQueue extraObjectDataThreadQueue_;
    ShadowStack shadowStack_;
    gc::GC::ThreadData gc_;
    std_support::vector<std::pair<ObjHeader**, ObjHeader*>> initializingSingletons_;
    ThreadSuspensionData suspensionData_;
};

} // namespace mm
} // namespace kotlin

#endif // RUNTIME_MM_THREAD_DATA_H
