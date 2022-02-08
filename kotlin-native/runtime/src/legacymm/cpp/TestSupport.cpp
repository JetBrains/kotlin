/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "TestSupport.hpp"

extern "C" void Kotlin_TestSupport_AssertClearGlobalState() {
    // Nothing to do. Supported for the new MM only.
}

void kotlin::DeinitMemoryForTests(MemoryState* memoryState) {
    DeinitMemory(memoryState, false);
}