/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ExtraObjectDataFactory.hpp"

#include "GlobalData.hpp"
#include "ThreadData.hpp"

using namespace kotlin;

// static
mm::ExtraObjectDataFactory& mm::ExtraObjectDataFactory::Instance() noexcept {
    return GlobalData::Instance().extraObjectDataFactory();
}

mm::ExtraObjectData& mm::ExtraObjectDataFactory::CreateExtraObjectDataForObject(mm::ThreadData* threadData, TypeInfo* info) noexcept {
    return **threadData->extraObjectDataThreadQueue().Emplace(info);
}

void mm::ExtraObjectDataFactory::DestroyExtraObjectData(mm::ThreadData* threadData, ExtraObjectData& data) noexcept {
    threadData->extraObjectDataThreadQueue().Erase(&Queue::Node::fromValue(data));
}

void mm::ExtraObjectDataFactory::ProcessThread(mm::ThreadData* threadData) noexcept {
    threadData->extraObjectDataThreadQueue().Publish();
}

void mm::ExtraObjectDataFactory::ProcessDeletions() noexcept {
    extraObjects_.ApplyDeletions();
}

mm::ExtraObjectDataFactory::ExtraObjectDataFactory() = default;
mm::ExtraObjectDataFactory::~ExtraObjectDataFactory() = default;
