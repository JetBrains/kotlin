/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_MM_THREAD_DATA_H
#define RUNTIME_MM_THREAD_DATA_H

#include <atomic>
#include <cstdint>
#include <optional>
#include <pthread.h>
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

#if defined(__aarch64__)
struct KotlinFrameAnchor {
    uint64_t* fp;
    uint64_t* pc;

    KotlinFrameAnchor() = default;
    KotlinFrameAnchor(uint64_t* fp, uint64_t* pc) : fp(fp), pc(pc) {
        RuntimeCheck((uint64_t) fp % 16 == 0, "Frame pointer is not aligned on 16");
    }

    // Reads the frame record this anchor points to and validates it before anything is built from it.
    // `stackHigh` is the exclusive upper bound of the thread's stack. A frame record must lie above the
    // current one, be 16-byte aligned and stay inside the stack; anything else means the chain is broken.
    std::optional<KotlinFrameAnchor> next(const uint64_t* stackHigh) const {
        RuntimeAssert(fp != nullptr, "Current fp is null, cannot get next");
        auto* nextFp = reinterpret_cast<uint64_t*>(fp[0]);
        auto* nextPc = reinterpret_cast<uint64_t*>(fp[1]);

        if (nextFp == nullptr) return KotlinFrameAnchor(nullptr, nextPc);

        if (reinterpret_cast<uintptr_t>(nextFp) % 16 != 0) return std::nullopt;
        if (nextFp <= fp) return std::nullopt;
        if (stackHigh != nullptr && nextFp >= stackHigh) return std::nullopt;

        return KotlinFrameAnchor(nextFp, nextPc);
    }
};

// Captures the immediate caller's {fp, pc} by reading this function's own
// frame record (ALWAYS_INLINE so __builtin_frame_address(0) resolves to
// the caller's frame once inlined, not a separate frame of its own).
ALWAYS_INLINE inline KotlinFrameAnchor captureCallerFrameAnchor() {
    uint64_t* fp = reinterpret_cast<uint64_t*>(__builtin_frame_address(0));
    return KotlinFrameAnchor{(uint64_t*) fp[0], (uint64_t*) fp[1]};
}
#endif

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
        suspensionData_(ThreadState::kNative, *this) {
#if defined(__aarch64__)
        captureStackBounds();
#endif
    }

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

#if defined(__aarch64__)
    void pushStackMapAnchor(const KotlinFrameAnchor& anchor) noexcept {
        RuntimeLogInfo({logging::Tag::kLogging}, "Pushing new anchor: fp=%p pc=%p", anchor.fp, anchor.pc);
        frameAnchors_.emplace_back(anchor);
    }

    void popStackMapAnchor() noexcept {
        RuntimeLogInfo({logging::Tag::kLogging}, "Poping last anchor: fp=%p pc=%p", frameAnchors_.back().fp, frameAnchors_.back().pc);
        frameAnchors_.pop_back();
    }

    const std::vector<KotlinFrameAnchor>& frameAnchors() {
        return frameAnchors_;
    }

    // Bounds of this thread's stack: [stackLow, stackHigh). Null if they are unknown.
    // Taken on the owning thread in the constructor, read by the GC thread.
    uint64_t* stackLow() const noexcept { return stackLow_; }
    uint64_t* stackHigh() const noexcept { return stackHigh_; }

    
    bool cameFromNative() {
        return cameFromNative_;
    }

    void setCameFromNative(bool val) {
        cameFromNative_ = val;
    }
#endif

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
#if defined(__aarch64__)
    // Must run on the thread that owns this `ThreadData`.
    void captureStackBounds() noexcept {
        pthread_t self = pthread_self();
#if defined(__APPLE__)
        // `pthread_get_stackaddr_np` returns the highest address of the stack.
        auto high = reinterpret_cast<uintptr_t>(pthread_get_stackaddr_np(self));
        size_t size = pthread_get_stacksize_np(self);
        stackHigh_ = reinterpret_cast<uint64_t*>(high);
        stackLow_ = reinterpret_cast<uint64_t*>(high - size);
#else
        pthread_attr_t attr;
        if (pthread_getattr_np(self, &attr) == 0) {
            void* low = nullptr;
            size_t size = 0;
            if (pthread_attr_getstack(&attr, &low, &size) == 0) {
                stackLow_ = reinterpret_cast<uint64_t*>(low);
                stackHigh_ = reinterpret_cast<uint64_t*>(reinterpret_cast<uintptr_t>(low) + size);
            }
            pthread_attr_destroy(&attr);
        }
#endif
    }

    uint64_t* stackLow_ = nullptr;
    uint64_t* stackHigh_ = nullptr;
    std::vector<KotlinFrameAnchor> frameAnchors_;
    // True only while the thread runs the safepoint of a Native -> Runnable.
    bool cameFromNative_ = false;
#endif
};

} // namespace mm
} // namespace kotlin

#endif // RUNTIME_MM_THREAD_DATA_H
