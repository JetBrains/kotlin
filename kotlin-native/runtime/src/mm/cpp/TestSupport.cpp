/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "MemoryPrivate.hpp"
#include "TestSupport.hpp"
#include "ThreadRegistry.hpp"

MemoryState* kotlin::InitMemoryForTests() {
    auto threadDataNode = mm::ThreadRegistry::Instance().RegisterCurrentThread();
    return mm::ToMemoryState(threadDataNode);
}

void kotlin::DeinitMemoryForTests(MemoryState* state) {
    auto threadDataNode = mm::FromMemoryState(state);
    mm::ThreadRegistry::Instance().Unregister(threadDataNode);
    // Nullify current thread data. The thread is still alive, so this is safe.
    mm::ThreadRegistry::TestSupport::SetCurrentThreadData(nullptr);
}