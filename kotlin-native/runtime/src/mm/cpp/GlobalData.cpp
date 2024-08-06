/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "GlobalData.hpp"

#include <condition_variable>
#include <mutex>

#include "CompilerConstants.hpp"
#include "Porting.h"

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

std::mutex globalDataInitMutex;
std::condition_variable globalDataInitCV;

void constructGlobalDataInstance() noexcept {
    std::unique_lock guard{globalDataInitMutex};
    auto initialState = InitState::kUninitialized;
    globalDataInitState.compare_exchange_strong(initialState, InitState::kInitializing, std::memory_order_acq_rel);
    RuntimeAssert(initialState != InitState::kInitializing, "Expected state %s, but was %s", initStateToString(InitState::kUninitialized), initStateToString(initialState));
    if (initialState == InitState::kInitialized) return;
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

// static
void mm::GlobalData::waitInitialized() noexcept {
        if (globalDataInitState.load(std::memory_order_acquire) == InitState::kInitialized) {
            return;
        }
        auto initializingThread = globalDataInitializingThread.load(std::memory_order_relaxed);
        if (initializingThread == std::this_thread::get_id()) {
            return;
        }
        std::unique_lock guard{globalDataInitMutex};
        globalDataInitCV.wait(guard, []() noexcept {
            return globalDataInitState.load(std::memory_order_relaxed) == InitState::kInitialized;
        });
}

mm::GlobalData::GlobalData() noexcept = default;

// static
