/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <atomic>
#include <cstdint>
#include <memory>

#include "ExtraObjectData.hpp"
#include "GCScheduler.hpp"
#include "Memory.h"
#include "Utils.hpp"

namespace kotlin {

namespace alloc {
class Allocator;
}

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

        void OnSuspendForGC() noexcept;

        void safePoint() noexcept;

        void onThreadRegistration() noexcept;

        void onAllocation(ObjHeader* object) noexcept;

    private:
        std::unique_ptr<Impl> impl_;
    };

    // Header to be placed before each heap object. GC will use this to keep its data if needed.
    // This is used via `type_layout::descriptor_t`, which is specialized below.
    // If GC doesn't need any data, it can make `size()` return 0 and `alignment()`
    // return 1.
    // Note: GC does not deinitialize `ObjectData`, so the implementations must ensure that
    //       the destructor is a trivial one.
    class ObjectData;

    GC(alloc::Allocator& allocator, gcScheduler::GCScheduler& gcScheduler) noexcept;
    ~GC();

    Impl& impl() noexcept { return *impl_; }

    void ClearForTests() noexcept;

    void StartFinalizerThreadIfNeeded() noexcept;
    void StopFinalizerThreadIfRunning() noexcept;
    bool FinalizersThreadIsRunning() noexcept;

    static void processObjectInMark(void* state, ObjHeader* object) noexcept;
    static void processArrayInMark(void* state, ArrayHeader* array) noexcept;
    static void processFieldInMark(void* state, ObjHeader* field) noexcept;

    // TODO: These should exist only in the scheduler.
    int64_t Schedule() noexcept;
    void WaitFinished(int64_t epoch) noexcept;
    void WaitFinalizers(int64_t epoch) noexcept;

private:
    std::unique_ptr<Impl> impl_;
};

bool isMarked(ObjHeader* object) noexcept;
OBJ_GETTER(tryRef, std::atomic<ObjHeader*>& object) noexcept;

// This will drop the mark bit if it was set and return `true`.
// If the mark bit was unset, this will return `false`.
bool tryResetMark(GC::ObjectData& objectData) noexcept;

inline constexpr bool kSupportsMultipleMutators = true;

} // namespace gc

template <>
struct type_layout::descriptor<gc::GC::ObjectData> {
    struct type {
        using value_type = gc::GC::ObjectData;

        static uint64_t size() noexcept;
        static size_t alignment() noexcept;

        static value_type* construct(uint8_t* ptr) noexcept;
    };
};

} // namespace kotlin
