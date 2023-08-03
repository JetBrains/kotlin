#pragma once

#include <atomic>
#include <cstddef>

#include "Allocator.hpp"
#include "IntrusiveList.hpp"
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


} // namespace kotlin::gc::mark