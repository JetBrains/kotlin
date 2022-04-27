/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_CPP_SUPPORT_SPAN_H
#define RUNTIME_CPP_SUPPORT_SPAN_H

#include <array>
#include <iterator>
#include <limits>
#include <type_traits>

#include "KAssert.h"

namespace kotlin {
namespace std_support {

namespace internal {

template <typename SpanElementType, typename T>
inline constexpr bool ConvertibleForSpan = std::is_convertible_v<T (*)[], SpanElementType (*)[]>;

template <typename SpanElementType, typename T>
using EnableIfConvertibleForSpan = std::enable_if_t<ConvertibleForSpan<SpanElementType, T>, std::nullptr_t>;

} // namespace internal

inline constexpr std::size_t dynamic_extent = std::numeric_limits<std::size_t>::max();

template <typename T, std::size_t Extent = dynamic_extent>
class span {
public:
    using element_type = T;
    using value_type = std::remove_cv_t<T>;
    using size_type = std::size_t;
    using difference_type = std::ptrdiff_t;
    using pointer = T*;
    using const_pointer = const T*;
    using reference = T&;
    using const_reference = const T&;
    using iterator = T*;
    using reverse_iterator = std::reverse_iterator<iterator>;

    static constexpr std::size_t extent = Extent;

    template <std::size_t Size = Extent, std::enable_if_t<Size == 0, std::nullptr_t> = nullptr>
    constexpr span() noexcept : data_(nullptr) {}

    // TODO: Instead of U* it should just be a `contiguos_iterator` with a deduction guide.
    template <typename U, internal::EnableIfConvertibleForSpan<element_type, U> = nullptr>
    constexpr explicit span(U* first, size_type count) : data_(first) {
        RuntimeAssert(count == Extent, "Mismatched count=%zu, expected %zu", count, Extent);
    }

    // TODO: Instead of U* it should just be a `contiguos_iterator` with a deduction guide.
    template <typename U, internal::EnableIfConvertibleForSpan<element_type, U> = nullptr>
    constexpr explicit span(U* first, U* last) : data_(first) {
        auto count = std::distance(first, last);
        RuntimeAssert(count == Extent, "Mismatched count=%zu, expected %zu", count, Extent);
    }

    constexpr span(element_type (&arr)[Extent]) noexcept : data_(arr) {}

    template <typename U, internal::EnableIfConvertibleForSpan<element_type, U> = nullptr>
    constexpr span(std::array<U, Extent>& arr) noexcept : data_(arr.data()) {}

    template <typename U, internal::EnableIfConvertibleForSpan<element_type, const U> = nullptr>
    constexpr span(const std::array<U, Extent>& arr) noexcept : data_(arr.data()) {}

    // TODO: Constructor from a range with a deduction guide.

    template <typename U, internal::EnableIfConvertibleForSpan<element_type, U> = nullptr>
    constexpr span(const span<U, Extent>& source) noexcept : data_(source.data()) {}

    template <typename U, internal::EnableIfConvertibleForSpan<element_type, U> = nullptr>
    constexpr explicit span(const span<U, dynamic_extent>& source) noexcept : data_(source.data()) {
        RuntimeAssert(source.size() == Extent, "Mismatched count=%zu, expected %zu", source.size(), Extent);
    }

    constexpr span(const span& other) noexcept = default;

    constexpr span& operator=(const span& other) noexcept = default;

    constexpr iterator begin() const noexcept { return data_; }
    constexpr iterator end() const noexcept { return data_ + size(); }

    constexpr reverse_iterator rbegin() const noexcept { return std::reverse_iterator(end()); }
    constexpr reverse_iterator rend() const noexcept { return std::reverse_iterator(begin()); }

    constexpr reference front() const {
        RuntimeAssert(!empty(), "Calling front on an empty span");
        return *begin();
    }
    constexpr reference back() const {
        RuntimeAssert(!empty(), "Calling back on an empty span");
        return *(end() - 1);
    }
    constexpr reference operator[](size_type idx) const {
        RuntimeAssert(idx < size(), "Indexing at %zu on a span of size %zu", idx, size());
        return data()[idx];
    }

