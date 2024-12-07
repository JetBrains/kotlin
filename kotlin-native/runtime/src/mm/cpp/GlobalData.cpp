/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "GlobalData.hpp"

#include <condition_variable>
#include <mutex>

#include "CompilerConstants.hpp"
#include "Porting.h"

#if KONAN_WINDOWS
#include "concurrent/Mutex.hpp"
#endif

using namespace kotlin;

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

std::atomic<InitState> globalDataInitState = InitState::kUninitialized;
std::atomic<std::thread::id> globalDataInitializingThread{};
ManuallyScoped<mm::GlobalData> globalDataInstance{};

#if KONAN_WINDOWS
// On winpthreads, there's a weird bug if this is a regular `std::mutex`:
// even though `constructGlobalDataInstance()` has already started (and so,
// has already successfully tried locking this mutex), `waitGlobalDataInitialized`
// may crash trying to lock it too.
SpinLock globalDataInitMutex;
std::condition_variable_any globalDataInitCV;
#else
std::mutex globalDataInitMutex;
std::condition_variable globalDataInitCV;
#endif

void constructGlobalDataInstance() noexcept {
    std::unique_lock guard{globalDataInitMutex};
    auto initialState = InitState::kUninitialized;
    globalDataInitState.compare_exchange_strong(initialState, InitState::kInitializing, std::memory_order_acq_rel);
    RuntimeAssert(initialState != InitState::kInitializing, "Expected state %s, but was %s", initStateToString(InitState::kUninitialized), initStateToString(initialState));
    globalDataInitializingThread.store(std::this_thread::get_id(), std::memory_order_relaxed);

    globalDataInstance.construct();

    auto initializingState = InitState::kInitializing;
    globalDataInitState.compare_exchange_strong(initializingState, InitState::kInitialized, std::memory_order_acq_rel);
    RuntimeAssert(initializingState == InitState::kInitializing, "Expected state %s, but was %s", initStateToString(InitState::kInitializing), initStateToString(initializingState));
    guard.unlock();
    globalDataInitCV.notify_all();
}

[[maybe_unused]] struct GlobalDataEagerInit {
    GlobalDataEagerInit() noexcept {
        if (!compiler::globalDataLazyInit()) {
            constructGlobalDataInstance();
        }
    }
} globalDataEagerInit;

}

// static
mm::GlobalData& mm::GlobalData::Instance() noexcept {
    if (compiler::runtimeAssertsEnabled()) {
        auto s = globalDataInitState.load(std::memory_order_relaxed);
        auto initializingThread = globalDataInitializingThread.load(std::memory_order_relaxed);
        RuntimeAssert(s == InitState::kInitialized || initializingThread == std::this_thread::get_id(), "Expected state %s, but was %s.", initStateToString(InitState::kInitialized), initStateToString(s));
    }
    return *globalDataInstance;
}

// static
void mm::GlobalData::init() noexcept {
    if (compiler::globalDataLazyInit()) {
        constructGlobalDataInstance();
    }
}

void mm::waitGlobalDataInitialized() noexcept {
    if (globalDataInitState.load(std::memory_order_acquire) == InitState::kInitialized) {
        return;
    }
    if (compiler::runtimeAssertsEnabled()) {
        auto initializingThread = globalDataInitializingThread.load(std::memory_order_relaxed);
        RuntimeAssert(initializingThread != std::this_thread::get_id(), "A thread that initialized global data cannot be waiting for its initialization");
    }
    std::unique_lock guard{globalDataInitMutex};
    globalDataInitCV.wait(guard, []() noexcept {
            return globalDataInitState.load(std::memory_order_relaxed) == InitState::kInitialized;
            });
}

mm::GlobalData::GlobalData() noexcept = default;

// static
