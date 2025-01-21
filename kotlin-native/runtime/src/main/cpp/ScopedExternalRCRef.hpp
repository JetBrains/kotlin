/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "ExternalRCRef.hpp"
#include "RawPtr.hpp"
#include "Types.h"
#include "Utils.hpp"

namespace kotlin::mm {

// Scoped ExternalRCRef:
// - creates retained in constructor.
// - releases and dispose in destructor.
class ScopedExternalRCRef : private MoveOnly {
public:
    ScopedExternalRCRef() noexcept = default;
    ScopedExternalRCRef(nullptr_t) noexcept {}

    // Cast raw ref into a scoped reference.
    explicit ScopedExternalRCRef(RawExternalRCRef* raw) noexcept : raw_(raw) {}

    // Create new retained reference for `obj`.
    explicit ScopedExternalRCRef(KRef obj) noexcept : raw_(createRetainedExternalRCRef(obj)) {}

    ScopedExternalRCRef(ScopedExternalRCRef&&) noexcept = default;
    ScopedExternalRCRef& operator=(ScopedExternalRCRef&&) noexcept = default;

    ~ScopedExternalRCRef() {
        mm::releaseAndDisposeExternalRCRef(static_cast<mm::RawExternalRCRef*>(raw_));
    }

    // Cast into raw ref.
    explicit operator RawExternalRCRef*() const noexcept {
        return static_cast<RawExternalRCRef*>(raw_);
    }

    // Get the underlying object.
    // Always safe, because the object is guaranteed to be in the root set.
    [[nodiscard("expensive pure function")]] ObjHeader* operator*() const noexcept {
        return dereferenceExternalRCRef(static_cast<mm::RawExternalRCRef*>(raw_));
    }

private:
    raw_ptr<RawExternalRCRef> raw_;
};

} // namespace kotlin::mm