    constexpr pointer data() const noexcept { return data_; }

    constexpr size_type size() const noexcept { return Extent; }
    constexpr size_type size_bytes() const noexcept { return Extent * sizeof(element_type); }
    [[nodiscard]] constexpr bool empty() const noexcept { return Extent == 0; }

    template <std::size_t Count>
    constexpr span<element_type, Count> first() const {
        static_assert(Count <= Extent, "Count must be smaller than Extent");
        return span<element_type, Count>(data_, Count);
    }

    constexpr span<element_type, dynamic_extent> first(size_type count) const {
        RuntimeAssert(count <= size(), "count %zu must be smaller than size %zu", count, size());
        return span<element_type, dynamic_extent>(data_, count);
    }

    template <std::size_t Count>
    constexpr span<element_type, Count> last() const {
        static_assert(Count <= Extent, "Count must be smaller than Extent");
        return span<element_type, Count>(data_ + size() - Count, Count);
    }

    constexpr span<element_type, dynamic_extent> last(size_type count) const {
        RuntimeAssert(count <= size(), "count %zu must be smaller than size %zu", count, size());
        return span<element_type, dynamic_extent>(data_ + size() - count, count);
    }

    template <std::size_t Offset, std::size_t Count = dynamic_extent>
    constexpr span<element_type, Count != dynamic_extent ? Count : Extent - Offset> subspan() const {
        static_assert(Offset <= Extent, "Offset must be smaller than Extent");
        static_assert(Count == dynamic_extent || Count <= Extent - Offset, "Count must be smaller than Extent - Offset");

        return span < element_type,
               Count != dynamic_extent ? Count : Extent - Offset > (data_ + Offset, Count == dynamic_extent ? size() - Offset : Count);
    }

    constexpr span<element_type, dynamic_extent> subspan(size_type offset, size_type count = dynamic_extent) const {
        RuntimeAssert(offset <= size(), "offset %zu must be smaller than size %zu", offset, size());
        RuntimeAssert(
                count == dynamic_extent || count <= size() - offset, "count %zu must be smaller than size %zu - offset %zu", count, size(),
                offset);

        if (count == dynamic_extent) {
            return span<element_type, dynamic_extent>(data() + offset, size() - offset);
        }

        return span<element_type, dynamic_extent>(data() + offset, count);
    }

private:
    T* data_;
};

template <typename T>
class span<T, dynamic_extent> {
public:
    using element_type = T;
    using value_type = std::remove_cv_t<T>;
    using size_type = std::size_t;
    using difference_type = std::ptrdiff_t;
    using pointer = T*;
    using const_pointer = const T*;
    using reference = T&;
    using const_reference = const T&;
    using iterator = T*;
    using reverse_iterator = std::reverse_iterator<iterator>;

    static constexpr std::size_t extent = dynamic_extent;

    constexpr span() noexcept : data_(nullptr), size_(0) {}

    // TODO: Instead of U* it should just be a `contiguos_iterator` with a deduction guide.
    template <typename U, internal::EnableIfConvertibleForSpan<element_type, U> = nullptr>
    constexpr span(U* first, size_type count) : data_(first), size_(count) {}

    // TODO: Instead of U* it should just be a `contiguos_iterator` with a deduction guide.
    template <typename U, internal::EnableIfConvertibleForSpan<element_type, U> = nullptr>
    constexpr span(U* first, U* last) : data_(first), size_(std::distance(first, last)) {}

    template <std::size_t N>
    constexpr span(element_type (&arr)[N]) noexcept : data_(arr), size_(N) {}

    template <typename U, std::size_t N, internal::EnableIfConvertibleForSpan<element_type, U> = nullptr>
    constexpr span(std::array<U, N>& arr) noexcept : data_(arr.data()), size_(N) {}

    template <typename U, std::size_t N, internal::EnableIfConvertibleForSpan<element_type, const U> = nullptr>
    constexpr span(const std::array<U, N>& arr) noexcept : data_(arr.data()), size_(N) {}

