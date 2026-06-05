/*
 * Copyright 2010-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_FLATTENING_ITERATOR_H
#define RUNTIME_FLATTENING_ITERATOR_H

#include <type_traits>
#include <utility>

namespace kotlin {

// Projection that returns its argument unchanged; use to flatten a range of ranges.
struct Identity {
    template <typename T>
    T& operator()(T& value) const noexcept {
        return value;
    }
};

// Projection that returns `pair.second`; use to flatten the values of an associative container.
struct SecondOfPair {
    template <typename Pair>
    auto& operator()(Pair& pair) const noexcept {
        return pair.second;
    }
};

// A forward iterator that lazily flattens a range whose elements expose inner ranges into a
// single sequence -- a C++17 stand-in for `std::views::join`.
//
// `OuterIterator` walks the outer range; `Project` maps an outer element to a reference to its
// inner range (defaults to `Identity` for a range of ranges; use `SecondOfPair` to flatten e.g.
// the values of a `std::unordered_map`). Dereferencing yields a reference to the inner element.
//
// `Project` must be a copy-assignable functor (a named struct, not a lambda, whose copy-assignment
// is deleted in C++17) so that the iterator itself stays copy-assignable -- which some callers rely
// on, e.g. when storing it in a `union`.
template <typename OuterIterator, typename Project = Identity>
class FlatteningIterator {
    using InnerRange = std::remove_reference_t<decltype(std::declval<Project>()(*std::declval<OuterIterator>()))>;
    using InnerIterator = decltype(std::declval<InnerRange&>().begin());

public:
    FlatteningIterator(OuterIterator current, OuterIterator end, Project project = Project()) noexcept :
        outerIt_(current), outerEnd_(end), project_(project) {
        if (outerIt_ != outerEnd_) {
            innerIt_ = project_(*outerIt_).begin();
            SkipExhausted();
        }
    }

    decltype(auto) operator*() const noexcept { return *innerIt_; }

    FlatteningIterator& operator++() noexcept {
        ++innerIt_;
        SkipExhausted();
        return *this;
    }

    bool operator==(const FlatteningIterator& rhs) const noexcept {
        if (outerIt_ != rhs.outerIt_) return false;
        // At the end the inner iterator is singular and must not be compared.
        if (outerIt_ == outerEnd_) return true;
        return innerIt_ == rhs.innerIt_;
    }
    bool operator!=(const FlatteningIterator& rhs) const noexcept { return !(*this == rhs); }

private:
    // Advance past inner ranges whose elements are exhausted, landing on the next element or the end.
    void SkipExhausted() noexcept {
        while (outerIt_ != outerEnd_ && innerIt_ == project_(*outerIt_).end()) {
            ++outerIt_;
            if (outerIt_ != outerEnd_) innerIt_ = project_(*outerIt_).begin();
        }
    }

    OuterIterator outerIt_;
    OuterIterator outerEnd_;
    InnerIterator innerIt_;
    Project project_;
};

} // namespace kotlin

#endif // RUNTIME_FLATTENING_ITERATOR_H
