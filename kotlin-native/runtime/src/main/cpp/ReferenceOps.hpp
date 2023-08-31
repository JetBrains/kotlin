/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "Memory.h"
#include "std_support/AtomicRef.hpp"

// Concurrent GC may cause conflicting unordered accesses to references in heap.
// C++ memory model declares that accesses have data race unless they are atomic.
// Thus TSAN would report such non-atomic accesses.
// TODO find out if compiler may take advantage of the theoretical UB here.
//
// However, in practice all the ptr-sized loads and stores are atomic on CPU-level
// even if they are not std::atomic.
// And as far as we aware,
// std::atomic operations are not optimized by LLVM even if they have relaxed memory order.
// So we don't want to compile every heap reference access into std::atomic access.
#define ALWAYS_ATOMIC_REFS __has_feature(thread_sanitizer)

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
#if ALWAYS_ATOMIC_REFS
        return loadAtomic(std::memory_order_relaxed);
#else
        return ref_;
#endif
    }

    ALWAYS_INLINE void store(ObjHeader* desired) noexcept {
#if ALWAYS_ATOMIC_REFS
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

    explicit RefAccessor(ObjHeader*& fieldRef) noexcept: direct_(fieldRef) {}

    explicit RefAccessor(ObjHeader** fieldPtr) noexcept: RefAccessor(*fieldPtr) {}

    RefAccessor(const RefAccessor& other) noexcept: direct_(other.direct_) {}

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

    ALWAYS_INLINE ObjHeader* operator=(ObjHeader* desired) noexcept {
        store(desired);
        return desired;
    }

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

OBJ_GETTER(weakRefReadBarrier, ObjHeader* weakReferee) noexcept;

}