    // TODO: Constructor from a range with a deduction guide.

    template <typename U, std::size_t N, internal::EnableIfConvertibleForSpan<element_type, U> = nullptr>
    constexpr span(const span<U, N>& source) noexcept : data_(source.data()), size_(source.size()) {}

    constexpr span(const span& other) noexcept = default;

    constexpr span& operator=(const span& other) noexcept = default;

    constexpr iterator begin() const noexcept { return data_; }
    constexpr iterator end() const noexcept { return data_ + size(); }

    constexpr reverse_iterator rbegin() const noexcept { return std::reverse_iterator(end()); }
    constexpr reverse_iterator rend() const noexcept { return std::reverse_iterator(begin()); }

    constexpr reference front() const {
        RuntimeAssert(!empty(), "Calling front on an empty span");
        return *begin();
    }
    constexpr reference back() const {
        RuntimeAssert(!empty(), "Calling back on an empty span");
        return *(end() - 1);
    }
    constexpr reference operator[](size_type idx) const {
        RuntimeAssert(idx < size(), "Indexing at %zu on a span of size %zu", idx, size());
        return data()[idx];
    }

    constexpr pointer data() const noexcept { return data_; }

    constexpr size_type size() const noexcept { return size_; }
    constexpr size_type size_bytes() const noexcept { return size_ * sizeof(element_type); }
    [[nodiscard]] constexpr bool empty() const noexcept { return size_ == 0; }

    template <std::size_t Count>
    constexpr span<element_type, Count> first() const {
        RuntimeAssert(Count <= size(), "Count %zu must be smaller than size %zu", Count, size());
        return span<element_type, Count>(data_, Count);
    }

    constexpr span<element_type, dynamic_extent> first(size_type count) const {
        RuntimeAssert(count <= size(), "count %zu must be smaller than size %zu", count, size());
        return span<element_type, dynamic_extent>(data_, count);
    }

    template <std::size_t Count>
    constexpr span<element_type, Count> last() const {
        RuntimeAssert(Count <= size(), "Count %zu must be smaller than size %zu", Count, size());
        return span<element_type, Count>(data_ + size() - Count, Count);
    }

    constexpr span<element_type, dynamic_extent> last(size_type count) const {
        RuntimeAssert(count <= size(), "count %zu must be smaller than size %zu", count, size());
        return span<element_type, dynamic_extent>(data_ + size() - count, count);
    }

    template <std::size_t Offset, std::size_t Count = dynamic_extent>
    constexpr span<element_type, Count> subspan() const {
        RuntimeAssert(Offset <= size(), "Offset %zu must be smaller than size %zu", Offset, size());
        RuntimeAssert(
                Count == dynamic_extent || Count <= size() - Offset, "Count %zu must be smaller than size %zu - Offset %zu", Count, size(),
                Offset);

        return span<element_type, Count>(data_ + Offset, Count == dynamic_extent ? size() - Offset : Count);
    }

    constexpr span<element_type, dynamic_extent> subspan(size_type offset, size_type count = dynamic_extent) const {
        RuntimeAssert(offset <= size(), "offset %zu must be smaller than size %zu", offset, size());
        RuntimeAssert(
                count == dynamic_extent || count <= size() - offset, "count %zu must be smaller than size %zu - offset %zu", count, size(),
                offset);

        if (count == dynamic_extent) {
            return span<element_type, dynamic_extent>(data() + offset, size() - offset);
        }

        return span<element_type, dynamic_extent>(data() + offset, count);
    }

private:
    T* data_;
    size_type size_;
};

template <class T, std::size_t N>
span(T (&)[N]) -> span<T, N>;

template <class T, std::size_t N>
span(std::array<T, N>&) -> span<T, N>;

template <class T, std::size_t N>
span(const std::array<T, N>&) -> span<const T, N>;

// TODO: as_bytes, as_writable_bytes

// TODO: Tags for ranges.

} // namespace std_support
} // namespace kotlin

#endif // RUNTIME_CPP_SUPPORT_SPAN_H
