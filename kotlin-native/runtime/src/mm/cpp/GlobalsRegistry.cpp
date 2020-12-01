/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "GlobalsRegistry.hpp"

#include <iterator>

#include "GlobalData.hpp"
#include "ThreadData.hpp"

using namespace kotlin;

// static
mm::GlobalsRegistry& mm::GlobalsRegistry::Instance() noexcept {
    return GlobalData::Instance().globalsRegistry();
}

void mm::GlobalsRegistry::RegisterStorageForGlobal(mm::ThreadData* threadData, ObjHeader** location) noexcept {
    threadData->globalsThreadQueue().Insert(location);
}

void mm::GlobalsRegistry::ProcessThread(mm::ThreadData* threadData) noexcept {
    threadData->globalsThreadQueue().Publish();
}

mm::GlobalsRegistry::GlobalsRegistry() = default;
mm::GlobalsRegistry::~GlobalsRegistry() = default;
