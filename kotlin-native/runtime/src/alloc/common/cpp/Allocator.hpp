/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <cstdint>
#include <memory>

#include "GC.hpp"
#include "GCStatistics.hpp"
#include "Utils.hpp"
#include "Memory.h"

namespace kotlin::alloc {

// TODO: Move allocator-specific data and API here.
class Allocator : private Pinned {
public:
    class Impl;

    class ThreadData : private Pinned {
    public:
        class Impl;

        explicit ThreadData(Allocator& allocator) noexcept;
        ~ThreadData();

        Impl& impl() noexcept { return *impl_; }

        ObjHeader* allocateObject(const TypeInfo* typeInfo) noexcept;
        ArrayHeader* allocateArray(const TypeInfo* typeInfo, uint32_t elements) noexcept;
        mm::ExtraObjectData& allocateExtraObjectData(ObjHeader* object, const TypeInfo* typeInfo) noexcept;
        void destroyUnattachedExtraObjectData(mm::ExtraObjectData& extraObject) noexcept;

        void prepareForGC() noexcept;

        // TODO: Move into AllocatorTestSupport.hpp
        void clearForTests() noexcept;

    private:
        std::unique_ptr<Impl> impl_;
    };

    Allocator() noexcept;
    ~Allocator();

    Impl& impl() noexcept { return *impl_; }

    void prepareForGC() noexcept;

    // TODO: Move into AllocatorTestSupport.hpp
    void clearForTests() noexcept;

    void TraverseAllocatedObjects(std::function<void(ObjHeader*)> fn) noexcept;

    void TraverseAllocatedExtraObjects(std::function<void(mm::ExtraObjectData*)> fn) noexcept;

    void startFinalizerThreadIfNeeded() noexcept;
    void stopFinalizerThreadIfRunning() noexcept;
    bool finalizersThreadIsRunning() noexcept;

    void configureMainThreadFinalizerProcessor(std::function<void(alloc::RunLoopFinalizerProcessorConfig&)> f) noexcept;
    bool mainThreadFinalizerProcessorAvailable() noexcept;

    void sweep(gc::GCHandle gcHandle) noexcept;
    void scheduleFinalization(gc::GCHandle gcHandle) noexcept;

private:
    std::unique_ptr<Impl> impl_;
};

void initObjectPool() noexcept;
// Instruct the allocator to free unused resources.
void compactObjectPoolInCurrentThread() noexcept;

gc::GC::ObjectData& objectDataForObject(ObjHeader* object) noexcept;
ObjHeader* objectForObjectData(gc::GC::ObjectData& objectData) noexcept;

// A read-only snapshot of the heap's page layout that resolves an interior pointer (such as the
// address of a reference field, i.e. a remembered-set slot) back to the heap object that contains it.
// Build one while the world is stopped and query it repeatedly; it takes no locks and reads only
// object layout that is stable during STW. Only the custom allocator implements resolution -- other
// backends always return nullptr from `containerOf`, which callers must treat as "unknown" and handle
// conservatively (e.g. keep the referent). Used by the generational GC to filter its remembered set
// down to genuine old->young edges; see gc/gms.
class HeapLayoutSnapshot : private Pinned {
public:
    HeapLayoutSnapshot() noexcept;
    ~HeapLayoutSnapshot();

    // Returns the heap object that contains `interiorPointer`, or nullptr if it is not inside any
    // managed heap object (or this allocator backend cannot resolve interior pointers).
    ObjHeader* containerOf(void* interiorPointer) const noexcept;

    // Whether this allocator backend can resolve interior pointers at all (custom allocator: yes;
    // other backends: no). When false, `containerOf` always returns nullptr, so a nullptr result
    // cannot be distinguished from "not in any heap object" and callers must fall back to a
    // conservative policy rather than treating nullptr as "skip". See gc/gms's remembered-set drain.
    bool resolvesInteriorPointers() const noexcept;

private:
    class Impl;
    std::unique_ptr<Impl> impl_;
};

// Whether the linked allocator backend can resolve an interior pointer to its containing object (see
// HeapLayoutSnapshot::resolvesInteriorPointers). Unlike constructing a HeapLayoutSnapshot, this is a
// cheap constant query with no heap scan, so it can gate policy decisions cheaply. The generational
// GC uses it to run Eden collections only when true (a slot cannot be resolved-and-validated at drain
// otherwise, risking a stale-slot dereference); see gc/gms's GmsCollectionPolicy.
bool heapLayoutResolvesInteriorPointers() noexcept;

// This does not take into account how much storage did the underlying allocator reserved.
size_t allocatedHeapSize(ObjHeader* object) noexcept;

size_t allocatedBytes() noexcept;

void destroyExtraObjectData(mm::ExtraObjectData& extraObject) noexcept;
} // namespace kotlin::alloc
