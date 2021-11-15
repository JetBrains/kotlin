/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_STACK_TRACE_H
#define RUNTIME_STACK_TRACE_H

#include "cpp_support/Span.hpp"
#include "Memory.h"
#include "Types.h"

namespace kotlin {

namespace internal {

NO_INLINE KStdVector<void*> GetCurrentStackTrace(size_t skipFrames) noexcept;
NO_INLINE size_t GetCurrentStackTrace(size_t skipFrames, std_support::span<void*> buffer) noexcept;

enum class StackTraceCapacityKind {
    kFixed, kDynamic
};

template <StackTraceCapacityKind kind>
constexpr size_t GetMaxStackTraceDepth() noexcept {
#if KONAN_NO_BACKTRACE
    return 0;
#elif USE_GCC_UNWIND
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

    // Maximal stacktrace depth that can be collected due to implementation limitations.
    // Note that this limitation doesn't take into account the skipFrames parameter.
    // I.e. real size of a returned stacktrace will be limited by (maxDepth - skipFrames).
    static constexpr size_t maxDepth =
            std::min(internal::GetMaxStackTraceDepth<internal::StackTraceCapacityKind::kFixed>(), Capacity);

    NO_INLINE static StackTrace current(size_t skipFrames = 0) noexcept {
        StackTrace result;
        result.size_ = internal::GetCurrentStackTrace(
                skipFrames + 1, std_support::span<void*>(result.buffer_.data(), result.buffer_.size()));
        return result;
    }

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

    // Maximal stacktrace depth that can be collected due to implementation limitations.
    // Note that this limitation doesn't take into account the skipFrames parameter.
    // I.e. real size of a returned stacktrace will be limited by (maxDepth - skipFrames).
    static constexpr size_t maxDepth = internal::GetMaxStackTraceDepth<internal::StackTraceCapacityKind::kDynamic>();

    NO_INLINE static StackTrace current(size_t skipFrames = 0) {
        StackTrace result;
        result.buffer_ = internal::GetCurrentStackTrace(skipFrames + 1);
        return result;
    }

private:
    KStdVector<void*> buffer_;
};


KStdVector<KStdString> GetStackTraceStrings(std_support::span<void* const> stackTrace) noexcept;

// It's not always safe to extract SourceInfo during unhandled exception termination.
void DisallowSourceInfo();

void PrintStackTraceStderr();

} // namespace kotlin

#endif // RUNTIME_STACK_TRACE_H