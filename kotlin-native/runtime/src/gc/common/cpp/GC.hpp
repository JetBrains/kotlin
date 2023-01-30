/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "GCScheduler.hpp"
#include "Memory.h"
#include "Types.h"
#include "Utils.hpp"
#include "std_support/Memory.hpp"

namespace kotlin {

namespace mm {
class ThreadData;
}

namespace gc {

class GC : private Pinned {
public:
    class Impl;

    class ThreadData : private Pinned {
    public:
        class Impl;

        ThreadData(GC& gc, mm::ThreadData& threadData) noexcept;
        ~ThreadData();

        Impl& impl() noexcept { return *impl_; }

        void SafePointFunctionPrologue() noexcept;
        void SafePointLoopBody() noexcept;

        void Schedule() noexcept;
        void ScheduleAndWaitFullGC() noexcept;
        void ScheduleAndWaitFullGCWithFinalizers() noexcept;

        void Publish() noexcept;
        void ClearForTests() noexcept;

        ObjHeader* CreateObject(const TypeInfo* typeInfo) noexcept;
        ArrayHeader* CreateArray(const TypeInfo* typeInfo, uint32_t elements) noexcept;

        void OnStoppedForGC() noexcept;
        void OnSuspendForGC() noexcept;

    private:
        std_support::unique_ptr<Impl> impl_;
    };

    GC() noexcept;
    ~GC();

    Impl& impl() noexcept { return *impl_; }

    static size_t GetAllocatedHeapSize(ObjHeader* object) noexcept;

    size_t GetTotalHeapObjectsSizeBytes() const noexcept;

    gc::GCSchedulerConfig& gcSchedulerConfig() noexcept;

    void ClearForTests() noexcept;

    void StartFinalizerThreadIfNeeded() noexcept;
    void StopFinalizerThreadIfRunning() noexcept;
    bool FinalizersThreadIsRunning() noexcept;

    static void processObjectInMark(void* state, ObjHeader* object) noexcept;
    static void processArrayInMark(void* state, ArrayHeader* array) noexcept;
    static void processFieldInMark(void* state, ObjHeader* field) noexcept;

private:
    std_support::unique_ptr<Impl> impl_;
};

inline constexpr bool kSupportsMultipleMutators = true;

} // namespace gc
} // namespace kotlin
