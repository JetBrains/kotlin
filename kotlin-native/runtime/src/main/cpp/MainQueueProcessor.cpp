/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "MainQueueProcessor.hpp"

#include <atomic>

#include "KAssert.h"

#if KONAN_SUPPORTS_GRAND_CENTRAL_DISPATCH
#include <dispatch/dispatch.h>
#endif

using namespace kotlin;

namespace {

#if KONAN_SUPPORTS_GRAND_CENTRAL_DISPATCH
std::atomic<bool> isMainQueueProcessed = false;
#endif

} // namespace

void kotlin::initializeMainQueueProcessor() noexcept {
#if KONAN_SUPPORTS_GRAND_CENTRAL_DISPATCH
    dispatch_async_f(
            dispatch_get_main_queue(), nullptr, [](void*) { isMainQueueProcessed.store(true, std::memory_order_relaxed); });
#endif
}

bool kotlin::isMainQueueProcessorAvailable() noexcept {
#if KONAN_SUPPORTS_GRAND_CENTRAL_DISPATCH
    return isMainQueueProcessed.load(std::memory_order_relaxed);
#else
    return false;
#endif
}

void kotlin::runOnMainQueue(void* arg, void (*f)(void*)) noexcept {
    RuntimeAssert(isMainQueueProcessorAvailable(), "Running on main queue when it's not processed");
#if KONAN_SUPPORTS_GRAND_CENTRAL_DISPATCH
    dispatch_async_f(dispatch_get_main_queue(), arg, f);
#endif
}
