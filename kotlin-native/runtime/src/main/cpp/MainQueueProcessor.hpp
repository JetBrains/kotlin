/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

namespace kotlin {

void initializeMainQueueProcessor() noexcept;

bool isMainQueueProcessorAvailable() noexcept;

// Run `f(arg)` on main queue without waiting for its completion.
// Only valid if `isMainQueueProcessorAvailable()` returns true.
void runOnMainQueue(void* arg, void (*f)(void*)) noexcept;

} // namespace kotlin
