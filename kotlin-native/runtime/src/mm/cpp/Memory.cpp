/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "Memory.h"

#include "ThreadData.hpp"
#include "ThreadRegistry.hpp"
#include "Utils.hpp"

using namespace kotlin;

// Delete all means of creating this type directly as it only serves
// as a typedef for `mm::ThreadRegistry::Node`.
extern "C" struct MemoryState : Pinned {
    MemoryState() = delete;
    ~MemoryState() = delete;
};

namespace {

// `reinterpret_cast` to it and back to the same type
// will yield precisely the same pointer, so it's safe.
ALWAYS_INLINE MemoryState* ToMemoryState(mm::ThreadRegistry::Node* data) {
    return reinterpret_cast<MemoryState*>(data);
}

ALWAYS_INLINE mm::ThreadRegistry::Node* FromMemoryState(MemoryState* state) {
    return reinterpret_cast<mm::ThreadRegistry::Node*>(state);
}

} // namespace

extern "C" MemoryState* InitMemory(bool firstRuntime) {
    return ToMemoryState(mm::ThreadRegistry::Instance().RegisterCurrentThread());
}

extern "C" void DeinitMemory(MemoryState* state, bool destroyRuntime) {
    mm::ThreadRegistry::Instance().Unregister(FromMemoryState(state));
}
