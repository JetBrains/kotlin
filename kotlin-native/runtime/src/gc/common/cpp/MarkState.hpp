/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

#pragma once

#include <utility>

namespace kotlin {
namespace gc {

template <typename Traits>
struct MarkState {
    /**
     * This is the mark queue for proper heap objects (ObjHeader::heapNotLocal == true).
     */
    typename Traits::MarkQueue globalQueue;

    /**
     * This is the mark queue for local objects (ObjHeader::local == true).
     *
     * They reside in the heap, but can refer to stack objects, and therefore should be scanned in STW.
     * Otherwise the following might happen in CMS:
     * 1. Such a local object is added to the mark queue.
     * 2. The mutator leaves the corresponding stack frame, invalidating the stack object.
     *    Technically, the local object is already unreachable, but it was added to the queue before that.
     * 3. `Mark` scans the local object. It encounters the reference to the removed stack object,
     *     tries to dereference it and examine its TypeInfo, which leads to a crash.
     *
     * On the other hand, local objects can refer to other local objects that are not roots.
     * Therefore, a transitive closure with this queue is necessary. See KT-75861.
     *
     * To address both problems, this local queue exists: local objects are added to it instead of the global queue,
     * and the local queue is processed as the last step in thread root scanning during STW.
     *
     * Local objects can be referred only from other local or stack objects or stack roots.
     * There can't be references to local objects from `heapNotLocal` objects.
     * That's why objects are enqueued here only during thread stack scanning in STW.
     */
    typename Traits::LocalMarkQueue localQueue;

    template <typename... Args>
    explicit MarkState(Args&&... args) : globalQueue(std::forward<Args>(args)...), localQueue() {}
};

} // namespace gc
} // namespace kotlin