/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <array>
#include <type_traits>
#include <vector>

#include "KAssert.h"
#include "Memory.h"
#include "ReferenceOps.hpp"
#include "TypeInfo.h"
#include "Types.h"
#include "Utils.hpp"

namespace kotlin {
namespace test_support {

// TODO: Some concepts from here can be used in production code.

template<typename Host>
using RefFieldPtr = mm::RefField Host::*;

template<typename Host>
using NoRefFields = std::array<RefFieldPtr<Host>, 0>;

class TypeInfoHolder : private Pinned {
private:
    class Builder {
    protected:
        friend class TypeInfoHolder;

        virtual ~Builder() = default;

        int32_t instanceSize_ = 0;
        std::vector<int32_t> objOffsets_;
        int32_t objOffsetsCount_ = 0;
        int32_t flags_ = 0;
        int32_t instanceAlignment_ = 8;
        const TypeInfo* superType_ = nullptr;
        void (*processObjectInMark_)(void*, ObjHeader*) = nullptr;
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

        ObjectBuilder&& setSuperType(const TypeInfo* superType) noexcept {
            superType_ = superType;
            return std::move(*this);
        }
    };

    template <typename Payload>
    class ArrayBuilder : public Builder {
    public:
        ArrayBuilder() noexcept {
            instanceSize_ = -static_cast<int32_t>(sizeof(Payload));
            if constexpr (std::is_same_v<Payload, ObjHeader*>) {
                // Following RTTIGenerator.kt
                objOffsetsCount_ = 1;
                processObjectInMark_ = Kotlin_processArrayInMark;
            } else {
                processObjectInMark_ = Kotlin_processEmptyObjectInMark;
            }
        }

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
        typeInfo_.objOffsetsCount_ = builder.objOffsetsCount_;
        typeInfo_.processObjectInMark = builder.processObjectInMark_;
        typeInfo_.flags_ = builder.flags_;
        typeInfo_.superType_ = builder.superType_;
        typeInfo_.instanceAlignment_ = builder.instanceAlignment_;
    }

    template<>
    class ArrayBuilder<mm::RefField> : public ArrayBuilder<ObjHeader*> {};

    TypeInfo* typeInfo() noexcept { return &typeInfo_; }

private:
    TypeInfo typeInfo_{};
    std::vector<int32_t> objOffsets_;
};

class Any : private Pinned {
public:
    ObjHeader* header() noexcept { return &header_; }

    void installMetaObject() noexcept { (void)header()->meta_object(); }

protected:
    Any() noexcept = default;
    ~Any() = default;

    ObjHeader header_;
};

template <typename Payload>
class Object : public Any {
public:
    class FieldIterator {
    public:
        FieldIterator(Object& owner, size_t index) noexcept : owner_(owner), index_(index) {}

        mm::RefField& operator*() noexcept {
            auto* header = &owner_.header_;
            return *reinterpret_cast<mm::RefField*>(reinterpret_cast<uintptr_t>(header) + header->type_info()->objOffsets_[index_]);
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

        mm::RefField& operator[](size_t index) noexcept { return *FieldIterator(owner_, index); }

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

    Payload& operator*() noexcept { return payload_; }
    Payload* operator->() noexcept { return &payload_; }

    FieldIterable fields() noexcept { return FieldIterable(*this); }

private:
    Payload payload_{};
};

template <typename Payload>
TypeInfoHolder::ObjectBuilder<Payload>::ObjectBuilder() noexcept {
    instanceSize_ = sizeof(Object<Payload>);
    char c;
    Object<Payload>& object = *reinterpret_cast<Object<Payload>*>(&c);
    auto& payload = *object;
    for (RefFieldPtr<Payload> field : Payload::kFields) {
        auto& actualField = payload.*field;
        objOffsets_.push_back(reinterpret_cast<uintptr_t>(&actualField) - reinterpret_cast<uintptr_t>(object.header()));
    }
    objOffsetsCount_ = objOffsets_.size();
    processObjectInMark_ = Kotlin_processObjectInMark;
}

namespace internal {

// Array types are predetermined, use one of the subclasses below.
template <typename Payload, size_t ElementCount>
class Array : public Any {
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
        count_ = ElementCount;
    }

    ArrayHeader* arrayHeader() noexcept { return header()->array(); }

    std::array<Payload, ElementCount>& elements() noexcept { return elements_; }

private:
    uint32_t count_;
    alignas(ArrayHeader) std::array<Payload, ElementCount> elements_{};
};

} // namespace internal

template <size_t ElementCount>
class ObjectArray : public internal::Array<mm::RefField, ElementCount> {
public:
    static ObjectArray<ElementCount>& FromArrayHeader(ArrayHeader* arr) noexcept {
        return static_cast<ObjectArray<ElementCount>&>(internal::Array<mm::RefField, ElementCount>::FromArrayHeader(arr));
    }

    ObjectArray() noexcept : internal::Array<mm::RefField, ElementCount>(theArrayTypeInfo) {}
};

template <size_t ElementCount>
class BooleanArray : public internal::Array<KBoolean, ElementCount> {
public:
    static BooleanArray<ElementCount>& FromArrayHeader(ArrayHeader* arr) noexcept {
        return static_cast<BooleanArray<ElementCount>&>(internal::Array<KBoolean, ElementCount>::FromArrayHeader(arr));
    }

