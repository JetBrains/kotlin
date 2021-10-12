/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_UTILS_H
#define RUNTIME_UTILS_H

#include <array>
#include <new>
#include <type_traits>

#include "KAssert.h"

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

// A helper that scopley assings a value to a variable. The variable will
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

// An adapter that allows allocating STL containers in a preallocated buffer. Useful for stack allocations.
// TODO: May be make an arena allocator instead?
template <typename T, size_t Capacity>
class StackBuffer final : private Pinned {
    static_assert(std::is_trivially_default_constructible<T>::value);

public:
    class StackAllocator {
    public:
        using value_type = T;

        StackAllocator(const StackAllocator& other) = default;
        StackAllocator(StackAllocator&& other) noexcept = default;

        StackAllocator& operator=(const StackAllocator& other) = default;
        StackAllocator& operator=(StackAllocator&& other) noexcept = default;

        bool operator==(const StackAllocator& other) { return buffer_ == other.buffer_; }

        size_t max_size() noexcept { return buffer_->allocated_ ? 0 : Capacity; }

        T* allocate(size_t size) {
            auto result = buffer_->allocate(size);
            if (!result) {
                throw std::bad_array_new_length();
            }
            return result;
        }

        void deallocate(T* ptr, size_t count) noexcept {
            buffer_->deallocate(ptr, count);
        }

    private:
        explicit StackAllocator(StackBuffer<T, Capacity>* buffer) noexcept : buffer_(buffer) {}

        StackBuffer<T, Capacity>* buffer_;

        friend class StackBuffer;
    };

    StackBuffer() noexcept : allocated_(false) {}

    StackAllocator allocator() {
        return StackAllocator(this);
    }

private:
    T* allocate(size_t size) noexcept {
        if (allocated_ || size > Capacity) { return nullptr; }
        allocated_ = true;
        return buffer_.data();
    }

    void deallocate(T* ptr, size_t count) noexcept {
        RuntimeAssert(allocated_ && ptr == buffer_.data(),
                      "Expected ptr to be allocated in this StackBuffer. ptr: %p, buffer: %p, allocated: %s",
                      ptr, buffer_.data(), (allocated_) ? "true" : "false");
        allocated_ = false;
    }

    std::array<T, Capacity> buffer_;
    bool allocated_;
};

} // namespace kotlin

#endif // RUNTIME_UTILS_H
