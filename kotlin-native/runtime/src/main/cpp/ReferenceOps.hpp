/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "Memory.h"
#include "std_support/AtomicRef.hpp"

#if __has_feature(thread_sanitizer)
#include <sanitizer/tsan_interface.h>
#endif

// C++ memory model is in some sence stricter than the memmory model of real target CPUs.
// For example all the ptr-sized memory accesses on intel x86 and arm CPUs are atomic.
// Another case is the release-consume memory ordering, which can be achieved without additional memory fences on consume.
//
// However, LLVM often fails to properly optimize atomic operations.
// So we have to allow some imeplementation-defined UB here.
//
// Under this flag all tha operations with references in the kotlin heap
// are implemented in complete complience with C++ memory model.
#define STRICT_ATOMICS_IN_HEAP __has_feature(thread_sanitizer)

namespace kotlin::mm {

// TODO: Make sure these operations work with any kind of thread stopping: safepoints and signals.

// TODO: Consider adding some kind of an `Object` type (that wraps `ObjHeader*`) which
//       will have these operations for a friendlier API.

/**
 * Represents direct low-level operations on Koltin references.
 * No GC barriers are inserted. Should be used with care!
 */
class DirectRefAccessor {
public:
    DirectRefAccessor() = delete;
    DirectRefAccessor& operator=(const DirectRefAccessor&) = delete;

    explicit DirectRefAccessor(ObjHeader*& fieldRef) noexcept : ref_(fieldRef) {}
    explicit DirectRefAccessor(ObjHeader** fieldPtr) noexcept : DirectRefAccessor(*fieldPtr) {}
    DirectRefAccessor(const DirectRefAccessor& other) noexcept : DirectRefAccessor(other.ref_) {}

    ObjHeader** location() const noexcept { return &ref_; }

    ALWAYS_INLINE operator ObjHeader*() const noexcept { return load(); }
    ALWAYS_INLINE ObjHeader* operator=(ObjHeader* desired) noexcept { store(desired); return desired; }

    ALWAYS_INLINE ObjHeader* load() const noexcept {
#if STRICT_ATOMICS_IN_HEAP
        // Consume stores in the object, that were released on the object's allocation
        // See `ObjectOps.cpp`
        auto loaded = loadAtomic(std::memory_order_consume);
#if __has_feature(thread_sanitizer)
        // The stores were released by an atomic_thread_fence, TSAN doesn't support fences.
        __tsan_acquire(loaded);
#endif
        return loaded;
#else
        return ref_;
#endif
    }

    ALWAYS_INLINE void store(ObjHeader* desired) noexcept {
#if STRICT_ATOMICS_IN_HEAP
        storeAtomic(desired, std::memory_order_relaxed);
#else
        ref_ = desired;
#endif
    }

    ALWAYS_INLINE auto atomic() noexcept {
        return std_support::atomic_ref<ObjHeader*>{ref_};
    }
    ALWAYS_INLINE auto atomic() const noexcept {
        return std_support::atomic_ref<ObjHeader*>{ref_};
    }

    ALWAYS_INLINE ObjHeader* loadAtomic(std::memory_order order) const noexcept {
        return atomic().load(order);
    }
    ALWAYS_INLINE void storeAtomic(ObjHeader* desired, std::memory_order order) noexcept {
        atomic().store(desired, order);
    }
    ALWAYS_INLINE ObjHeader* exchange(ObjHeader* desired, std::memory_order order) noexcept {
        return atomic().exchange(desired, order);
    }
    ALWAYS_INLINE bool compareAndExchange(ObjHeader*& expected, ObjHeader* desired, std::memory_order order) noexcept {
        return atomic().compare_exchange_strong(expected, desired, order);
    }

private:
    ObjHeader*& ref_;
};

/**
 * Represents Koltin-level operations on Koltin references.
 * With all the necessary GC barriers etc.
 * Prefer using aliases below.
 */
template<bool kOnStack>
class RefAccessor {
public:
    RefAccessor() = delete;
    RefAccessor& operator=(const RefAccessor&) = delete;

    explicit RefAccessor(ObjHeader*& fieldRef) noexcept : direct_(fieldRef) {}
    explicit RefAccessor(ObjHeader** fieldPtr) noexcept : RefAccessor(*fieldPtr) {}
    RefAccessor(const RefAccessor& other) noexcept : direct_(other.direct_) {}

    DirectRefAccessor direct() const noexcept { return direct_; }

    void beforeLoad() noexcept;
    void afterLoad() noexcept;
    void beforeStore(ObjHeader* value) noexcept;
    void afterStore(ObjHeader* value) noexcept;

    ALWAYS_INLINE operator ObjHeader*() noexcept { return load(); }

    ALWAYS_INLINE ObjHeader* load() noexcept {
        AssertThreadState(ThreadState::kRunnable);
        beforeLoad();
        auto result = direct_.load();
        afterLoad();
        return result;
    }

    ALWAYS_INLINE ObjHeader* loadAtomic(std::memory_order order) noexcept {
        AssertThreadState(ThreadState::kRunnable);
        beforeLoad();
        auto result = direct_.loadAtomic(order);
        afterLoad();
        return result;
    }

    ALWAYS_INLINE ObjHeader* operator=(ObjHeader* desired) noexcept { store(desired); return desired; }

    ALWAYS_INLINE void store(ObjHeader* desired) noexcept {
        AssertThreadState(ThreadState::kRunnable);
        beforeStore(desired);
        direct_.store(desired);
        afterStore(desired);
    }

    ALWAYS_INLINE void storeAtomic(ObjHeader* desired, std::memory_order order) noexcept {
        AssertThreadState(ThreadState::kRunnable);
        beforeStore(desired);
        direct_.storeAtomic(desired, order);
        afterStore(desired);
    }

    ALWAYS_INLINE ObjHeader* exchange(ObjHeader* desired, std::memory_order order) noexcept {
        AssertThreadState(ThreadState::kRunnable);
        beforeLoad();
        beforeStore(desired);
        auto result = direct_.exchange(desired, order);
        afterStore(desired);
        afterLoad();
        return result;
    }

    ALWAYS_INLINE bool compareAndExchange(ObjHeader*& expected, ObjHeader* desired, std::memory_order order) noexcept {
        AssertThreadState(ThreadState::kRunnable);
        beforeLoad();
        beforeStore(desired);
        bool result = direct_.compareAndExchange(expected, desired, order);
        afterStore(desired);
        afterLoad();
        return result;
    }

private:
    DirectRefAccessor direct_;
};

using RefFieldAccessor = RefAccessor<false>;
using GlobalRefAccessor = RefAccessor<false>;
using StackRefAccessor = RefAccessor<true>;

class RefField : private Pinned {
public:
    auto accessor() noexcept {
        return mm::RefFieldAccessor(value_);
    }
    auto direct() noexcept {
        return accessor().direct();
    }
    // FIXME probably most of the uses should instead use accessor
    auto ptr() noexcept {
        return direct().location();
    }

    // TODO consider adding other operations
    ObjHeader* operator=(ObjHeader* value) noexcept {
        accessor() = value;
        return value_;
    }

    bool operator==(const RefField& other) const noexcept {
        return value_ == other.value_;
    }

    bool operator!=(const RefField& other) const noexcept {
        return !operator==(other);
    }

private:
    ObjHeader* value_ = nullptr;
};

OBJ_GETTER(weakRefReadBarrier, std::atomic<ObjHeader*>& weakReferee) noexcept;

}
