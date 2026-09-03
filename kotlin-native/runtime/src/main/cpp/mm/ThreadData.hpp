/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_MM_THREAD_DATA_H
#define RUNTIME_MM_THREAD_DATA_H

#include <atomic>
#include <cstdint>
#include <vector>

#include "Common.h"
#include "KAssert.h"
#include "mm/GlobalData.hpp"
#include "mm/GlobalsRegistry.hpp"
#include "gc/GC.hpp"
#include "mm/ShadowStack.hpp"
#include "mm/ExternalRCRefRegistry.hpp"
#include "mm/ThreadLocalStorage.hpp"
#include "Utils.hpp"
#include "mm/ThreadSuspension.hpp"

struct ObjHeader;

namespace kotlin {
namespace mm {

struct KotlinFrameAnchor {
    uint64_t* fp;
    uint64_t* pc;

    KotlinFrameAnchor() = default;
    KotlinFrameAnchor(uint64_t* fp, uint64_t* pc) : fp(fp), pc(pc) {}

    ALWAYS_INLINE static KotlinFrameAnchor getKotlinFrameAnchor() {
        uint64_t* fp = reinterpret_cast<uint64_t*>(__builtin_frame_address(0));
        return KotlinFrameAnchor{(uint64_t*) fp[0], (uint64_t*) fp[1]};
    }
};

// `ThreadData` is supposed to be thread local singleton.
// Pin it in memory to prevent accidental copying.
class ThreadData final : private Pinned {
public:
    explicit ThreadData(uintptr_t threadId) noexcept :
        threadId_(threadId),
        globalsThreadQueue_(GlobalsRegistry::Instance()),
        externalRCRefRegistry_(ExternalRCRefRegistry::instance()),
        gcScheduler_(GlobalData::Instance().gcScheduler(), *this),
        allocator_(GlobalData::Instance().allocator()),
        gc_(GlobalData::Instance().gc(), *this),
        suspensionData_(ThreadState::kNative, *this) {}

    ~ThreadData() = default;

    uintptr_t threadId() const noexcept { return threadId_; }

    GlobalsRegistry::ThreadQueue& globalsThreadQueue() noexcept { return globalsThreadQueue_; }

    ThreadLocalStorage& tls() noexcept { return tls_; }

    ExternalRCRefRegistry::ThreadQueue& externalRCRefRegistry() noexcept { return externalRCRefRegistry_; }

    ThreadState state() noexcept { return suspensionData_.state(); }

    ThreadState setState(ThreadState state) noexcept { return suspensionData_.setState(state); }

    ShadowStack& shadowStack() noexcept { return shadowStack_; }

    std::vector<std::pair<ObjHeader**, ObjHeader*>>& initializingSingletons() noexcept { return initializingSingletons_; }

    gcScheduler::GCScheduler::ThreadData& gcScheduler() noexcept { return gcScheduler_; }

    alloc::Allocator::ThreadData& allocator() noexcept { return allocator_; }

    gc::GC::ThreadData& gc() noexcept { return gc_; }

    ThreadSuspensionData& suspensionData() { return suspensionData_; }

    void Publish() noexcept {
        // TODO: These use separate locks, which is inefficient.
        globalsThreadQueue_.Publish();
        externalRCRefRegistry_.publish();
    }

    void ClearForTests() noexcept {
        globalsThreadQueue_.ClearForTests();
        externalRCRefRegistry_.clearForTests();
        allocator_.clearForTests();
    }

    void pushStackMapAnchor(uint64_t* fp, uint64_t* pc) noexcept {
        frameAnchors_.emplace_back(fp, pc);
    }

    void pushLastStackMapAnchor() noexcept {
        RuntimeAssert(lastFrame_.fp != nullptr, "Trying push last anchor, but last anchor is not initialized");
        frameAnchors_.emplace_back(lastFrame_);
    }

    void popStackMapAnchor() noexcept {
        frameAnchors_.pop_back();
    }

    const std::vector<KotlinFrameAnchor>& frameAnchors() {
        return frameAnchors_;
    }

    void setLastFrame(KotlinFrameAnchor anchor) noexcept {
        lastFrame_ = anchor;
    }

private:
    const uintptr_t threadId_;
    GlobalsRegistry::ThreadQueue globalsThreadQueue_;
    ThreadLocalStorage tls_;
    ExternalRCRefRegistry::ThreadQueue externalRCRefRegistry_;
    ShadowStack shadowStack_;
    gcScheduler::GCScheduler::ThreadData gcScheduler_;
    alloc::Allocator::ThreadData allocator_;
    gc::GC::ThreadData gc_;
    std::vector<std::pair<ObjHeader**, ObjHeader*>> initializingSingletons_;
    ThreadSuspensionData suspensionData_;
    std::vector<KotlinFrameAnchor> frameAnchors_;
    KotlinFrameAnchor lastFrame_ = {};
};

} // namespace mm
} // namespace kotlin

#endif // RUNTIME_MM_THREAD_DATA_H
