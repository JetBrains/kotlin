/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "InitializationScheme.hpp"

#include "Common.h"
#include "ObjectOps.hpp"
#include "ThreadData.hpp"

using namespace kotlin;

OBJ_GETTER(mm::InitThreadLocalSingleton, ThreadData* threadData, ObjHeader** location, const TypeInfo* typeInfo, void (*ctor)(ObjHeader*)) {
    // TODO: Is it possible that threadData != CurrentThreadData?
    AssertThreadState(threadData, ThreadState::kRunnable);
    if (auto* value = *location) {
        // Initialized by someone else.
        RETURN_OBJ(value);
    }
    auto* value = mm::AllocateObject(threadData, typeInfo, OBJ_RESULT);
    mm::SetHeapRef(location, value);
#if KONAN_NO_EXCEPTIONS
    ctor(value);
#else
    try {
        ctor(value);
    } catch (...) {
        mm::SetStackRef(OBJ_RESULT, nullptr);
        mm::SetHeapRef(location, nullptr);
        throw;
    }
#endif
    return value;
}

OBJ_GETTER(mm::InitSingleton, ThreadData* threadData, ObjHeader** location, const TypeInfo* typeInfo, void (*ctor)(ObjHeader*)) {
    // TODO: Is it possible that threadData != CurrentThreadData?
    AssertThreadState(threadData, ThreadState::kRunnable);
    auto& initializingSingletons = threadData->initializingSingletons();

    // Search from the top of the stack.
    for (auto it = initializingSingletons.rbegin(); it != initializingSingletons.rend(); ++it) {
        if (it->first == location) {
            RETURN_OBJ(it->second);
        }
    }

    ObjHeader* initializing = reinterpret_cast<ObjHeader*>(1);

    // Spin lock.
    ObjHeader* value = nullptr;
    {
        ThreadStateGuard guard(ThreadState::kNative);
        while ((value = __sync_val_compare_and_swap(location, nullptr, initializing)) == initializing) {
        }
    }
    if (value != nullptr) {
        // Initialized by someone else.
        RETURN_OBJ(value);
    }
    auto* object = mm::AllocateObject(threadData, typeInfo, OBJ_RESULT);
    initializingSingletons.push_back(std::make_pair(location, object));

#if KONAN_NO_EXCEPTIONS
    ctor(object);
#else
    try {
        ctor(object);
    } catch (...) {
        mm::SetStackRef(OBJ_RESULT, nullptr);
        mm::SetHeapRefAtomic(location, nullptr);
        initializingSingletons.pop_back();
        throw;
    }
#endif
    mm::GlobalsRegistry::Instance().RegisterStorageForGlobal(threadData, location);
    mm::SetHeapRefAtomic(location, object);
    initializingSingletons.pop_back();
    return object;
}
