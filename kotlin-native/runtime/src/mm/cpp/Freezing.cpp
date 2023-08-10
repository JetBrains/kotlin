/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "Freezing.hpp"

#include <unordered_set>
#include <vector>

#include "ExtraObjectData.hpp"
#include "FreezeHooks.hpp"
#include "Memory.h"
#include "Natives.h"
#include "ObjectTraversal.hpp"
#include "Types.h"

using namespace kotlin;

bool mm::IsFrozen(const ObjHeader* object) noexcept {
    if (!compiler::freezingChecksEnabled()) return false;
    if (object->permanent()) {
        return true;
    }

    if (auto* extraObjectData = mm::ExtraObjectData::Get(object)) {
        return extraObjectData->getFlag(mm::ExtraObjectData::FLAGS_FROZEN);
    }
    return false;
}

ObjHeader* mm::FreezeSubgraph(ObjHeader* root) noexcept {
    if (IsFrozen(root)) return nullptr;

    std::vector<ObjHeader*> objects;
    std::vector<ObjHeader*> stack;
    // TODO: This may be a suboptimal container for the job.
    std::unordered_set<ObjHeader*> visited;
    stack.push_back(root);
    while (!stack.empty()) {
        ObjHeader* object = stack.back();
        stack.pop_back();
        auto visitedResult = visited.insert(object);
        if (!visitedResult.second) continue;
        objects.push_back(object);
        RunFreezeHooks(object);
        traverseReferredObjects(object, [&stack](ObjHeader* field) noexcept {
            if (!IsFrozen(field)) {
                stack.push_back(field);
            }
        });
    }
    for (auto* object : objects) {
        if (auto* extraObjectData = mm::ExtraObjectData::Get(object)) {
            if (extraObjectData->getFlag(mm::ExtraObjectData::FLAGS_NEVER_FROZEN)) {
                return object;
            }
        }
    }
    for (auto* object : objects) {
        mm::ExtraObjectData::GetOrInstall(object).setFlag(mm::ExtraObjectData::FLAGS_FROZEN);
    }
    return nullptr;
}

bool mm::EnsureNeverFrozen(ObjHeader* object) noexcept {
    if (IsFrozen(object)) {
        return false;
    }

    mm::ExtraObjectData::GetOrInstall(object).setFlag(mm::ExtraObjectData::FLAGS_NEVER_FROZEN);
    return true;
}
