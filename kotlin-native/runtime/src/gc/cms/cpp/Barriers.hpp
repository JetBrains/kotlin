/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <atomic>
#include <optional>

#include "Utils.hpp"
#include "GCStatistics.hpp"
#include "ReferenceOps.hpp"

namespace kotlin::gc::barriers {

class BarriersThreadData : private Pinned {
public:
    void onThreadRegistration() noexcept;
    void onSafePoint() noexcept;
    
    void startMarkingNewObjects(GCHandle gcHandle) noexcept;
    void stopMarkingNewObjects() noexcept;
    bool shouldMarkNewObjects() const noexcept;

    void onAllocation(ObjHeader* allocated);
private:
    std::optional<GCHandle::GCMarkScope> markHandle_{};
};

// Must be called during STW.
void enableMarkBarriers(int64_t epoch) noexcept;
void disableMarkBarriers() noexcept;

void beforeHeapRefUpdate(mm::DirectRefAccessor ref, ObjHeader* value) noexcept;

// TODO re-enable again
// Must be called during STW.
//void enableWeakRefBarriers(int64_t epoch) noexcept;
//void disableWeakRefBarriers() noexcept;
//
//OBJ_GETTER(WeakRefRead, ObjHeader* weakReferee) noexcept;


} // namespace kotlin::gc
