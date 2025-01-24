/*
 * Copyright 2010-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <cstddef>
#include <cstdint>
#include <limits>

#include "Memory.h"
#include "RawPtr.hpp"
#include "Types.h"
#include "Utils.hpp"
#include "std_support/Atomic.hpp"

namespace kotlin::mm {

// TODO(KT-67741): Unify different ExternalRCRefs

class ExternalRCRefRegistry;

// `RawExternalRCRef*` is `kotlin.native.internal.ref.ExternalRCRef`.
//
// See also: ExternalRCRefImpl, OwningExternalRCRef, WeakExternalRCRef.
struct RawExternalRCRef;

// An externally-reference-counted entity bound to a Kotlin object `obj_`.
// `ExternalRCRefImpl` is registered in `ExternalRCRefRegistry` and has the following properties:
// * `rc_ > 0` -> `obj_` is in global roots
// * `rc_ == 0` -> `obj_` eventually disappears from roots
// * `rc_ == disposedMarker` -> this is not unsafe to use and will be deleted at some point.
//
// Every `ExternalRCRefImpl*` is `RawExternalRCRef*`.
// The reverse is not true: for permanent objects `RawExternalRCRef*` is specially encoded.
//
// See also: RawExternalRCRef, OwningExternalRCRef, WeakExternalRCRef.
class ExternalRCRefImpl : private Pinned {
public:
    using Rc = int32_t;
    inline static constexpr Rc disposedMarker = std::numeric_limits<Rc>::min();
    static_assert(disposedMarker < 0, "disposedMarker must be an impossible Rc value");

    ExternalRCRefImpl() noexcept = default;

    // The constructor is used internally. Create the reference by calling `create()`.
    ExternalRCRefImpl(ExternalRCRefRegistry& registry, KRef obj, Rc rc) noexcept;

    ~ExternalRCRefImpl();

    // Create new reference for `obj` with initial reference counter `rc`.
    // May only be called in the runnable state.
    [[nodiscard("must be manually disposed")]] static ExternalRCRefImpl& create(KRef obj, Rc rc) noexcept;

    // Dispose `this`. It's unsafe to use `this` after this call.
    void dispose() noexcept;

    // Get the underlying Kotlin object.
    // The result is safe to use only when `rc_ > 0`
    // May only be called in the runnable state.
    [[nodiscard("expensive pure function")]] KRef ref() const noexcept;

    // Get the type of the underlying Kotlin object.
    // Can only be called when `rc_ > 0`, or when `rc_ = 0`, but it is known that the underlying
    // Kotiln object is kept in the roots in some other way (e.g. on the stack)
    [[nodiscard("expensive pure function")]] const TypeInfo* typeInfo() const noexcept;

    // Try to get the underlying Kotlin object.
    // If the object is not yet collected by the GC, return that object.
    // Otherwise, returns `nullptr`.
    // May only be called in the runnable state.
    OBJ_GETTER0(tryRef) noexcept;

    // Increment `rc_`. Can only be called when `rc_ > 0`, or
    // when `rc_ = 0`, but it is known that the underlying Kotlin object
    // is kept in the roots in some other way (e.g. on the stack)
    void retainRef() noexcept;

    // Decrement `rc_`. Can only be called when `rc_ > 0`.
    void releaseRef() noexcept;

    // Convert to `RawExternalRCRef*`. Always a valid operation.
    RawExternalRCRef* toRaw() noexcept { return reinterpret_cast<RawExternalRCRef*>(this); }

    // Convert to `const RawExternalRCRef*`. Always a valid operation.
    const RawExternalRCRef* toRaw() const noexcept { return reinterpret_cast<const RawExternalRCRef*>(this); }

    // Convert from `RawExternalRCRef*`. Only valid when `ref` does not point to a permanent object.
    static ExternalRCRefImpl* fromRaw(RawExternalRCRef* ref) noexcept {
        return const_cast<ExternalRCRefImpl*>(fromRaw(static_cast<const RawExternalRCRef*>(ref)));
    }

    // Convert from `RawExternalRCRef*`. Only valid when `ref` does not point to a permanent object.
    static const ExternalRCRefImpl* fromRaw(const RawExternalRCRef* ref) noexcept;

private:
    friend class ExternalRCRefRegistry;
    friend class ExternalRCRefRegistryTest;

    // obj_ is set in the constructor and can be nulled out only by the
    // GC thread when processing weaks. It's the responsibility of the
    // GC to make sure nulling out obj_ is synchronized with mutators:
    // * via STW: nulling obj_ only happens when mutators are paused.
    // * via weak read barriers: when GC enters a weak processing phase,
    //   it enables weak read barriers which do not read obj_ if obj_ will
    //   be nulled, and disable the barriers when the phase is completed.
    //   Synchronization between GC and mutators happens via enabling/disabling
    //   the barriers.
    // TODO: Try to handle it atomically only when the GC is in progress.
    std_support::atomic_ref<KRef> objAtomic() noexcept { return std_support::atomic_ref{obj_}; }
    std_support::atomic_ref<const KRef> objAtomic() const noexcept { return std_support::atomic_ref{obj_}; }
    KRef obj_ = nullptr;

    // Only ever updated using relaxed memory ordering. Any synchronization
    // with nextRoot_ is achieved via acquire-release of nextRoot_.
    std::atomic<Rc> rc_ = disposedMarker; // After dispose() will be disposedMarker.
    // Singly linked lock free list. Using acquire-release throughout.
    std::atomic<ExternalRCRefImpl*> nextRoot_ = nullptr;
};

// Object if the given `ref` points to permanent object, nullptr otherwise.
KRef externalRCRefAsPermanentObject(const RawExternalRCRef* ref) noexcept;

// Create `RawExternalRCRef*` for the given permanent object.
RawExternalRCRef* permanentObjectAsExternalRCRef(KRef obj) noexcept;

// Create `RawExternalRCRef*` pointing to `obj`. The initial reference count will be 1.
// May only be called in the runnable state.
inline RawExternalRCRef* createRetainedExternalRCRef(KRef obj) noexcept {
    AssertThreadState(ThreadState::kRunnable);
    if (!obj) return nullptr;
    if (obj->permanent()) return permanentObjectAsExternalRCRef(obj);
    return ExternalRCRefImpl::create(obj, 1).toRaw();
}

// Create `RawExternalRCRef*` pointing to `obj`. The initial reference count will be 0.
// May only be called in the runnable state.
inline RawExternalRCRef* createUnretainedExternalRCRef(KRef obj) noexcept {
    AssertThreadState(ThreadState::kRunnable);
    if (!obj) return nullptr;
    if (obj->permanent()) return permanentObjectAsExternalRCRef(obj);
    return ExternalRCRefImpl::create(obj, 0).toRaw();
}

// Dispose `RawExternalRCRef*`. `ref` becomes invalid to use after this operation.
// May only be called when the reference count is 0.
// Can be called in any state.
inline void disposeExternalRCRef(RawExternalRCRef* ref) noexcept {
    if (!ref || externalRCRefAsPermanentObject(ref)) return;
    ExternalRCRefImpl::fromRaw(ref)->dispose();
}

// Return object that `RawExternalRCRef*` points to.
// The result is only safe to use when the reference count is >0 or if the object is
// known to be in the roots in some other way (e.g. on stack).
// May only be called in the runnable state.
inline KRef dereferenceExternalRCRef(const RawExternalRCRef* ref) noexcept {
    AssertThreadState(ThreadState::kRunnable);
    if (!ref) return nullptr;
    if (auto obj = externalRCRefAsPermanentObject(ref)) return obj;
    return ExternalRCRefImpl::fromRaw(ref)->ref();
}

// Return type of the object that `RawExternalRCRef*` points to.
// Safe to call only when the reference count is >0 or if the object is
// known to be in the roots in some other way (e.g. on stack).
// Can be called in any state.
inline const TypeInfo* typeOfExternalRCRef(const RawExternalRCRef* ref) noexcept {
    if (!ref) return nullptr;
    if (auto obj = externalRCRefAsPermanentObject(ref)) return obj->type_info();
    return ExternalRCRefImpl::fromRaw(ref)->typeInfo();
}

// Increment the reference count.
// May only be called when the reference count is >0 or if the object is
// known to be in the roots in some other way (e.g. on stack).
// Can be called in any state.
inline void retainExternalRCRef(RawExternalRCRef* ref) noexcept {
    if (!ref || externalRCRefAsPermanentObject(ref)) return;
    ExternalRCRefImpl::fromRaw(ref)->retainRef();
}

// Decrement the reference count.
// May only be called when the reference count is >0.
// Can be called in any state.
inline void releaseExternalRCRef(RawExternalRCRef* ref) noexcept {
    if (!ref || externalRCRefAsPermanentObject(ref)) return;
    ExternalRCRefImpl::fromRaw(ref)->releaseRef();
}

// Safely dereference `RawExternalRCRef*`: if the underlying object is not yet collected by the GC,
// returns it. Otherwise returns `nullptr`.
// May only be called in the runnable state.
inline OBJ_GETTER(tryRefExternalRCRef, RawExternalRCRef* ref) noexcept {
    AssertThreadState(ThreadState::kRunnable);
    if (!ref) RETURN_OBJ(nullptr);
    if (auto obj = externalRCRefAsPermanentObject(ref)) RETURN_OBJ(obj);
    RETURN_RESULT_OF0(ExternalRCRefImpl::fromRaw(ref)->tryRef);
}

namespace internal {

struct OwningTraits {
    static RawExternalRCRef* create(KRef obj) { return createRetainedExternalRCRef(obj); }

    static void destroy(RawExternalRCRef* ref) {
        releaseExternalRCRef(ref);
        disposeExternalRCRef(ref);
    }
};

struct WeakTraits {
    static RawExternalRCRef* create(KRef obj) { return createUnretainedExternalRCRef(obj); }

    static void destroy(RawExternalRCRef* ref) { disposeExternalRCRef(ref); }
};

} // namespace internal

// Smart pointer wrapper around `RawExternalRCRef*`.
// Use `OwningExternalRCRef` when it should model owning pointer.
// Use `WeakExternalRCRef` when it should model weak pointer.
template <typename Traits = internal::OwningTraits>
class ExternalRCRef : private MoveOnly {
public:
    ExternalRCRef() noexcept = default;
    ExternalRCRef(std::nullptr_t) noexcept {}

    // Adopt `RawExternalRCRef*` without affecting reference count.
    explicit ExternalRCRef(RawExternalRCRef* raw) noexcept : raw_(raw) {}

    // Create new `ExternalRCRef` pointing to `obj`.
    // `OwningExternalRCRef` creates retained reference.
    // `WeakExternalRCRef` creates unretained reference.
    explicit ExternalRCRef(KRef obj) noexcept : raw_(Traits::create(obj)) {}

    ExternalRCRef(ExternalRCRef&& rhs) noexcept = default;

    // Dispose the current reference.
    // `OwningExternalRCRef` additionally releases the reference first.
    ~ExternalRCRef() { Traits::destroy(get()); }

    friend void swap(ExternalRCRef& lhs, ExternalRCRef& rhs) noexcept { return lhs.raw_.swap(rhs.raw_); }

    ExternalRCRef& operator=(ExternalRCRef&& rhs) noexcept {
        ExternalRCRef tmp(std::move(rhs));
        swap(*this, tmp);
        return *this;
    }

    RawExternalRCRef* get() const noexcept { return static_cast<RawExternalRCRef*>(raw_); }

    // Detach from current reference without disposing or altering reference count.
    RawExternalRCRef* release() noexcept {
        auto result = std::move(raw_);
        return static_cast<RawExternalRCRef*>(result);
    }

    void reset() noexcept { *this = nullptr; }

    void reset(RawExternalRCRef* raw) noexcept { *this = ExternalRCRef(raw); }

    void reset(KRef obj) noexcept { *this = ExternalRCRef(obj); }

    // Return the underlying object.
    // The result is only safe to use, when reference count is >0, or there is a guarantee
    // that the object is in roots in some other way (e.g. on stack)
    KRef ref() const noexcept { return dereferenceExternalRCRef(get()); }

    KRef operator*() const noexcept { return ref(); }
    KRef* operator->() const noexcept { return &*this; }

    // Return the underlying object.
    // May only be called when reference count is >0, or there is a guarantee
    // that the object is in roots in some other way (e.g. on stack)
    const TypeInfo* typeInfo() const noexcept { return typeOfExternalRCRef(get()); }

    // Safely return the underlying object.
    // If the object is not collected by the GC, return it.
    // Otherwise, returns nullptr
    OBJ_GETTER0(tryRef) const noexcept { RETURN_RESULT_OF(tryRefExternalRCRef, get()); }

private:
    RawExternalRCRef* raw() noexcept { return static_cast<RawExternalRCRef*>(raw_); }

    raw_ptr<RawExternalRCRef> raw_;
};

using OwningExternalRCRef = ExternalRCRef<internal::OwningTraits>;
using WeakExternalRCRef = ExternalRCRef<internal::WeakTraits>;

} // namespace kotlin::mm
