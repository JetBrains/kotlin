/*
 * Copyright 2010-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "Memory.h"
#include "ScopedExternalRCRef.hpp"
#include "Utils.hpp"

namespace kotlin::objc {

// Convenience wrapper around ScopedExternalRCRef, that
// preserves separate obj_ field optimization.
class RefHolder : private Pinned {
public:
    void init(KRef ref) noexcept {
        AssertThreadState(kotlin::ThreadState::kRunnable);
        obj_ = ref;
        ref_ = mm::ScopedExternalRCRef(ref);
    }

    KRef ref() const noexcept {
        AssertThreadState(kotlin::ThreadState::kRunnable);
        return obj_;
    }

private:
    KRef obj_; // TODO: This is a tradeoff: one less indirection, but an extra pointer field.
    mm::ScopedExternalRCRef ref_;
};

}
