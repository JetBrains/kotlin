/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ExtraObjectDataFactory.hpp"

#include "GlobalData.hpp"
#include "ThreadData.hpp"

using namespace kotlin;

mm::ExtraObjectData& alloc::ExtraObjectDataFactory::ThreadQueue::CreateExtraObjectDataForObject(
        ObjHeader* baseObject, const TypeInfo* info) noexcept {
    return **Emplace(baseObject, info);
}

void alloc::ExtraObjectDataFactory::ThreadQueue::DestroyExtraObjectData(mm::ExtraObjectData& data) noexcept {
    Erase(&Queue::Node::fromValue(data));
}

alloc::ExtraObjectDataFactory::ExtraObjectDataFactory() = default;
alloc::ExtraObjectDataFactory::~ExtraObjectDataFactory() = default;
