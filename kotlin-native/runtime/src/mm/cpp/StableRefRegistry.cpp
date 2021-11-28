/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "StableRefRegistry.hpp"

#include "GlobalData.hpp"
#include "ThreadData.hpp"

using namespace kotlin;

// static
mm::StableRefRegistry& mm::StableRefRegistry::Instance() noexcept {
    return GlobalData::Instance().stableRefRegistry();
}

mm::StableRefRegistry::Node* mm::StableRefRegistry::RegisterStableRef(mm::ThreadData* threadData, ObjHeader* object) noexcept {
    return threadData->stableRefThreadQueue().Insert(object);
}

void mm::StableRefRegistry::UnregisterStableRef(mm::ThreadData* threadData, Node* node) noexcept {
    threadData->stableRefThreadQueue().Erase(node);
}

void mm::StableRefRegistry::ProcessThread(mm::ThreadData* threadData) noexcept {
    threadData->stableRefThreadQueue().Publish();
}

void mm::StableRefRegistry::ProcessDeletions() noexcept {
    stableRefs_.ApplyDeletions();
}

mm::StableRefRegistry::StableRefRegistry() = default;
mm::StableRefRegistry::~StableRefRegistry() = default;
