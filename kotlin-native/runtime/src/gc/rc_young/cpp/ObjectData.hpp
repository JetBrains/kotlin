/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <atomic>
#include <type_traits>

#include "Allocator.hpp"
#include "GC.hpp"
#include "IntrusiveList.hpp"
#include "KAssert.h"

namespace kotlin::gc {

namespace internal {

class GCWord {
    using ValueType = uint64_t; // FIXME arm32?
    static constexpr auto kRCTag = static_cast<ValueType>(0x1);
    //static constexpr auto kLeakedValue = std::numeric_limits<ValueType>::max();
public:
    // RefCounted: [ownerTid: 32bit | count: 31bit | 1 ]
    // FIXME constexpr
    static GCWord refCounted(uint32_t refCount, uint32_t ownerTid) noexcept {
        //RuntimeAssert(refCount != 0, "");
        RuntimeAssert(refCount <= (~(static_cast<uint32_t>(1) << 31)), "");
        auto word = GCWord((static_cast<uint64_t>(ownerTid) << 32) | (refCount << 1) | kRCTag);
        RuntimeAssert(word.isRefCounted(), "");
        RuntimeAssert(word.refCount() == refCount, "");
        RuntimeAssert(word.ownerTid() == ownerTid, "");
        return word;
    }

    static GCWord traced(GC::ObjectData* next) noexcept {
        auto nextAsValue = reinterpret_cast<ValueType>(next);
        RuntimeAssert((nextAsValue & kRCTag) == 0, "Unexpected next value: %p", next);
        auto word = GCWord(nextAsValue);
        RuntimeAssert(word.isTraced(), "");
        RuntimeAssert(word.nextInMarkQueue() == next, "");
        return word;
    }

    // FIXME it is just traced(nullptr)
//    static GCWord leaked() noexcept {
//        auto word = GCWord(kLeakedValue);
//        RuntimeAssert(word.isLeaked(), "");
//        RuntimeAssert(word.isRefCounted(), "");
//        return word;
//    }

    // RCed

    constexpr bool isRefCounted() const noexcept {
        return (value_ & kRCTag) != 0;
    }

//    bool isLeaked() const noexcept {
//        return value_ == kLeakedValue;
//    }

    uint32_t refCount() const noexcept {
        RuntimeAssert(isRefCounted(), "");
        return (value_ & std::numeric_limits<uint32_t>::max()) >> 1;
    }

    uint32_t ownerTid() const noexcept {
        RuntimeAssert(isRefCounted(), "");
        return (value_ >> 32) & std::numeric_limits<uint32_t>::max();
    }

    [[nodiscard]] GCWord incCounter() const noexcept {
        RuntimeAssert(isRefCounted(), "");
        RuntimeAssert(refCount() >= 0, ""); // FIXME ?
        auto result = GCWord(value_ + 2);
        RuntimeAssert(result.refCount() == refCount() + 1, "");
        RuntimeAssert(result.ownerTid() == ownerTid(), "");
        return result;
    }

    [[nodiscard]] GCWord decCounter() const noexcept {
        RuntimeAssert(isRefCounted(), "");
        RuntimeAssert(refCount() > 0, ""); // FIXME ?
        auto result = GCWord(value_ - 2);
        RuntimeAssert(result.refCount() == refCount() - 1, "");
        RuntimeAssert(result.ownerTid() == ownerTid(), "");
        return result;
    }

    // traced

    constexpr bool isTraced() const noexcept {
        return !isRefCounted();
    }

    GC::ObjectData* nextInMarkQueue() const noexcept {
        RuntimeAssert(isTraced(), "");
        return reinterpret_cast<GC::ObjectData*>(value_);
    }

private:
    constexpr explicit GCWord(ValueType value) : value_(value) {}

    ValueType value_;
};

}

class GC::ObjectData {
    static constexpr intptr_t kNoQueueMark = 2;
public:
    bool isRCed() noexcept {
        return gcWord_.load(std::memory_order_relaxed).isRefCounted();
    }
    uint32_t refCount() noexcept {
        return gcWord_.load(std::memory_order_relaxed).refCount();
    }
    void initToRC(mm::ThreadData& thread) noexcept;

    void incRefCounter(mm::ThreadData& thread, const char* reason) noexcept;
    void decRefCounter(mm::ThreadData& thread, const char* reason) noexcept;
    void killObj(mm::ThreadData& thread) noexcept;

    void globalise() noexcept {
        auto word = gcWord_.load(std::memory_order_relaxed);
        while (true) {
            if (word.isTraced()) return;
            bool swapped = gcWord_.compare_exchange_weak(word, internal::GCWord::traced(nullptr));
            if (swapped) return;
        }
    }

    bool tryRecycle() noexcept {
        auto word = gcWord_.load(std::memory_order_relaxed);
        if (word.isRefCounted() && word.refCount() == 0) return true;
        return false;
    }

    bool tryMark() noexcept { return trySetNext(reinterpret_cast<ObjectData*>(kNoQueueMark)); }

    void markUncontended() noexcept {
        RuntimeFail("Not implemented yet");
//        RuntimeAssert(!marked(), "Must not be marked previously");
//        auto nextVal = reinterpret_cast<ObjectData*>(kNoQueueMark);
//        setNext(nextVal);
//        RuntimeAssert(next() == nextVal, "Non-atomic marking must not be contended");
    }

    bool marked() const noexcept { return next() != nullptr; }

    // FIXME doc: returns true iff obj is alive
    bool tryResetMark() noexcept {
        // OK if RCed from previous epoch, new epoch can not be meet here
        // FIXME will fail with allocs during concurrent mark/weaksweep
        if (next() == nullptr) return false;
        gcWord_.store(internal::GCWord::traced(nullptr), std::memory_order_relaxed);
        return true;
    }

private:
    friend struct DefaultIntrusiveForwardListTraits<ObjectData>;

    ObjectData* next() const noexcept {
        auto word = gcWord_.load(std::memory_order_relaxed);
        if (!word.isTraced()) return nullptr;
        return word.nextInMarkQueue();
    }

    void setNext(ObjectData* next) noexcept {
        RuntimeAssert(next, "next cannot be nullptr");
        RuntimeAssert(gcWord_.load(std::memory_order_relaxed).isTraced(), "");
        gcWord_.store(internal::GCWord::traced(next), std::memory_order_relaxed);
    }

    bool trySetNext(ObjectData* next) noexcept {
        auto desired = internal::GCWord::traced(next);
        RuntimeAssert(next, "next cannot be nullptr");
        auto word = gcWord_.load(std::memory_order_relaxed);
        while (true) {
            if (word.isTraced() && word.nextInMarkQueue() != nullptr) {
                return false;
            }
            bool swapped = gcWord_.compare_exchange_weak(word, desired, std::memory_order_relaxed);
            if (swapped) return true;
        }
    }

    std::atomic<internal::GCWord> gcWord_ = internal::GCWord::traced(nullptr); // TODO is it the best default?
};

static_assert(std::is_trivially_destructible_v<GC::ObjectData>);

} // namespace kotlin::gc
