/*
 * Copyright 2010-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <cstdint>
#include <limits>

#include "Memory.h"
#include "Types.h"
#include "std_support/Atomic.hpp"

namespace kotlin::mm {

// TODO(KT-67741): Unify different ExternalRCRefs

class ExternalRCRefRegistry;

// `RawExternalRCRef*` is `kotlin.native.internal.ref.ExternalRCRef`.
// NOTE: operations on `RawExternalRCRef*` support `nullptr`, while
//       operations on `kotlin.native.internal.ref.ExternalRCRef` do not support `null`.
//
// See also: ExternalRCRefImpl.
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
// See also: RawExternalRCRef.
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
    [[nodiscard("must be manually disposed")]] static ExternalRCRefImpl& create(KRef obj, Rc rc) noexcept;

    // Dispose `this`. It's unsafe to use `this` after this call.
    void dispose() noexcept;

    // Get the underlying Kotlin object.
    // The result is safe to use only when `rc_ > 0`
    [[nodiscard("expensive pure function")]] KRef ref() const noexcept;

    // Try to get the underlying Kotlin object.
    // If the object is not yet collected by the GC, return that object.
    // Otherwise, returns `nullptr`.
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
// Can be called in any state.
inline KRef dereferenceExternalRCRef(const RawExternalRCRef* ref) noexcept {
    if (!ref) return nullptr;
    if (auto obj = externalRCRefAsPermanentObject(ref)) return obj;
    return ExternalRCRefImpl::fromRaw(ref)->ref();
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

}
