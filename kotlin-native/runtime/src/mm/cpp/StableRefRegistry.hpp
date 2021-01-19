/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_MM_STABLE_REF_REGISTRY_H
#define RUNTIME_MM_STABLE_REF_REGISTRY_H

#include "Memory.h"
#include "MultiSourceQueue.hpp"
#include "ThreadRegistry.hpp"

namespace kotlin {
namespace mm {

// Registry for all objects that have references outside of Kotlin.
class StableRefRegistry : Pinned {
public:
    class ThreadQueue : public MultiSourceQueue<ObjHeader*>::Producer {
    public:
        explicit ThreadQueue(StableRefRegistry& registry) : Producer(registry.stableRefs_) {}
        // Do not add fields as this is just a wrapper and Producer does not have virtual destructor.
    };

    using Iterable = MultiSourceQueue<ObjHeader*>::Iterable;
    using Iterator = MultiSourceQueue<ObjHeader*>::Iterator;
    using Node = MultiSourceQueue<ObjHeader*>::Node;

    static StableRefRegistry& Instance() noexcept;

    Node* RegisterStableRef(mm::ThreadData* threadData, ObjHeader* object) noexcept;

    void UnregisterStableRef(mm::ThreadData* threadData, Node* node) noexcept;

    // Collect stable references from thread corresponding to `threadData`. Must be called by the thread
    // when it's asked by GC to stop.
    void ProcessThread(mm::ThreadData* threadData) noexcept;

    // Lock registry and apply deletions. Should be called on GC thread after all threads have published, and before `Iter`.
    void ProcessDeletions() noexcept;

    // Lock registry for safe iteration.
    // TODO: Iteration over `stableRefs_` will be slow, because it's `KStdList` collected at different times from
    // different threads, and so the nodes are all over the memory. Use metrics to understand how
    // much of a problem is it.
    Iterable Iter() noexcept { return stableRefs_.Iter(); }

private:
    friend class GlobalData;

    StableRefRegistry();
    ~StableRefRegistry();

    // Current approach optimizes for creating and disposing of stable refs:
    // * creation just enqueues ref, disposing either queues or deletes the ref immediately (if it still resides in the current queue).
    // * when thread is stopped, it'll scan through the local queue (to mark that refs no longer reside in it) and push creation and
    //   deletion queues to the global registry.
    // * during marking GC will have to `ProcessDeletions` to actually delete the refs that were enqueued for deletion.
    // So, we sacrifice memory (to keep deleted queues) and marking time (to process these queues) to improve creation and disposal times.
    //
    // Other alternatives:
    // * Use a single global collection (e.g. lock free doubly linked list).
    // * Sacrifice disposal time to try to delete as early as possible (e.g. post directly into owning producer, so it processes deletions
    //   before posting queue to the global registry)
    //
    // TODO: Measure to understand, if this approach is problematic.
    MultiSourceQueue<ObjHeader*> stableRefs_;
};

} // namespace mm
} // namespace kotlin

#endif // RUNTIME_MM_STABLE_REF_REGISTRY_H
