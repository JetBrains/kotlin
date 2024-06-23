/*
 * Copyright 2010-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ObjectData.hpp"
#include "ThreadData.hpp"
#include "ObjectTraversal.hpp"

ALWAYS_INLINE void kotlin::gc::GC::ObjectData::incRefCounter(mm::ThreadData& thread, const char* reason) noexcept {
    uint32_t currentTid = thread.threadId();
    auto word = gcWord_.load(std::memory_order_relaxed);
    if (word.isRefCounted()) {
        if (word.ownerTid() == currentTid) {
            RuntimeLogDebug({kTagGC}, "inc(%p) %s %u->%u", this, reason, word.refCount(), word.refCount() + 1);
            auto desired = word.incCounter();
            gcWord_.compare_exchange_strong(word, desired, std::memory_order_relaxed);
            // if swapped: object remains local
            // if not swapped: object has escaped, nothing to do
        } else {
            // FIXME object can die here
            //       then we have two possibilities:
            //       either the object remains dead and the store will make it traced-dead
            //       or a new object is already allocated in this place, and the store will make it escaped
            gcWord_.store(internal::GCWord::traced(nullptr), std::memory_order_relaxed);
        }
    }
}

ALWAYS_INLINE void kotlin::gc::GC::ObjectData::decRefCounter(mm::ThreadData& thread, const char* reason) noexcept {
    uint32_t currentTid = thread.threadId();
    auto word = gcWord_.load(std::memory_order_relaxed);
    if (word.isRefCounted()) {
        if (word.ownerTid() == currentTid) {
            if (word.refCount() > 0) {
                RuntimeLogDebug({kTagGC}, "dec(%p) %s %u->%u", this, reason, word.refCount(), word.refCount() - 1);
                auto desired = word.decCounter();
                bool swapped = gcWord_.compare_exchange_strong(word, desired, std::memory_order_relaxed);
                if (swapped && desired.refCount() == 0) {
                    killObj(thread);
                }
            }
            // if not swapped: object has escaped, nothing to do
        } else {
            // FIXME object can die here
            //       then we have two possibilities:
            //       either the object remains dead and the store will make it traced-dead
            //       or a new object is already allocated in this place, and the store will make it escaped
            gcWord_.store(internal::GCWord::traced(nullptr), std::memory_order_relaxed);
        }
    }
}

NO_INLINE void kotlin::gc::GC::ObjectData::killObj(mm::ThreadData& thread) noexcept {
    thread.allocator().freeReferenceCounted(*this);

    traverseReferredObjects(alloc::objectForObjectData(*this), [](ObjHeader* referred) {
        if (referred->heap()) {
            decCounter(referred, "parent died");
        }
    });
}

ALWAYS_INLINE void kotlin::gc::GC::ObjectData::initToRC(kotlin::mm::ThreadData& thread) noexcept {
    RuntimeAssert(gcWord_.load(std::memory_order_relaxed).isTraced(), "");
    gcWord_.store(internal::GCWord::refCounted(0, thread.threadId()));
}
