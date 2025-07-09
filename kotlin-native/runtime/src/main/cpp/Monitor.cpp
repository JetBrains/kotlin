/*
 * Copyright 2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include <mutex>
#include <condition_variable>
#include <thread>

#include "CallsChecker.hpp"
#include "Exceptions.h"
#include "Utils.hpp"
#include "Memory.h"
#include "Types.h"

using namespace kotlin;

namespace {

class MonitorImpl : Pinned {
public:
    void enter() {
        ThreadStateGuard guard(ThreadState::kNative, false);
        mutex_.lock();
    }

    void leave() {
        mutex_.unlock();
    }

    void wait(KLong timeoutMillis) {
        ThreadStateGuard guard(ThreadState::kNative, false);
        std::unique_lock lock{mutex_, std::adopt_lock};
        if (timeoutMillis == 0) {
            conditionVariable_.wait(lock);
        } else {
            conditionVariable_.wait_for(lock, std::chrono::milliseconds{timeoutMillis});
        }
        lock.release();
    }

    void notify() noexcept {
        conditionVariable_.notify_one();
    }

    void notifyAll() noexcept {
        conditionVariable_.notify_all();
    }

private:
    std::mutex mutex_{};
    std::condition_variable conditionVariable_{};
};

} // namespace

extern "C" {

void Kotlin_Monitor_enter(MonitorImpl* monitor) {
    wrappingCppExceptions([&] {
        monitor->enter();
    });
}

void Kotlin_Monitor_leave(MonitorImpl* monitor) {
    wrappingCppExceptions([&] {
        monitor->leave();
    });
}

void Kotlin_Monitor_wait(MonitorImpl* monitor, KLong timeoutMillis) {
    wrappingCppExceptions([&] {
        monitor->wait(timeoutMillis);
    });
}

void Kotlin_Monitor_notify(MonitorImpl* monitor) noexcept {
    monitor->notify();
}

void Kotlin_Monitor_notifyAll(MonitorImpl* monitor) noexcept {
    monitor->notifyAll();
}

MonitorImpl* Kotlin_Monitor_allocate(ObjHeader*) {
    CallsCheckerIgnoreGuard recursiveGuard;
    return new MonitorImpl();
}

void Kotlin_Monitor_destroy(MonitorImpl* monitor) noexcept {
    CallsCheckerIgnoreGuard recursiveGuard;
    delete monitor;
}

}
