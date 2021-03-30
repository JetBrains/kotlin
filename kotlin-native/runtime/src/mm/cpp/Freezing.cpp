/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "Freezing.hpp"

#include "ExtraObjectData.hpp"
#include "FreezeHooks.hpp"
#include "Memory.h"
#include "Natives.h"
#include "ObjectTraversal.hpp"
#include "Types.h"

using namespace kotlin;

bool mm::IsFrozen(const ObjHeader* object) noexcept {
    if (object->permanent()) {
        return true;
    }

    if (auto* extraObjectData = mm::ExtraObjectData::Get(object)) {
        return (extraObjectData->flags() & mm::ExtraObjectData::FLAGS_FROZEN) != 0;
    }
    return false;
}

ObjHeader* mm::FreezeSubgraph(ObjHeader* root) noexcept {
    if (IsFrozen(root)) return nullptr;

    KStdVector<ObjHeader*> objects;
    KStdVector<ObjHeader*> stack;
    // TODO: This may be a suboptimal container for the job.
    KStdUnorderedSet<ObjHeader*> visited;
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
            if ((extraObjectData->flags() & mm::ExtraObjectData::FLAGS_NEVER_FROZEN) != 0) return object;
        }
    }
    for (auto* object : objects) {
        auto& flags = mm::ExtraObjectData::GetOrInstall(object).flags();
        flags = static_cast<mm::ExtraObjectData::Flags>(flags | mm::ExtraObjectData::FLAGS_FROZEN);
    }
    return nullptr;
}

bool mm::EnsureNeverFrozen(ObjHeader* object) noexcept {
    if (IsFrozen(object)) {
        return false;
    }

    auto& flags = mm::ExtraObjectData::GetOrInstall(object).flags();
    flags = static_cast<mm::ExtraObjectData::Flags>(flags | mm::ExtraObjectData::FLAGS_NEVER_FROZEN);
    return true;
}
