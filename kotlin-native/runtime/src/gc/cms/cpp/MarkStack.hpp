#pragma once

#include <atomic>
#include <cstddef>

#include "Allocator.hpp"
#include "IntrusiveList.hpp"
#include "CooperativeIntrusiveList.hpp"
#include "ObjectFactory.hpp"
#include "Types.h"
#include "Utils.hpp"
#include "std_support/Memory.hpp"

namespace kotlin::gc::mark {

class ObjectData {
    struct ObjectFactoryTraits {
        using ObjectData = ObjectData;
        class Allocator;
    };
public:
    using ObjectFactory = mm::ObjectFactory<ObjectFactoryTraits>;

    bool tryMark() noexcept {
        return trySetNext(reinterpret_cast<ObjectData*>(1));
    }

    bool marked() const noexcept { return next() != nullptr; }

    bool tryResetMark() noexcept {
        if (next() == nullptr) return false;
        next_.store(nullptr, std::memory_order_relaxed);
        return true;
    }

    ObjHeader* objHeader() noexcept { // FIXME const
        return ObjectFactory::NodeRef::From(*this).GetObjHeader();
    }

private:
    friend struct DefaultIntrusiveForwardListTraits<ObjectData>;

    ObjectData* next() const noexcept { return next_.load(std::memory_order_relaxed); }
    void setNext(ObjectData* next) noexcept {
        RuntimeAssert(next, "next cannot be nullptr");
        next_.store(next, std::memory_order_relaxed);
    }
    bool trySetNext(ObjectData* next) noexcept {
        RuntimeAssert(next, "next cannot be nullptr");
        ObjectData* expected = nullptr;
        return next_.compare_exchange_strong(expected, next, std::memory_order_relaxed);
    }

    std::atomic<ObjectData*> next_ = nullptr;
};

class MarkTraits {
public:
    using MarkQueue = CooperativeIntrusiveList<ObjectData>;
    using ObjectFactory = ObjectData::ObjectFactory;

    static void clear(MarkQueue& queue) noexcept { queue.clearLocal(); }

    // DELETED static ObjHeader* tryDequeue(MarkQueue& queue) noexcept;

    static bool tryEnqueue(MarkQueue& queue, ObjHeader* object) noexcept {
        auto& objectData = ObjectFactory::NodeRef::From(object).ObjectData();
        return queue.tryPushLocal(objectData);
    }

    static bool tryMark(ObjHeader* object) noexcept {
        auto& objectData = ObjectFactory::NodeRef::From(object).ObjectData();
        return objectData.tryMark();
    }

    static void processInMark(MarkQueue& markQueue, ObjHeader* object) noexcept {
        auto process = object->type_info()->processObjectInMark;
        RuntimeAssert(process != nullptr, "Got null processObjectInMark for object %p", object);
        process(static_cast<void*>(&markQueue), object);
    }
};

} // namespace kotlin::gc::mark