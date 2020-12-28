/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ObjectFactory.hpp"

#include "Alignment.hpp"
#include "Alloc.h"
#include "GlobalData.hpp"
#include "Types.h"

using namespace kotlin;

ObjHeader* mm::ObjectFactory::ThreadQueue::CreateObject(const TypeInfo* typeInfo) noexcept {
    RuntimeAssert(!typeInfo->IsArray(), "Must not be an array");
    size_t allocSize = typeInfo->instanceSize_;
    auto& node = producer_.Insert(allocSize);
    auto* object = static_cast<ObjHeader*>(node.Data());
    object->typeInfoOrMeta_ = const_cast<TypeInfo*>(typeInfo);
    return object;
}

ArrayHeader* mm::ObjectFactory::ThreadQueue::CreateArray(const TypeInfo* typeInfo, uint32_t count) noexcept {
    RuntimeAssert(typeInfo->IsArray(), "Must be an array");
    uint32_t arraySize = static_cast<uint32_t>(-typeInfo->instanceSize_) * count;
    // Note: array body is aligned, but for size computation it is enough to align the sum.
    size_t allocSize = AlignUp(sizeof(ArrayHeader) + arraySize, kObjectAlignment);
    auto& node = producer_.Insert(allocSize);
    auto* array = static_cast<ArrayHeader*>(node.Data());
    array->typeInfoOrMeta_ = const_cast<TypeInfo*>(typeInfo);
    array->count_ = count;
    return array;
}

bool mm::ObjectFactory::Iterator::IsArray() noexcept {
    // `ArrayHeader` and `ObjHeader` are kept compatible, so the former can
    // be always casted to the other.
    auto* object = static_cast<ObjHeader*>((*iterator_).Data());
    return object->type_info()->IsArray();
}

ObjHeader* mm::ObjectFactory::Iterator::GetObjHeader() noexcept {
    auto* object = static_cast<ObjHeader*>((*iterator_).Data());
    RuntimeAssert(!object->type_info()->IsArray(), "Must not be an array");
    return object;
}

ArrayHeader* mm::ObjectFactory::Iterator::GetArrayHeader() noexcept {
    auto* array = static_cast<ArrayHeader*>((*iterator_).Data());
    RuntimeAssert(array->type_info()->IsArray(), "Must be an array");
    return array;
}

mm::ObjectFactory::ObjectFactory() noexcept = default;
mm::ObjectFactory::~ObjectFactory() = default;

// static
mm::ObjectFactory& mm::ObjectFactory::Instance() noexcept {
    return GlobalData::Instance().objectFactory();
}
