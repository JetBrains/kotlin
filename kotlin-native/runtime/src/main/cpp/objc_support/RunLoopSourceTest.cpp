/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#if KONAN_HAS_FOUNDATION_FRAMEWORK

#include "RunLoopSource.hpp"

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "RunLoopTestSupport.hpp"
#include "ScopedThread.hpp"

using namespace kotlin;

namespace {

class Checkpoint : private Pinned {
public:
    Checkpoint() noexcept = default;

    void checkpoint() noexcept { value_.fetch_add(1, std::memory_order_release); }

    template <typename F>
    int64_t performAndWaitChange(F&& f) noexcept {
        auto initial = value_.load(std::memory_order_acquire);
        std::invoke(std::forward<F>(f));
        while (value_.load(std::memory_order_relaxed) <= initial) {
            std::this_thread::yield();
        }
        auto final = value_.load(std::memory_order_acquire);
        return final - initial;
    }

private:
    std::atomic<int64_t> value_ = 0;
};

class MainThreadShutdownToken : private Pinned {
public:
    MainThreadShutdownToken() noexcept = default;

    ~MainThreadShutdownToken() { CFRunLoopStop(CFRunLoopGetMain()); }
};

} // namespace

TEST(RunLoopSourceTest, MainThread) {
    ASSERT_EQ(CFRunLoopGetMain(), CFRunLoopGetCurrent());
    Checkpoint checkpoint;
    objc_support::RunLoopSource source([&]() noexcept { checkpoint.checkpoint(); });
    auto subscription = source.attachToCurrentRunLoop();
    auto thread = ScopedThread([&]() noexcept {
        MainThreadShutdownToken token;
        ASSERT_THAT(
                checkpoint.performAndWaitChange([&]() noexcept {
                    source.signal();
                    CFRunLoopWakeUp(CFRunLoopGetMain());
                }),
                1);
        ASSERT_THAT(
                checkpoint.performAndWaitChange([&]() noexcept {
                    source.signal();
                    CFRunLoopWakeUp(CFRunLoopGetMain());
                }),
                1);
    });
    CFRunLoopRun();
}

TEST(RunLoopSourceTest, SignalRunsOnce) {
    Checkpoint checkpoint;
    objc_support::RunLoopSource source([&]() noexcept { checkpoint.checkpoint(); });
    objc_support::test_support::RunLoopInScopedThread runLoop([&]() noexcept { return source.attachToCurrentRunLoop(); });
    ASSERT_THAT(
            checkpoint.performAndWaitChange([&]() noexcept {
                source.signal();
                runLoop.wakeUp();
            }),
            1);
    ASSERT_THAT(
            checkpoint.performAndWaitChange([&]() noexcept {
                source.signal();
                runLoop.wakeUp();
            }),
            1);
}

TEST(RunLoopSourceTest, ConnectToDifferentLoop) {
    Checkpoint checkpoint;
    objc_support::RunLoopSource source([&]() noexcept { checkpoint.checkpoint(); });
    {
        objc_support::test_support::RunLoopInScopedThread runLoop([&]() noexcept { return source.attachToCurrentRunLoop(); });
        ASSERT_THAT(
                checkpoint.performAndWaitChange([&]() noexcept {
                    source.signal();
                    runLoop.wakeUp();
                }),
                1);
    }
    {
        objc_support::test_support::RunLoopInScopedThread runLoop([&]() noexcept { return source.attachToCurrentRunLoop(); });
        ASSERT_THAT(
                checkpoint.performAndWaitChange([&]() noexcept {
                    source.signal();
                    runLoop.wakeUp();
                }),
                1);
    }
}

TEST(RunLoopSourceTest, Reconnect) {
    Checkpoint checkpoint1;
    Checkpoint checkpoint2;
    objc_support::RunLoopSource source([&]() noexcept { checkpoint1.checkpoint(); });
    objc_support::test_support::RunLoopInScopedThread runLoop([&]() noexcept { return 0; });

    std::unique_ptr<objc_support::RunLoopSource::Subscription> subscription;

    // Subscribe. Check that `source` is attached. Unsubscribe
    checkpoint2.performAndWaitChange([&]() noexcept {
        runLoop.schedule([&]() noexcept {
            subscription = source.attachToCurrentRunLoop();
            checkpoint2.checkpoint();
        });
    });
    checkpoint1.performAndWaitChange([&]() noexcept {
        source.signal();
        runLoop.wakeUp();
    });
    checkpoint2.performAndWaitChange([&]() noexcept {
        runLoop.schedule([&]() noexcept {
            subscription = nullptr;
            checkpoint2.checkpoint();
        });
    });

    // Repeat the procedure.
    checkpoint2.performAndWaitChange([&]() noexcept {
        runLoop.schedule([&]() noexcept {
            subscription = source.attachToCurrentRunLoop();
            checkpoint2.checkpoint();
        });
    });
    checkpoint1.performAndWaitChange([&]() noexcept {
        source.signal();
        runLoop.wakeUp();
    });
    checkpoint2.performAndWaitChange([&]() noexcept {
        runLoop.schedule([&]() noexcept {
            subscription = nullptr;
            checkpoint2.checkpoint();
        });
    });
}

#endif