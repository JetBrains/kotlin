/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ExtraObjectDataFactory.hpp"

#include "GlobalData.hpp"
#include "ThreadData.hpp"

using namespace kotlin;

mm::ExtraObjectData& mm::ExtraObjectDataFactory::ThreadQueue::CreateExtraObjectDataForObject(
        ObjHeader* baseObject, const TypeInfo* info) noexcept {
    return **Emplace(baseObject, info);
}

void mm::ExtraObjectDataFactory::ThreadQueue::DestroyExtraObjectData(ExtraObjectData& data) noexcept {
    Erase(&Queue::Node::fromValue(data));
}

mm::ExtraObjectDataFactory::ExtraObjectDataFactory() = default;
mm::ExtraObjectDataFactory::~ExtraObjectDataFactory() = default;
