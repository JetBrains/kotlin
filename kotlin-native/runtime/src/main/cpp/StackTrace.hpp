/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_STACK_TRACE_H
#define RUNTIME_STACK_TRACE_H

#include <algorithm>
#include <string>
#include <vector>

#include "Common.h"
#include "Utils.hpp"
#include "std_support/Span.hpp"

namespace kotlin {

namespace internal {

NO_INLINE std::vector<void*> GetCurrentStackTrace(size_t skipFrames) noexcept;
NO_INLINE size_t GetCurrentStackTrace(size_t skipFrames, std_support::span<void*> buffer) noexcept;

enum class StackTraceCapacityKind {
    kFixed, kDynamic
};

template <StackTraceCapacityKind kind>
constexpr size_t GetMaxStackTraceDepth() noexcept {
#if USE_GCC_UNWIND
    return std::numeric_limits<size_t>::max();
#else
    switch (kind) {
        case StackTraceCapacityKind::kFixed:
            return 32;
        case StackTraceCapacityKind::kDynamic:
            return 128;
    }
#endif
}

}

static constexpr size_t kDynamicCapacity = std::numeric_limits<size_t>::max();

template <size_t Capacity = kDynamicCapacity>
class StackTrace {
public:
    using Iterator = void* const*;

    StackTrace() noexcept : size_(0), buffer_{nullptr} {};
    StackTrace(const StackTrace& other) = default;
    StackTrace(StackTrace&& other) noexcept = default;

    StackTrace& operator=(const StackTrace& other) = default;
    StackTrace& operator=(StackTrace&& other) noexcept = default;

    size_t size() const noexcept {
        return size_;
    }

    void* operator[](size_t index) const noexcept {
        return buffer_[index];
    }

    Iterator begin() const noexcept {
        return buffer_.data();
    }

    Iterator end() const noexcept {
        return buffer_.data() + size();
    }

    std_support::span<void* const> data() const noexcept {
        return std_support::span<void* const>(buffer_.data(), size());
    }

    bool operator==(const StackTrace& other) const noexcept {
        return std::equal(begin(), end(), other.begin(), other.end());
    }

    bool operator!=(const StackTrace& other) const noexcept {
        return !(*this == other);
    }

    // Maximal stacktrace depth that can be collected due to implementation limitations.
    // Note that this limitation doesn't take into account the skipFrames parameter.
    // I.e. real size of a returned stacktrace will be limited by (maxDepth - skipFrames).
    static constexpr size_t maxDepth =
            std::min(internal::GetMaxStackTraceDepth<internal::StackTraceCapacityKind::kFixed>(), Capacity);

    NO_INLINE static StackTrace current(size_t skipFrames, size_t depthLimit) {
        StackTrace result;
        auto fullTraceSize = internal::GetCurrentStackTrace(
                skipFrames + 1, std_support::span<void*>(result.buffer_.data(), result.buffer_.size()));
        result.size_ = std::min(fullTraceSize, depthLimit);
        return result;
    }

    NO_INLINE static StackTrace current(size_t skipFrames = 0) noexcept {
        // Avoid delegating to current(skipFrames, depth)
        // to have the same number of "service" frames for both overloads.
        StackTrace result;
        result.size_ = internal::GetCurrentStackTrace(
                skipFrames + 1, std_support::span<void*>(result.buffer_.data(), result.buffer_.size()));
        return result;
    }

    struct TestSupport : private Pinned {
        static StackTrace constructFrom(std::initializer_list<void*> values) {
            StackTrace result;
            size_t elementsCount = std::min(values.size(), result.buffer_.size());
            std::copy_n(values.begin(), elementsCount, result.buffer_.begin());
            result.size_ = elementsCount;
            return result;
        }
    };

private:
    size_t size_;
    std::array<void*, Capacity> buffer_;
};

template<>
class StackTrace<kDynamicCapacity> {
public:
    using Iterator = void* const*;

    StackTrace() noexcept = default;
    StackTrace(const StackTrace& other) = default;
    StackTrace(StackTrace&& other) noexcept = default;

    StackTrace& operator=(const StackTrace& other) noexcept = default;
    StackTrace& operator=(StackTrace&& other) noexcept = default;

    size_t size() const noexcept {
        return buffer_.size();
    }

    void* operator[](size_t index) const noexcept {
        return buffer_[index];
    }

    Iterator begin() const noexcept {
        return buffer_.data();
    }

    Iterator end() const noexcept  {
        return buffer_.data() + size();
    }

    std_support::span<void* const> data() const noexcept {
        return std_support::span<void* const>(buffer_.data(), size());
    }

    bool operator==(const StackTrace& other) const noexcept {
        return std::equal(begin(), end(), other.begin(), other.end());
    }

    bool operator!=(const StackTrace& other) const noexcept {
        return !(*this == other);
    }

    // Maximal stacktrace depth that can be collected due to implementation limitations.
    // Note that this limitation doesn't take into account the skipFrames parameter.
    // I.e. real size of a returned stacktrace will be limited by (maxDepth - skipFrames).
    static constexpr size_t maxDepth = internal::GetMaxStackTraceDepth<internal::StackTraceCapacityKind::kDynamic>();

    NO_INLINE static StackTrace current(size_t skipFrames, size_t depthLimit) {
        auto traceElements = internal::GetCurrentStackTrace(skipFrames + 1);
        if (traceElements.size() > depthLimit) {
            traceElements.resize(depthLimit);
        }
        return StackTrace(std::move(traceElements));
    }

    NO_INLINE static StackTrace current(size_t skipFrames = 0) {
        // Avoid delegating to current(skipFrames, depth)
        // to have the same number of "service" frames for both overloads.
        auto traceElements = internal::GetCurrentStackTrace(skipFrames + 1);
        return StackTrace(std::move(traceElements));
    }

    struct TestSupport : private Pinned {
        static StackTrace constructFrom(std::initializer_list<void*> values) {
            std::vector<void*> traceElements(values);
            return StackTrace(std::move(traceElements));
        }
    };

private:
    explicit StackTrace(std::vector<void*>&& buffer) noexcept : buffer_(buffer) {}

    std::vector<void*> buffer_;
};

std::vector<std::string> GetStackTraceStrings(std_support::span<void* const> stackTrace) noexcept;

// It's not always safe to extract SourceInfo during unhandled exception termination.
void DisallowSourceInfo();

void PrintStackTraceStderr();

} // namespace kotlin

template <size_t Capacity>
struct std::hash<kotlin::StackTrace<Capacity>> {
    size_t operator()(kotlin::StackTrace<Capacity> value) const {
        size_t result = 0;
        std::hash<void*> hasher;
        for (void* p : value) {
            result += kotlin::CombineHash(result, hasher(p));
        }
        return result;
    }
};

#endif // RUNTIME_STACK_TRACE_H
