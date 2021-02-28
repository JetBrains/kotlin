/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_MEMORYPRIVATE_HPP
#define RUNTIME_MEMORYPRIVATE_HPP

#include "Utils.hpp"
#include "ThreadData.hpp"
#include "ThreadRegistry.hpp"

using namespace kotlin;

namespace kotlin {
namespace mm {

// `reinterpret_cast` to it and back to the same type
// will yield precisely the same pointer, so it's safe.
ALWAYS_INLINE inline MemoryState* ToMemoryState(ThreadRegistry::Node* data) {
    return reinterpret_cast<MemoryState*>(data);
}

ALWAYS_INLINE inline ThreadRegistry::Node* FromMemoryState(MemoryState* state) {
    return reinterpret_cast<mm::ThreadRegistry::Node*>(state);
}

} // namespace mm
} // namepace kotlin

// Delete all means of creating this type directly as it only serves
// as a typedef for `mm::ThreadRegistry::Node`.
extern "C" struct MemoryState : kotlin::Pinned {
    MemoryState() = delete;
    ~MemoryState() = delete;

    ALWAYS_INLINE mm::ThreadData* GetThreadData() {
        return mm::FromMemoryState(this)->Get();
    }
};

#endif //RUNTIME_MEMORYPRIVATE_HPP
