/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_MM_EXTRA_OBJECT_DATA_REGISTRY_H
#define RUNTIME_MM_EXTRA_OBJECT_DATA_REGISTRY_H

#include "ExtraObjectData.hpp"
#include "Memory.h"
#include "MultiSourceQueue.hpp"
#include "ObjectAlloc.hpp"
#include "ThreadRegistry.hpp"

namespace kotlin {
namespace mm {

// Registry for extra data, attached to some kotlin objects: weak refs, associated objects, ...
class ExtraObjectDataFactory : Pinned {
    using Mutex = SpinLock<MutexThreadStateHandling::kIgnore>;
    using Queue = MultiSourceQueue<mm::ExtraObjectData, Mutex, ObjectPoolAllocator<mm::ExtraObjectData>>;
public:
    class ThreadQueue : public Queue::Producer {
    public:
        explicit ThreadQueue(ExtraObjectDataFactory& registry) : Producer(registry.extraObjects_) {}
        // Do not add fields as this is just a wrapper and Producer does not have virtual destructor.
    };

    using Iterable = Queue::Iterable;
    using Iterator = Queue::Iterator;

    ExtraObjectDataFactory();
    ~ExtraObjectDataFactory();

    static ExtraObjectDataFactory& Instance() noexcept;

    ExtraObjectData& CreateExtraObjectDataForObject(mm::ThreadData* threadData, ObjHeader* baseObject, const TypeInfo* info) noexcept;
    ExtraObjectData& CreateExtraObjectDataForObject(ThreadQueue& threadQueue, ObjHeader* baseObject, const TypeInfo* info) noexcept;

    void DestroyExtraObjectData(mm::ThreadData* threadData, ExtraObjectData& data) noexcept;
    void DestroyExtraObjectData(ThreadQueue& threadQueue, ExtraObjectData& data) noexcept;

    // Collect extra data objects from thread corresponding to `threadData`. Must be called by the thread
    // when it's asked by GC to stop.
    void ProcessThread(mm::ThreadData* threadData) noexcept;

    // Lock registry for safe iteration.
    Iterable LockForIter() noexcept { return extraObjects_.LockForIter(); }

    void ClearForTests() noexcept { extraObjects_.ClearForTests(); }

    size_t GetSizeUnsafe() noexcept { return extraObjects_.GetSizeUnsafe(); }
    size_t GetTotalObjectsSizeUnsafe() noexcept { return extraObjects_.GetSizeUnsafe() * sizeof(ExtraObjectData); }

    // requires LockForIter
    void EraseAndAdvance(Iterator &it) { extraObjects_.EraseAndAdvance(it); }

private:
    Queue extraObjects_;
};

} // namespace mm
} // namespace kotlin

#endif // RUNTIME_MM_EXTRA_OBJECT_DATA_REGISTRY_H
