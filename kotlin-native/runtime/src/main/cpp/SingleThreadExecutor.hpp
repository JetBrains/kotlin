/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <condition_variable>
#include <functional>
#include <future>
#include <mutex>
#include <shared_mutex>

#include "ScopedThread.hpp"
#include "Types.h"
#include "Utils.hpp"

namespace kotlin {

// TODO: Try to generalize enough, so that FinalizerProcessor is implementable in terms of it.
//       Requirements: avoid heap allocations as much as possible.
// TODO: Try to generalize so enough, that Worker.cpp can be written on top of this.
//       Requirements: delayed tasks.
// TODO: Makes sense to specialize Context to void when context is unneeded.

// Execute tasks on a single worker thread. `Context` is created and destroyed on the worker thread.
template <typename Context>
class SingleThreadExecutor : private Pinned {
public:
    // Starts the worker thread immediately.
    template <typename ContextFactory>
    explicit SingleThreadExecutor(ContextFactory&& contextFactory) noexcept :
        thread_(&SingleThreadExecutor::runLoop<ContextFactory>, this, std::forward<ContextFactory>(contextFactory)) {}

    // Starts the worker thread immediately.
    SingleThreadExecutor() noexcept : SingleThreadExecutor([] { return Context(); }) {}

    ~SingleThreadExecutor() {
        {
            std::unique_lock guard(workMutex_);
            // Note: This can only happen in destructor, because otherwise `context_` will be a dangling
            // pointer to the destroyed thread's stack.
            shutdownRequested_ = true;
        }
        workCV_.notify_one();
        thread_.join();
    }

    // May lock until the context is created by the worker thread.
    Context& context() const noexcept {
        std::shared_lock guard(contextMutex_);
        contextCV_.wait(guard, [this] { return context_ != nullptr; });
        return *context_;
    }

    // Id of the worker thread.
    ScopedThread::id threadId() const noexcept { return thread_.get_id(); }

    // Schedule task execution on the worker thread. The returned future is resolved when the task has completed.
    // If `this` is destroyed before the task manages to complete, the returned future will fail with exception upon `.get()`.
    // If the task moves the runtime into a runnable state, it should move it back into the native state.
    template <typename Task>
    [[nodiscard]] std::future<void> execute(Task&& f) noexcept {
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
    template <typename ContextFactory>
    void runLoop(ContextFactory&& contextFactory) noexcept {
        auto context = contextFactory();
        {
            std::unique_lock guard(contextMutex_);
            context_ = &context;
        }
        contextCV_.notify_all();
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

    mutable std::condition_variable_any contextCV_;
    mutable std::shared_mutex contextMutex_;
    Context* context_ = nullptr;

    std::condition_variable workCV_;
    std::mutex workMutex_;
    KStdDeque<std::packaged_task<void()>> queue_;
    bool shutdownRequested_ = false;

    ScopedThread thread_;
};

} // namespace kotlin
