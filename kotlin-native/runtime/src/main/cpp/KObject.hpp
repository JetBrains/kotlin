/*
 * Copyright 2010-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "Memory.h"
#include "TypeInfo.h"
#include "TypeLayout.hpp"
#include "Utils.hpp"

namespace kotlin {

struct KObject : private Pinned {
    class descriptor {
    public:
        using value_type = KObject;

        // `typeInfo` may be `nullptr` if `size()` and `construct()` are not used.
        explicit descriptor(const TypeInfo* typeInfo) noexcept : typeInfo_(typeInfo) {
            RuntimeAssert(!typeInfo_ || !typeInfo_->IsArray(), "Creating KObject::descriptor for array with type info %p", typeInfo_);
        }

        static constexpr size_t alignment() noexcept { return kObjectAlignment; }

        uint64_t size() const noexcept {
            RuntimeAssert(typeInfo_ != nullptr, "Cannot call size() on KObject::descriptor(nullptr)");
            return typeInfo_->instanceSize_;
        }

        value_type* construct(uint8_t* ptr) noexcept {
            RuntimeAssert(
                    isZeroed(std_support::span<uint8_t>(ptr, size())), "KObject::descriptor::construct@%p memory is not zeroed", ptr);
            return reinterpret_cast<value_type*>(ptr);
        }

    private:
        const TypeInfo* typeInfo_;
    };

    static KObject* from(ObjHeader* header) noexcept { return reinterpret_cast<KObject*>(header); }

    ObjHeader* header() noexcept { return reinterpret_cast<ObjHeader*>(this); }

private:
    KObject() = delete;
    ~KObject() = delete;
};

static_assert(std::is_same_v<type_layout::descriptor_t<KObject>, KObject::descriptor>);

// Every `KArray` is also a `KObject`.
struct KArray : private Pinned {
    class descriptor {
    public:
        using value_type = KArray;

        // `typeInfo` may be `nullptr` if `size()` and `construct()` are not used.
        explicit descriptor(const TypeInfo* typeInfo, uint32_t count) noexcept : typeInfo_(typeInfo), count_(count) {
            RuntimeAssert(
                    !typeInfo_ || typeInfo_->IsArray(), "Creating KArray::descriptor for a plain object with type info %p", typeInfo_);
        }

        static constexpr size_t alignment() noexcept { return kObjectAlignment; }

        uint64_t size() const noexcept {
            RuntimeAssert(typeInfo_ != nullptr, "Cannot call size() on KArray::descriptor(nullptr)");
            uint64_t elementSize = static_cast<uint64_t>(-typeInfo_->instanceSize_);
            // This is true for now. May change with arrays of value types. Or with
            // support of overaligned types.
            size_t elementAlignment = elementSize;
            // -(int32_t min) * uint32_t max cannot overflow uint64_t. And are capped
            // at about half of uint64_t max.
            auto elementsSize = elementSize * count_;
            return AlignUp<uint64_t>(AlignUp(sizeof(ArrayHeader), elementAlignment) + elementsSize, alignment());
        }

        value_type* construct(uint8_t* ptr) noexcept {
            RuntimeAssert(
                    isZeroed(std_support::span<uint8_t>(ptr, size())), "KArray::descriptor::construct@%p memory is not zeroed", ptr);
            return reinterpret_cast<KArray*>(ptr);
        }

    private:
        const TypeInfo* typeInfo_;
        uint32_t count_;
    };

    static KArray* from(ArrayHeader* header) noexcept { return reinterpret_cast<KArray*>(header); }

    ArrayHeader* header() noexcept { return reinterpret_cast<ArrayHeader*>(this); }

    operator KObject&() noexcept { return reinterpret_cast<KObject&>(*this); }

private:
    KArray() = delete;
    ~KArray() = delete;
};

static_assert(std::is_same_v<type_layout::descriptor_t<KArray>, KArray::descriptor>);


} // namespace kotlin