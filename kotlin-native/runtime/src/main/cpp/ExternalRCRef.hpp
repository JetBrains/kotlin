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

// An externally-reference-counted entity bound to a Kotlin object `obj_`.
// `ExternalRCRefImpl` is registered in `ExternalRCRefRegistry` and has the following properties:
// * `rc_ > 0` -> `obj_` is in global roots
// * `rc_ == 0` -> `obj_` eventually disappears from roots
// * `rc_ == disposedMarker` -> this is not unsafe to use and will be deleted at some point.
//
// Every `ExternalRCRefImpl*` is valid `kotlin.native.internal.ref.ExternalRCRef`.
// The reverse is not true: for permanent objects `kotlin.native.internal.ref.ExternalRCRef`
// is specially encoded.
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

// Object if the given kotlin.native.internal.ref.ExternalRCRef is permanent object, nullptr otherwise.
KRef externalRCRefAsPermanentObject(void* ref) noexcept;

// kotlin.native.internal.ref.ExternalRCRef for the given permanent object.
void* permanentObjectAsExternalRCRef(KRef obj) noexcept;

// TypeInfo of the given kotlin.native.internal.ref.ExternalRCRef.
const TypeInfo* externalRCRefType(void* ref) noexcept;

}
