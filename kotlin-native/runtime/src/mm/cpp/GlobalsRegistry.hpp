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
    class ThreadQueue : public MultiSourceQueue<ObjHeader**>::Producer {
    public:
        explicit ThreadQueue(GlobalsRegistry& registry) : Producer(registry.globals_) {}
        // Do not add fields as this is just a wrapper and Producer does not have virtual destructor.
    };

    using Iterable = MultiSourceQueue<ObjHeader**>::Iterable;

    using Iterator = MultiSourceQueue<ObjHeader**>::Iterator;

    static GlobalsRegistry& Instance() noexcept;

    void RegisterStorageForGlobal(mm::ThreadData* threadData, ObjHeader** location) noexcept;

    // Collect globals from thread corresponding to `threadData`. Must be called by the thread
    // when it's asked by GC to stop.
    void ProcessThread(mm::ThreadData* threadData) noexcept;

    // Lock registry for safe iteration.
    // TODO: Iteration over `globals_` will be slow, because it's `KStdList` collected at different times from
    // different threads, and so the nodes are all over the memory. Use metrics to understand how
    // much of a problem is it.
    Iterable Iter() noexcept { return globals_.Iter(); }

private:
    friend class GlobalData;

    GlobalsRegistry();
    ~GlobalsRegistry();

    // TODO: Add-only MultiSourceQueue can be made more efficient. Measure, if it's a problem.
    MultiSourceQueue<ObjHeader**> globals_;
};

} // namespace mm
} // namespace kotlin

#endif // RUNTIME_MM_GLOBALS_REGISTRY_H
