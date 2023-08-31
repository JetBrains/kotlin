/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <atomic>
#include <optional>
#include <shared_mutex>

#include "Memory.h"
#include "Utils.hpp"
#include "GCStatistics.hpp"
#include "SafePoint.hpp"

namespace kotlin::gc::barriers {

namespace internal {

class BarriersControl {
public:
    GCHandle gcHandle() const { return gcHandle_; }
    bool markNewObjects() const { return markNewObjects_; }
    bool concurrentMarkBarriers() const { return concurrentMarkBarriers_; }
    bool weakProcessingBarriers() const { return weakProcessingBarriers_; }

    void checkInvariants() noexcept;

protected:
    GCHandle gcHandle_ = GCHandle::invalid();
    bool markNewObjects_ = false;
    bool concurrentMarkBarriers_ = false;
    bool weakProcessingBarriers_ = false;
};


/**
 * Defines correct state transitions for different barrier control flag combinations.
 * See also `BarriersControl::checkInvariants()`.
 *
 * A single controller thread requests state transitions on this proto structure
 * and then waits for mutator threads to actualize their local copies of barrier control flags.
 */
class BarriersControlProto : public BarriersControl, private Pinned {
public:
    BarriersControl getValues() const noexcept;

    void beginMarkingEpoch(GCHandle gcHandle) noexcept;
    void endMarkingEpoch() noexcept;

    void enableConcurrentMarkBarriers() noexcept;
    void disableConcurrentMarkBarriers() noexcept;

    void enableWeakProcessingBarriers() noexcept;
    void disableWeakProcessingBarriers() noexcept;

private:
    mutable std::shared_mutex mutex_;
};

} // namespace internal

class BarriersThreadData : public internal::BarriersControl , private Pinned {
public:
    /** GC will use this helper to ensure that thread local state will be actualized exactly once and synchronized. */
    class ActualizeAction : public mm::OncePerThreadAction<ActualizeAction> {
    public:
        static OncePerThreadAction::ThreadData& getUtilityData(mm::ThreadData& threadData);
        static void action(mm::ThreadData& threadData) noexcept;
    };

    explicit BarriersThreadData(mm::ThreadData& threadData);

    void actualizeFrom(internal::BarriersControl proto) noexcept;

    void onThreadRegistration() noexcept;
    void onSafePoint() noexcept;

    void onAllocation(ObjHeader* allocated) noexcept;
    void beforeHeapRefUpdate(mm::DirectRefAccessor ref, ObjHeader* value) noexcept; // TODO add dst object ref
    OBJ_GETTER(weakRefReadBarrier, ObjHeader* weakReferee) noexcept;

private:
    void beforeHeapRefUpdateSlowPath(mm::DirectRefAccessor ref, ObjHeader* value) noexcept;
    OBJ_GETTER(weakRefReadBarrierSlowPath, ObjHeader* weakReferee) noexcept;

    GCHandle::GCMarkScope markHandle_{GCHandle::invalid()};

    mm::ThreadData& base_;
    ActualizeAction::ThreadData actualizeActionData_;
};

void beginMarkingEpoch(GCHandle gcHandle) noexcept;
void endMarkingEpoch() noexcept;

// Eventually will be used by concurrent mark
[[maybe_unused]] void enableMarkBarriers() noexcept;
[[maybe_unused]] void disableMarkBarriers() noexcept;

void enableWeakRefBarriers() noexcept;
void disableWeakRefBarriers() noexcept;

} // namespace kotlin::gc::barriers
