/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include <array>
#include <type_traits>

#include "KAssert.h"
#include "Memory.h"
#include "TypeInfo.h"
#include "Types.h"
#include "Utils.hpp"

namespace kotlin {
namespace test_support {

// TODO: Some concepts from here can be used in production code.

class TypeInfoHolder : private Pinned {
private:
    class Builder {
    protected:
        friend class TypeInfoHolder;

        virtual ~Builder() = default;

        int32_t instanceSize_ = 0;
        KStdVector<int32_t> objOffsets_;
        int32_t flags_ = 0;
    };

public:
    template <typename Payload>
    class ObjectBuilder : public Builder {
    public:
        ObjectBuilder() noexcept;

        ObjectBuilder&& addFlag(Konan_TypeFlags flag) noexcept {
            flags_ |= flag;
            return std::move(*this);
        }

        ObjectBuilder&& removeFlag(Konan_TypeFlags flag) noexcept {
            flags_ &= ~flag;
            return std::move(*this);
        }
    };

    template <typename Payload>
    class ArrayBuilder : public Builder {
    public:
        ArrayBuilder() noexcept { instanceSize_ = -static_cast<int32_t>(sizeof(Payload)); }

        ArrayBuilder&& addFlag(Konan_TypeFlags flag) noexcept {
            flags_ |= flag;
            return std::move(*this);
        }

        ArrayBuilder&& removeFlag(Konan_TypeFlags flag) noexcept {
            flags_ &= ~flag;
            return std::move(*this);
        }
    };

    explicit TypeInfoHolder(Builder&& builder) noexcept {
        typeInfo_.typeInfo_ = &typeInfo_;
        typeInfo_.instanceSize_ = builder.instanceSize_;
        objOffsets_ = std::move(builder.objOffsets_);
        typeInfo_.objOffsets_ = objOffsets_.data();
        if (&typeInfo_ == theArrayTypeInfo) {
            // Following RTTIGenerator.kt
            typeInfo_.objOffsetsCount_ = 1;
        } else {
            typeInfo_.objOffsetsCount_ = objOffsets_.size();
        }
        typeInfo_.flags_ = builder.flags_;
    }

    TypeInfo* typeInfo() noexcept { return &typeInfo_; }

private:
    TypeInfo typeInfo_{};
    KStdVector<int32_t> objOffsets_;
};

template <typename Payload>
class Object : private Pinned {
public:
    class FieldIterator {
    public:
        FieldIterator(Object& owner, size_t index) noexcept : owner_(owner), index_(index) {}

        ObjHeader*& operator*() noexcept {
            auto* header = &owner_.header_;
            return *reinterpret_cast<ObjHeader**>(reinterpret_cast<uintptr_t>(header) + header->type_info()->objOffsets_[index_]);
        }

        FieldIterator& operator++() noexcept {
            ++index_;
            return *this;
        }

        bool operator==(const FieldIterator& rhs) const noexcept { return &owner_ == &rhs.owner_ && index_ == rhs.index_; }

        bool operator!=(const FieldIterator& rhs) const noexcept { return !(*this == rhs); }

    private:
        Object& owner_;
        size_t index_;
    };

    class FieldIterable {
    public:
        explicit FieldIterable(Object& owner) noexcept : owner_(owner) {}

        size_t size() const noexcept { return owner_.header_.type_info()->objOffsetsCount_; }

        ObjHeader*& operator[](size_t index) noexcept { return *FieldIterator(owner_, index); }

        FieldIterator begin() noexcept { return FieldIterator(owner_, 0); }
        FieldIterator end() noexcept { return FieldIterator(owner_, size()); }

    private:
        Object& owner_;
    };

    static Object<Payload>& FromObjHeader(ObjHeader* obj) noexcept {
        static_assert(std::is_trivially_destructible_v<Object>, "Object destructor is not guaranteed to be called.");
        RuntimeAssert(
                TypeInfoHolder{TypeInfoHolder::ObjectBuilder<Payload>()}.typeInfo()->IsLayoutCompatible(obj->type_info()),
                "getting object from incompatible ObjHeader");
        auto& object = *reinterpret_cast<Object<Payload>*>(obj);
        RuntimeAssert(object.header() == obj, "Object layout is broken");
        return object;
    }

    explicit Object(const TypeInfo* typeInfo) noexcept {
        static_assert(std::is_trivially_destructible_v<Object>, "Object destructor is not guaranteed to be called.");
        RuntimeAssert(
                TypeInfoHolder{TypeInfoHolder::ObjectBuilder<Payload>()}.typeInfo()->IsLayoutCompatible(typeInfo),
                "constructing object from incompatible type info");
        header_.typeInfoOrMeta_ = const_cast<TypeInfo*>(typeInfo);
    }

    ObjHeader* header() noexcept { return &header_; }

    Payload& operator*() noexcept { return payload_; }
    Payload* operator->() noexcept { return &payload_; }

