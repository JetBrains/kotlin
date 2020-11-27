/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_MM_GLOBALS_REGISTRY_H
#define RUNTIME_MM_GLOBALS_REGISTRY_H

#include "Memory.h"
#include "MultiSourceQueue.hpp"
#include "ThreadRegistry.hpp"
#include "Utils.hpp"

namespace kotlin {
namespace mm {

class GlobalsRegistry : Pinned {
public:
    using ThreadQueue = MultiSourceQueue<ObjHeader**>::Producer;

    using Iterator = std::list<ObjHeader**>::iterator;

    static GlobalsRegistry& Instance() noexcept;

    void RegisterStorageForGlobal(mm::ThreadData* threadData, ObjHeader** location) noexcept;

    // Collect globals from thread corresponding to `threadData`. Thread must be waiting for GC.
    // Only one thread can call this method.
    void ProcessThread(mm::ThreadData* threadData) noexcept;

    // These must be called on the same thread as `ProcessThread` to avoid races.
    // TODO: Iteration over `globals_` will be slow, because it's `std::list` collected at different times from
    // different threads, and so the nodes are all over the memory. Use metrics to understand how
    // much of a problem is it.
    Iterator begin() noexcept { return globals_.begin(); }
    Iterator end() noexcept { return globals_.end(); }

private:
    friend class GlobalData;

    GlobalsRegistry();
    ~GlobalsRegistry();

    MultiSourceQueue<ObjHeader**> globals_;
};

} // namespace mm
} // namespace kotlin

#endif // RUNTIME_MM_GLOBALS_REGISTRY_H
