/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <atomic>
#include <optional>

#include "Memory.h"
#include "Utils.hpp"

namespace kotlin::gc {

class BarriersThreadData : private Pinned {
public:
    void onCheckpoint() noexcept;
    void resetCheckpoint() noexcept;
    bool visitedCheckpoint() const noexcept;

private:
    std::atomic<bool> visitedCheckpoint_ = false;
};

// Must be called during STW.
void EnableWeakRefBarriers() noexcept;

// Must be called outside STW.
void DisableWeakRefBarriers() noexcept;

OBJ_GETTER(WeakRefRead, std::atomic<ObjHeader*>& weakReferee) noexcept;

} // namespace kotlin::gc