    FieldIterable fields() noexcept { return FieldIterable(*this); }

private:
    ObjHeader header_;
    Payload payload_{};
};

template <typename Payload>
TypeInfoHolder::ObjectBuilder<Payload>::ObjectBuilder() noexcept {
    instanceSize_ = sizeof(Object<Payload>);
    char c;
    Object<Payload>& object = *reinterpret_cast<Object<Payload>*>(&c);
    auto& payload = *object;
    using Field = ObjHeader* Payload::*;
    for (Field field : Payload::kFields) {
        auto& actualField = payload.*field;
        objOffsets_.push_back(reinterpret_cast<uintptr_t>(&actualField) - reinterpret_cast<uintptr_t>(object.header()));
    }
}

namespace internal {

// Array types are predetermined, use one of the subclasses below.
template <typename Payload, size_t ElementCount>
class Array : private Pinned {
public:
    static Array<Payload, ElementCount>& FromArrayHeader(ArrayHeader* arr) noexcept {
        static_assert(std::is_trivially_destructible_v<Array>, "Array destructor is not guaranteed to be called.");
        RuntimeAssert(
                TypeInfoHolder{TypeInfoHolder::ArrayBuilder<Payload>()}.typeInfo()->IsLayoutCompatible(arr->type_info()),
                "getting array from incompatible ArrayHeader");
        RuntimeAssert(arr->count_ == ElementCount, "getting array from ArrayHeader with different element count");
        auto& array = *reinterpret_cast<Array<Payload, ElementCount>*>(arr);
        RuntimeAssert(array.arrayHeader() == arr, "Array layout is broken");
        return array;
    }

    explicit Array(const TypeInfo* typeInfo) noexcept {
        static_assert(std::is_trivially_destructible_v<Array>, "Array destructor is not guaranteed to be called.");
        RuntimeAssert(
                TypeInfoHolder{TypeInfoHolder::ArrayBuilder<Payload>()}.typeInfo()->IsLayoutCompatible(typeInfo),
                "constructing array from incompatible type info");
        header_.typeInfoOrMeta_ = const_cast<TypeInfo*>(typeInfo);
        header_.count_ = ElementCount;
    }

    ObjHeader* header() noexcept { return header_.obj(); }
    ArrayHeader* arrayHeader() noexcept { return &header_; }

    std::array<Payload, ElementCount>& elements() noexcept { return elements_; }

private:
    ArrayHeader header_;
    std::array<Payload, ElementCount> elements_{};
};

} // namespace internal

template <size_t ElementCount>
class ObjectArray : public internal::Array<ObjHeader*, ElementCount> {
public:
    ObjectArray() noexcept : internal::Array<ObjHeader*, ElementCount>(theArrayTypeInfo) {}
};

template <size_t ElementCount>
class BooleanArray : public internal::Array<KBoolean, ElementCount> {
public:
    BooleanArray() noexcept : internal::Array<KBoolean, ElementCount>(theBooleanArrayTypeInfo) {}
};

template <size_t ElementCount>
class ByteArray : public internal::Array<KByte, ElementCount> {
public:
    ByteArray() noexcept : internal::Array<KByte, ElementCount>(theByteArrayTypeInfo) {}
};

template <size_t ElementCount>
class CharArray : public internal::Array<KChar, ElementCount> {
public:
    CharArray() noexcept : internal::Array<KChar, ElementCount>(theCharArrayTypeInfo) {}
};

template <size_t ElementCount>
class DoubleArray : public internal::Array<KDouble, ElementCount> {
public:
    DoubleArray() noexcept : internal::Array<KDouble, ElementCount>(theDoubleArrayTypeInfo) {}
};

template <size_t ElementCount>
class FloatArray : public internal::Array<KFloat, ElementCount> {
public:
    FloatArray() noexcept : internal::Array<KFloat, ElementCount>(theFloatArrayTypeInfo) {}
};

template <size_t ElementCount>
class IntArray : public internal::Array<KInt, ElementCount> {
public:
    IntArray() noexcept : internal::Array<KInt, ElementCount>(theIntArrayTypeInfo) {}
};

template <size_t ElementCount>
class LongArray : public internal::Array<KLong, ElementCount> {
public:
    LongArray() noexcept : internal::Array<KLong, ElementCount>(theLongArrayTypeInfo) {}
};

template <size_t ElementCount>
class NativePtrArray : public internal::Array<KNativePtr, ElementCount> {
public:
    NativePtrArray() noexcept : internal::Array<KNativePtr, ElementCount>(theNativePtrArrayTypeInfo) {}
};

template <size_t ElementCount>
class ShortArray : public internal::Array<KShort, ElementCount> {
public:
    ShortArray() noexcept : internal::Array<KShort, ElementCount>(theShortArrayTypeInfo) {}
};

template <size_t ElementCount>
class String : public internal::Array<KChar, ElementCount> {
public:
    String() noexcept : internal::Array<KChar, ElementCount>(theStringTypeInfo) {}
};

} // namespace test_support
} // namespace kotlin
