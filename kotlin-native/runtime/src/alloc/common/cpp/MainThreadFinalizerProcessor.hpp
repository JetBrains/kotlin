/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <atomic>

#include "RunLoopFinalizerProcessor.hpp"
#include "Utils.hpp"

namespace kotlin::alloc {

// Finalizer processor that runs on the main thread.
//
// Only enabled if `compiler::objcDisposeOnMain()` is true and if the main run loop is processed.
//
// It just wraps `RunLoopFinalizerProcessor`, see it for implementation details.
template <typename FinalizerQueue, typename FinalizerQueueTraits>
class MainThreadFinalizerProcessor : private Pinned {
public:
    MainThreadFinalizerProcessor() noexcept {
#if KONAN_HAS_FOUNDATION_FRAMEWORK
        if (compiler::objcDisposeOnMain()) {
            CFRunLoopPerformBlock(CFRunLoopGetMain(), kCFRunLoopDefaultMode, ^{
                [[clang::no_destroy]] static auto subscription = processor_.attachToCurrentRunLoop();
                available_.store(true, std::memory_order_release);
            });
        }
#endif
    }

    bool available() const noexcept {
#if KONAN_HAS_FOUNDATION_FRAMEWORK
        return available_.load(std::memory_order_acquire);
#else
        return false;
#endif
    }

    void schedule(FinalizerQueue tasks, uint64_t epoch) noexcept {
        if (FinalizerQueueTraits::isEmpty(tasks)) return;
#if KONAN_HAS_FOUNDATION_FRAMEWORK
        RuntimeAssert(available(), "MainThreadFinalizerProcessor is unavailable");
        processor_.schedule(std::move(tasks), epoch);
#else
        RuntimeAssert(false, "MainThreadFinalizerProcessor is unavailable");
#endif
    }

    template <typename F>
    std::invoke_result_t<F, RunLoopFinalizerProcessorConfig&> withConfig(F&& f) noexcept {
#if KONAN_HAS_FOUNDATION_FRAMEWORK
        return processor_.withConfig(std::forward<F>(f));
#else
        RuntimeAssert(false, "MainThreadFinalizerProcessor is unavailable");
#endif
    }

private:
#if KONAN_HAS_FOUNDATION_FRAMEWORK
    std::atomic<bool> available_ = false;
    RunLoopFinalizerProcessor<FinalizerQueue, FinalizerQueueTraits> processor_;
#endif
};

} // namespace kotlin::alloc