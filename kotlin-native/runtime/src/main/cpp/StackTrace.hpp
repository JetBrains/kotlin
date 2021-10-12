/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_STACK_TRACE_H
#define RUNTIME_STACK_TRACE_H

#if KONAN_NO_BACKTRACE
// Nothing to include
#elif USE_GCC_UNWIND
// GCC unwinder for backtrace.
#include <unwind.h>
#else
// Glibc backtrace() function.
#include <execinfo.h>
#endif

#include "cpp_support/Span.hpp"
#include "Memory.h"
#include "Types.h"

namespace kotlin {
namespace internal {

// TODO: int -> size_t. And other.
// TODO: uint32_t size - compare with 0 doesn't make sense

#if USE_GCC_UNWIND
template <class Allocator>
struct Backtrace {
    Backtrace(int count, int skip, int maxFramesToCollect, Allocator& allocator) :
        skipCount(skip), remainingFrames(maxFramesToCollect), array(allocator)  {
        int size = std::min(count - skipCount, maxFramesToCollect);
        if (size < 0) { size = 0; }
        array.reserve(size);
    }

    int skipCount;
    int remainingFrames;
    std::vector<void*, Allocator> array;
};

_Unwind_Reason_Code depthCountCallback(struct _Unwind_Context* context, void* arg);

template <class Allocator>
_Unwind_Reason_Code unwindCallback(struct _Unwind_Context* context, void* arg) {
    auto* backtrace = reinterpret_cast<Backtrace<Allocator>*>(arg);
    if (backtrace->skipCount > 0) {
        backtrace->skipCount--;
        return _URC_NO_REASON;
    }

    if (backtrace->remainingFrames == 0) {
        // Just skip frames until the end of the stack.
        return _URC_NO_REASON;
    }

#if (__MINGW32__ || __MINGW64__)
    _Unwind_Ptr address = _Unwind_GetRegionStart(context);
#else
    _Unwind_Ptr address = _Unwind_GetIP(context);
#endif
    backtrace->setNextElement(address);

    return _URC_NO_REASON;
}
#endif

// TODO: this implementation is just a hack, e.g. the result is inexact;
// however it is better to have an inexact stacktrace than not to have any.
template <class Allocator>
NO_INLINE std::vector<void*, Allocator> GetCurrentStackTrace(size_t extraSkipFrames,
                                                             size_t maxFramesToCollect,
                                                             const Allocator& allocator) {
#if KONAN_NO_BACKTRACE
    return {};
#else
    // Skips this function frame + anything asked by the caller.
    const int kSkipFrames = 1 + extraSkipFrames;
#if USE_GCC_UNWIND
    int depth = 0;
    _Unwind_Backtrace(depthCountCallback, static_cast<void*>(&depth));
    Backtrace result(depth, kSkipFrames, maxFramesToCollect, allocator);
    if (result.array.capacity() > 0) {
        _Unwind_Backtrace(unwindCallback<Allocator>, static_cast<void*>(&result));
    }
    return std::move(result.array);
#else
    const int maxSize = 32;
    void* buffer[maxSize];

    std::vector<void*, Allocator> result(0, allocator);
    int depth = backtrace(buffer, maxSize);
    if (depth < kSkipFrames) return result;

    size_t size = std::min(static_cast<size_t>(depth - kSkipFrames), maxFramesToCollect);
    result.reserve(size);
    for (size_t index = kSkipFrames; index < size + kSkipFrames; ++index) {
        result.push_back(buffer[index]);
    }
    return result;
#endif
#endif // !KONAN_NO_BACKTRACE
}

} // namespace internal

// TODO: Model API as in upcoming https://en.cppreference.com/w/cpp/utility/basic_stacktrace
template <typename Allocator = KonanAllocator<void*>>
class StackTrace final : private MoveOnly {
public:
    StackTrace() noexcept : buffer_(Allocator{}) {};
    StackTrace(StackTrace<Allocator>&& other) noexcept : buffer_(std::move(other.buffer_)) {};

    StackTrace& operator=(StackTrace<Allocator>&& other) noexcept {
        buffer_ = std::move(other.buffer_);
        return *this;
    }

    size_t size() noexcept {
        return buffer_.size();
    }

    void*& operator[](size_t index) {
        return buffer_[index];
    }

    std_support::span<void*> data() noexcept {
        return std_support::span<void*>(buffer_.data(), buffer_.size());
    }

    // TODO: It can throw. Is it ok?
    NO_INLINE static StackTrace current(size_t skipFrames = 0, const Allocator& allocator = Allocator()) {
        return StackTrace(internal::GetCurrentStackTrace(skipFrames + 1, std::numeric_limits<size_t>::max(), allocator));
    }

    NO_INLINE static StackTrace current(size_t skipFrames, size_t maxDepth, const Allocator& allocator = Allocator()) {
        return StackTrace(internal::GetCurrentStackTrace(skipFrames + 1, maxDepth, allocator));
    }

private:
    explicit StackTrace(std::vector<void*, Allocator>&& data) noexcept : buffer_(std::move(data)) {}

    std::vector<void*, Allocator> buffer_;
};

KStdVector<KStdString> GetStackTraceStrings(const std_support::span<void* const> stackTrace) noexcept;

// It's not always safe to extract SourceInfo during unhandled exception termination.
void DisallowSourceInfo();

void PrintStackTraceStderr();

} // namespace kotlin

#endif // RUNTIME_STACK_TRACE_H
