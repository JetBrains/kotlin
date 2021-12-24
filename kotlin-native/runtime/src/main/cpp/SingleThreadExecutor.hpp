/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <condition_variable>
#include <functional>
#include <future>
#include <mutex>
#include <thread>

#include "Types.h"
#include "Utils.hpp"

namespace kotlin {

// TODO: Try to generalize enough, so that FinalizerProcessor is implementable in terms of it.
//       Requirements: avoid heap allocations as much as possible.
// TODO: Try to generalize so enough, that Worker.cpp can be written on top of this.
//       Requirements: delayed tasks.

// Thread that has a context attached to it. Context is created and destroyed on the thread.
// Thread cannot join before the destructor, because otherwise `context()` will dangle.
template <typename Context>
class ThreadWithContext : private Pinned {
public:
    ThreadWithContext() = default;

    template <typename ContextFactory, typename Function, typename... Args>
    explicit ThreadWithContext(ContextFactory&& contextFactory, Function&& f, Args&&... args) :
        thread_(
                [this, contextFactory = std::forward<ContextFactory>(contextFactory), f = std::forward<Function>(f)](
                        Args&&... args) mutable {
                    auto context = contextFactory();
                    {
                        std::unique_lock guard(startMutex_);
                        context_ = &context;
                    }
                    startCV_.notify_one();
                    std::invoke(std::forward<Function>(f), std::forward<Args>(args)...);
                    std::unique_lock guard(stopMutex_);
                    stopCV_.wait(guard, [this] { return needsShutdown_; });
                },
                std::forward<Args>(args)...) {}

    ~ThreadWithContext() {
        {
            std::unique_lock guard(stopMutex_);
            needsShutdown_ = true;
        }
        stopCV_.notify_one();
        thread_.join();
    }

    // Wait until thread is fully initialized and `context()` is created.
    void waitInitialized() noexcept {
        std::unique_lock guard(startMutex_);
        startCV_.wait(guard, [this] { return context_ != nullptr; });
    }

    // May only be called after the thread has fully initialized. Use `WaitInitialized()` to be sure.
    Context& context() const noexcept {
        RuntimeAssert(context_ != nullptr, "context must be set");
        return *context_;
    }

    std::thread::id get_id() const noexcept { return thread_.get_id(); }

private:
    std::condition_variable startCV_;
    std::mutex startMutex_;
    Context* context_ = nullptr;

    // Need to keep thread alive for the entire lifetime of this object, because `context_` lifetime
    // is bound to the thread.
    std::condition_variable stopCV_;
    std::mutex stopMutex_;
    bool needsShutdown_ = false;

    std::thread thread_;
};

// TODO: Replace with `std::jthread`.
// A thread that always joins in the destructor
class JoiningThread : private MoveOnly {
public:
    JoiningThread() = default;

    template <typename Function, typename... Args>
    explicit JoiningThread(Function&& f, Args&&... args) : thread_(std::forward<Function>(f), std::forward<Args>(args)...) {}

    ~JoiningThread() { thread_.join(); }

    std::thread::id get_id() const noexcept { return thread_.get_id(); }

private:
    std::thread thread_;
};

// Execute tasks on a single worker thread.
// `Thread` must join in the destructor.
template <typename Thread>
class SingleThreadExecutor : private Pinned {
public:
    // Starts the worker thread immediately.
    template <typename ThreadFactory>
    explicit SingleThreadExecutor(ThreadFactory&& threadFactory) noexcept :
        thread_(std::forward<ThreadFactory>(threadFactory)(&SingleThreadExecutor::RunLoop, this)) {}

    SingleThreadExecutor() noexcept :
        SingleThreadExecutor([](auto&& function, auto&&... args) {
            return Thread(std::forward<decltype(function)>(function), std::forward<decltype(args)>(args)...);
        }) {}

    ~SingleThreadExecutor() {
        {
            std::unique_lock guard(workMutex_);
            // Note: This can only happen in destructor, because otherwise `context_` will be a dangling
            // pointer to the destroyed thread's stack.
            shutdownRequested_ = true;
        }
        workCV_.notify_one();
    }

    Thread& thread() noexcept { return thread_; }

    // Schedule task execution on the worker thread. The returned future is resolved when the task has completed.
    // If `this` is destroyed before the task manages to complete, the returned future will fail with exception upon `.get()`.
    // If the task moves the runtime into a runnable state, it should move it back into the native state.
    template <typename Task>
    [[nodiscard]] std::future<void> Execute(Task&& f) noexcept {
        std::packaged_task<void()> task(std::forward<Task>(f));
        auto future = task.get_future();
        {
            std::unique_lock guard(workMutex_);
            queue_.push_back(std::move(task));
        }
        workCV_.notify_one();
        return future;
    }

private:
    void RunLoop() noexcept {
        while (true) {
            std::packaged_task<void()> task;
            {
                std::unique_lock guard(workMutex_);
                workCV_.wait(guard, [this] { return !queue_.empty() || shutdownRequested_; });
                if (shutdownRequested_) {
                    return;
                }
                task = std::move(queue_.front());
                queue_.pop_front();
            }
            task();
        }
    }

    std::condition_variable workCV_;
    std::mutex workMutex_;
    KStdDeque<std::packaged_task<void()>> queue_;
    bool shutdownRequested_ = false;

    Thread thread_;
};

template <typename Context, typename ContextFactory>
SingleThreadExecutor<ThreadWithContext<Context>> MakeSingleThreadExecutorWithContext(ContextFactory&& contextFactory) noexcept {
    return SingleThreadExecutor<ThreadWithContext<Context>>(
            [contextFactory = std::forward<ContextFactory>(contextFactory)](auto&& function, auto&&... args) mutable {
                return ThreadWithContext<Context>(
                        std::forward<ContextFactory>(contextFactory), std::forward<decltype(function)>(function),
                        std::forward<decltype(args)>(args)...);
            });
}

template <typename Context>
SingleThreadExecutor<ThreadWithContext<Context>> MakeSingleThreadExecutorWithContext() noexcept {
    return MakeSingleThreadExecutorWithContext<Context>([] { return Context(); });
}

} // namespace kotlin