    BooleanArray() noexcept : internal::Array<KBoolean, ElementCount>(theBooleanArrayTypeInfo) {}
};

template <size_t ElementCount>
class ByteArray : public internal::Array<KByte, ElementCount> {
public:
    static ByteArray<ElementCount>& FromArrayHeader(ArrayHeader* arr) noexcept {
        return static_cast<ByteArray<ElementCount>&>(internal::Array<KByte, ElementCount>::FromArrayHeader(arr));
    }

    ByteArray() noexcept : internal::Array<KByte, ElementCount>(theByteArrayTypeInfo) {}
};

template <size_t ElementCount>
class CharArray : public internal::Array<KChar, ElementCount> {
public:
    static CharArray<ElementCount>& FromArrayHeader(ArrayHeader* arr) noexcept {
        return static_cast<CharArray<ElementCount>&>(internal::Array<KChar, ElementCount>::FromArrayHeader(arr));
    }

    CharArray() noexcept : internal::Array<KChar, ElementCount>(theCharArrayTypeInfo) {}
};

template <size_t ElementCount>
class DoubleArray : public internal::Array<KDouble, ElementCount> {
public:
    static DoubleArray<ElementCount>& FromArrayHeader(ArrayHeader* arr) noexcept {
        return static_cast<DoubleArray<ElementCount>&>(internal::Array<KDouble, ElementCount>::FromArrayHeader(arr));
    }

    DoubleArray() noexcept : internal::Array<KDouble, ElementCount>(theDoubleArrayTypeInfo) {}
};

template <size_t ElementCount>
class FloatArray : public internal::Array<KFloat, ElementCount> {
public:
    static FloatArray<ElementCount>& FromArrayHeader(ArrayHeader* arr) noexcept {
        return static_cast<FloatArray<ElementCount>&>(internal::Array<KFloat, ElementCount>::FromArrayHeader(arr));
    }

    FloatArray() noexcept : internal::Array<KFloat, ElementCount>(theFloatArrayTypeInfo) {}
};

template <size_t ElementCount>
class IntArray : public internal::Array<KInt, ElementCount> {
public:
    static IntArray<ElementCount>& FromArrayHeader(ArrayHeader* arr) noexcept {
        return static_cast<IntArray<ElementCount>&>(internal::Array<KInt, ElementCount>::FromArrayHeader(arr));
    }

    IntArray() noexcept : internal::Array<KInt, ElementCount>(theIntArrayTypeInfo) {}
};

template <size_t ElementCount>
class LongArray : public internal::Array<KLong, ElementCount> {
public:
    static LongArray<ElementCount>& FromArrayHeader(ArrayHeader* arr) noexcept {
        return static_cast<LongArray<ElementCount>&>(internal::Array<KLong, ElementCount>::FromArrayHeader(arr));
    }

    LongArray() noexcept : internal::Array<KLong, ElementCount>(theLongArrayTypeInfo) {}
};

template <size_t ElementCount>
class NativePtrArray : public internal::Array<KNativePtr, ElementCount> {
public:
    static NativePtrArray<ElementCount>& FromArrayHeader(ArrayHeader* arr) noexcept {
        return static_cast<NativePtrArray<ElementCount>&>(internal::Array<KNativePtr, ElementCount>::FromArrayHeader(arr));
    }

    NativePtrArray() noexcept : internal::Array<KNativePtr, ElementCount>(theNativePtrArrayTypeInfo) {}
};

template <size_t ElementCount>
class ShortArray : public internal::Array<KShort, ElementCount> {
public:
    static ShortArray<ElementCount>& FromArrayHeader(ArrayHeader* arr) noexcept {
        return static_cast<ShortArray<ElementCount>&>(internal::Array<KShort, ElementCount>::FromArrayHeader(arr));
    }

    ShortArray() noexcept : internal::Array<KShort, ElementCount>(theShortArrayTypeInfo) {}
};

template <size_t ElementCount>
class String : public internal::Array<KChar, ElementCount> {
public:
    static String<ElementCount>& FromArrayHeader(ArrayHeader* arr) noexcept {
        return static_cast<String<ElementCount>&>(internal::Array<KChar, ElementCount>::FromArrayHeader(arr));
    }

    String() noexcept : internal::Array<KChar, ElementCount>(theStringTypeInfo) {}
};

struct RegularWeakReferenceImplPayload {
    void* weakRef;
    void* referred;

    static constexpr test_support::NoRefFields<RegularWeakReferenceImplPayload> kFields{};
};

extern "C" OBJ_GETTER(Konan_RegularWeakReferenceImpl_get, ObjHeader*);

class RegularWeakReferenceImpl : public Object<RegularWeakReferenceImplPayload> {
public:
    static RegularWeakReferenceImpl& FromObjHeader(ObjHeader* obj) noexcept {
        RuntimeAssert(obj->type_info() == theRegularWeakReferenceImplTypeInfo, "Invalid type");
        return static_cast<RegularWeakReferenceImpl&>(Object::FromObjHeader(obj));
    }

    RegularWeakReferenceImpl() noexcept : Object(theRegularWeakReferenceImplTypeInfo) {}

    OBJ_GETTER0(get) noexcept { RETURN_RESULT_OF(Konan_RegularWeakReferenceImpl_get, header()); }

    ObjHeader* get() noexcept {
        ObjHeader* result;
        return get(&result);
    }
};

} // namespace test_support
} // namespace kotlin
