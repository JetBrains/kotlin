/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_UTILS_H
#define RUNTIME_UTILS_H

#include <cstddef>

#include <type_traits>

#include "std_support/Span.hpp"

namespace kotlin {

// A helper for implementing classes with disabled copy constructor and copy assignment.
// Usage:
// class A: private MoveOnly {
//     ...
// };
// Prefer private inheritance to discourage casting instances of `A` to instances
// of `MoveOnly`.
class MoveOnly {
    // Hide constructors, assignments and destructor, to discourage operating on an instance of `MoveOnly`.
protected:
    MoveOnly() noexcept = default;
    MoveOnly(const MoveOnly&) = delete;
    MoveOnly(MoveOnly&&) noexcept = default;

    MoveOnly& operator=(const MoveOnly&) = delete;
    MoveOnly& operator=(MoveOnly&&) noexcept = default;

    // Not virtual by design. Since this class hides this destructor, no one can destroy an
    // instance of `MoveOnly` directly, so this destructor is never called in a virtual manner.
    ~MoveOnly() = default;
};

// A helper for implementing classes with disabled copy and move constructors, and copy and move assignments.
// Usage:
// class A: private Pinned {
//     ...
// };
// Prefer private inheritance to discourage casting instances of `A` to instances
// of `Pinned`.
class Pinned {
    // Hide constructors, assignments and destructor, to discourage operating on an instance of `Pinned`.
protected:
    Pinned() noexcept = default;
    Pinned(const Pinned&) = delete;
    Pinned(Pinned&&) = delete;

    Pinned& operator=(const Pinned&) = delete;
    Pinned& operator=(Pinned&&) = delete;

    // Not virtual by design. Since this class hides this destructor, no one can destroy an
    // instance of `Pinned` directly, so this destructor is never called in a virtual manner.
    ~Pinned() = default;
};

// A helper that executes the action provided upon destruction of the ScopeGuard instance.
template<typename FinalAction>
class ScopeGuard final : private Pinned {
public:
    template<typename InitAction>
    ScopeGuard(InitAction initAction, FinalAction finalAction) noexcept : finalAction_(finalAction) {
        initAction();
    }
    ScopeGuard(FinalAction finalAction) noexcept : finalAction_(finalAction) {}
    ~ScopeGuard() noexcept {
        finalAction_();
    }
private:
    FinalAction finalAction_;
};

// A helper that scopley assigns a value to a variable. The variable will
// be set to its original value upon destruction of the AutoReset instance.
// Note that an AutoReset instance must have a shorter lifetime than
// the variable it works with to avoid invalid memory access.
template<typename T1, typename T2>
class AutoReset final : private Pinned {
    static_assert(std::is_assignable<T1&, T2>::value);

public:
    AutoReset(T1* variable, T2 value) : variable_(variable), oldValue_(*variable) {
        *variable_ = value;
    }

    ~AutoReset() {
        *variable_ = oldValue_;
    }

private:
    T1* variable_;
    T2 oldValue_;
};

size_t CombineHash(size_t seed, size_t value);

#define ownerOf(type, field, ref) *reinterpret_cast<type*>(reinterpret_cast<char*>(&ref) - offsetof(type, field))

// Returns `true` if the entire `span` is zeroed.
bool isZeroed(std_support::span<uint8_t> span) noexcept;

} // namespace kotlin

#endif // RUNTIME_UTILS_H
