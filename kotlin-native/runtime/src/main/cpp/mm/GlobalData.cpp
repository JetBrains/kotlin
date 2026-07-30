/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "mm/GlobalData.hpp"

#include <atomic>
#include <condition_variable>
#include <mutex>
#include <thread>

#include "CompilerConstants.hpp"
#include "ManuallyScoped.hpp"

#if KONAN_WINDOWS
#include "concurrent/Mutex.hpp"
#endif

using namespace kotlin;

// TODO(KT-71143): try to simplify when the initialization code lives in a single place.
//
// `GlobalData` lifetime:
// * created by the first thread entering K/N runtime (decided in Runtime.cpp) in the call to
//   `GlobalData::init()`;
// * all other threads call `waitGlobalDataInitialized()` during their first calls into K/N runtime;
// * every access into `GlobalData` happens via `GlobalData::Instance()`:
//   * the first thread is allowed that only during (or after the) call to `GlobalData::init()`
//   * all other threads only after `waitGlobalDataInitialized()`
// * `GlobalData` is never destroyed.

namespace {

enum class InitState {
    kUninitialized,
    kInitializing,
    kInitialized,
};

const char* initStateToString(InitState state) noexcept {
    switch (state) {
        case InitState::kUninitialized: return "uninitialized";
        case InitState::kInitializing: return "initializing";
        case InitState::kInitialized: return "initialized";
    }
}

std::atomic<InitState> globalDataInitState = InitState::kUninitialized; // can be read outside of mutex
std::atomic<std::thread::id> globalDataInitializingThread{}; // can be read outside of mutex
ManuallyScoped<mm::GlobalData> globalDataInstance{};

#if KONAN_WINDOWS
// On winpthreads, there's a weird bug if this is a regular `std::mutex`:
// even though `GlobalData::init()` has already started (and so,
// has already successfully tried locking this mutex), `waitGlobalDataInitialized`
// may crash trying to lock it too.
SpinLock globalDataInitMutex;
// `no_destroy` is required because winpthreads' `pthread_cond_destroy` deadlocks during `DLL_PROCESS_DETACH`
// when worker threads are terminated by the Windows loader while still holding winpthreads internal locks. See KT-85897.
[[clang::no_destroy]] std::condition_variable_any globalDataInitCV;
#else
[[clang::no_destroy]] std::mutex globalDataInitMutex;
[[clang::no_destroy]] std::condition_variable globalDataInitCV;
#endif

}

// static
mm::GlobalData& mm::GlobalData::Instance() noexcept {
    if (compiler::runtimeAssertsEnabled()) {
        // Synchronizes with release in `GlobalData::init`.
        // NOTE: this acquire changes behaviour: if a thread didn't call `waitGlobalDataInitialized`, but the
        //       `GlobalData` instance was created in parallel, then
        //       * with assertions enabled, this acquire creates happens-before for stuff inside `globalDataInstance` below
        //         and something like TSAN won't help here.
        //       * with assertions disabled, the happens-before wouldn't be created and accessing `globalDataInstance` is illegal
        switch (globalDataInitState.load(std::memory_order_acquire)) {
            case InitState::kUninitialized:
                RuntimeAssert(false, "The thread must have called GlobalData::init or waitGlobalDataInitialized first");
                break;
            case InitState::kInitializing:
                // This relaxed read is fine, because it's written into before the release on `globalDataInitState`.
                RuntimeAssert(
                        globalDataInitializingThread.load(std::memory_order_relaxed) == std::this_thread::get_id(),
                        "Only the initializing thread is allowed to access Instance in the initializing state");
                break;
            case InitState::kInitialized:
                break;
        }
    }
    return *globalDataInstance;
}

// static
void mm::GlobalData::init() noexcept {
    std::unique_lock guard{globalDataInitMutex};
    if (compiler::runtimeAssertsEnabled()) {
        auto s = globalDataInitState.load(std::memory_order_relaxed);
        RuntimeAssert(s == InitState::kUninitialized, "GlobalData is expected to be uninitialized, but is %s", initStateToString(s));
    }
    globalDataInitializingThread.store(std::this_thread::get_id(), std::memory_order_relaxed); // must be before the release below
    // Synchronizes with acquire in `GlobalData::Instance` (assert only)
    globalDataInitState.store(InitState::kInitializing, std::memory_order_release);

    globalDataInstance.construct();

    // Synchronizes with acquire in `waitGlobalDataInitialized` and `GlobalData::Instance` (assert only)
    globalDataInitState.store(InitState::kInitialized, std::memory_order_release);
    guard.unlock();
    globalDataInitCV.notify_all();
}

void mm::waitGlobalDataInitialized() noexcept {
    // Synchronizes with release in `GlobalData::init`.
    if (globalDataInitState.load(std::memory_order_acquire) == InitState::kInitialized) {
        return;
    }
    // We want this assert before taking the lock to catch a potential deadlock when `globalDataInitializingThread ==
    // std::this_thread::get_id`. Relaxed read is fine for this condition: to be triggered, the write must have happened on our thread
    // (other threads can't write our `get_id()`)
    RuntimeAssert(
            globalDataInitializingThread.load(std::memory_order_relaxed) != std::this_thread::get_id(),
            "A thread that initialized global data cannot be waiting for its initialization");
    std::unique_lock guard{globalDataInitMutex};
    globalDataInitCV.wait(guard, []() noexcept { return globalDataInitState.load(std::memory_order_relaxed) == InitState::kInitialized; });
}

mm::GlobalData::GlobalData() noexcept = default;
