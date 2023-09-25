/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <cstdint>
#include <memory>

#include "GC.hpp"
#include "Utils.hpp"

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

    size_t estimateOverheadPerThread() noexcept;

private:
    std::unique_ptr<Impl> impl_;
};

void initObjectPool() noexcept;
// Instruct the allocator to free unused resources.
void compactObjectPoolInCurrentThread() noexcept;

gc::GC::ObjectData& objectDataForObject(ObjHeader* object) noexcept;
ObjHeader* objectForObjectData(gc::GC::ObjectData& objectData) noexcept;

// This does not take into account how much storage did the underlying allocator reserved.
size_t allocatedHeapSize(ObjHeader* object) noexcept;

size_t allocatedBytes() noexcept;

void destroyExtraObjectData(mm::ExtraObjectData& extraObject) noexcept;
}
