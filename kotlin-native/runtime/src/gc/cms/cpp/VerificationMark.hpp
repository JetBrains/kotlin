/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "MarkAndSweepUtils.hpp"

namespace kotlin::gc {

struct VerificationMarkTraits {
    using MarkQueue = std_support::vector<GC::ObjectData*>;

    static void clear(MarkQueue& queue) noexcept { queue.clear(); }

    static bool tryMark(ObjHeader* object) noexcept {
        auto& objectData = objectDataForObject(object);
        return objectData.tryMark();
    }

    static ObjHeader* tryDequeue(MarkQueue& queue) noexcept {
        if (queue.empty()) return nullptr;
        auto top = queue.back();
        queue.pop_back();
        return objectForObjectData(*top);
    }

    static bool tryEnqueue(MarkQueue& queue, ObjHeader* object) noexcept {
        auto& objectData = objectDataForObject(object);
        if (objectData.verified()) return false;
        RuntimeAssert(objectData.marked(), "Verification mark found an unmarked object %p", object);
        objectData.remarkVerified();
        queue.push_back(&objectDataForObject(object));
        return true;
    }

    // FIXME copy&pasted
    static void processInMark(MarkQueue& markQueue, ObjHeader* object) noexcept {
        // just checking
        auto process = object->type_info()->processObjectInMark;
        RuntimeAssert(process != nullptr, "Got null processObjectInMark for object %p", object);

        if (object->type_info() == theArrayTypeInfo) {
            internal::processArrayInMark<VerificationMarkTraits>(static_cast<void*>(&markQueue), object->array());
        } else {
            internal::processObjectInMark<VerificationMarkTraits>(static_cast<void*>(&markQueue), object);
        }
    }
};

// must be called inside STW
inline void checkAllAliveObjectsMarked() {
    if (compiler::runtimeAssertsMode() == compiler::RuntimeAssertsMode::kIgnore) return;

    auto fakeHandle = GCHandle::invalid();
    VerificationMarkTraits::MarkQueue markQueue;
    gc::collectRootSet<VerificationMarkTraits>(fakeHandle, markQueue, [](mm::ThreadData&) { return true; });
    gc::Mark<VerificationMarkTraits>(fakeHandle, markQueue);
}

// TODO remove?
// must be called inside STW
template <typename Heap>
inline void checkMarkClosureComplete(Heap& heap) {
    if (compiler::runtimeAssertsMode() == compiler::RuntimeAssertsMode::kIgnore) return;

#ifndef CUSTOM_ALLOCATOR
    for (auto objRef: heap) {
        auto obj = objRef.GetObjHeader();
        auto& objData = objRef.ObjectData();
        if (objData.marked()) {
            traverseReferredObjects(obj, [obj](ObjHeader* field) {
                if (field->heap()) {
                    auto& fieldObjData = gc::ObjectFactory::NodeRef::From(field).ObjectData();
                    RuntimeAssert(fieldObjData.marked(), "Field %p of an alive obj %p must be alive", field, obj);
                }
            });
        }
    }
#endif
}

}
