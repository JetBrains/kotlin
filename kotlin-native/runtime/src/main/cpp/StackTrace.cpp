/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "StackTrace.hpp"

#if KONAN_NO_BACKTRACE
// Nothing to include
#elif USE_GCC_UNWIND
// GCC unwinder for backtrace.
#include <unwind.h>
#else
// Glibc backtrace() function.
#include <execinfo.h>
#endif

#include <algorithm>

#include "Common.h"
#include "ExecFormat.h"
#include "Porting.h"
#include "SourceInfo.h"
#include "Types.h"

#include "utf8.h"

using namespace kotlin;

namespace {

#if USE_GCC_UNWIND
struct Backtrace {
    Backtrace(size_t skip, std_support::span<void*> buffer): currentSize(0), skipCount(skip), buffer(buffer) {}

    void setNextElement(_Unwind_Ptr element) {
        RuntimeAssert(currentSize < buffer.size(), "Buffer overflow");
        buffer[currentSize++] = reinterpret_cast<void*>(element);
    }

    bool full() const { return currentSize >= buffer.size(); }

    size_t currentSize;
    size_t skipCount;
    std_support::span<void*> buffer;
};

_Unwind_Ptr getUnwindPtr(_Unwind_Context* context) {
#if (__MINGW32__ || __MINGW64__)
    return _Unwind_GetRegionStart(context);
#else
    return _Unwind_GetIP(context);
#endif
}

_Unwind_Reason_Code depthCountCallback(struct _Unwind_Context* context, void* arg) {
    size_t* result = reinterpret_cast<size_t*>(arg);
    (*result)++;
    return _URC_NO_REASON;
}

_Unwind_Reason_Code unwindCallback(struct _Unwind_Context* context, void* arg) {
    Backtrace* backtrace = reinterpret_cast<Backtrace*>(arg);
    if (backtrace->skipCount > 0) {
        backtrace->skipCount--;
        return _URC_NO_REASON;
    }

    // If the buffer is full, skip the remaining frames.
    if (backtrace->full()) {
        return _URC_NO_REASON;
    }

    _Unwind_Ptr address = getUnwindPtr(context);
    backtrace->setNextElement(address);

    return _URC_NO_REASON;
}
#endif

THREAD_LOCAL_VARIABLE bool disallowSourceInfo = false;

#if !KONAN_NO_BACKTRACE
int getSourceInfo(void* symbol, SourceInfo *result, int result_len) {
    return disallowSourceInfo ? 0 : compiler::getSourceInfo(symbol, result, result_len);
}
#endif

} // namespace

// TODO: this implementation is just a hack, e.g. the result is inexact;
// however it is better to have an inexact stacktrace than not to have any.
NO_INLINE KStdVector<void*> kotlin::internal::GetCurrentStackTrace(size_t skipFrames) noexcept {
    NativeOrUnregisteredThreadGuard guard(true);
#if KONAN_NO_BACKTRACE
    return {};
#else

#if (__MINGW32__ || __MINGW64__)
    // Skip GetCurrentStackTrace, _Unwind_Backtrace + anything asked by the caller.
    const size_t kSkipFrames = 2 + skipFrames;
#else
    // Skip GetCurrentStackTrace + anything asked by the caller.
    const size_t kSkipFrames = 1 + skipFrames;
#endif

    KStdVector<void*> result;
#if USE_GCC_UNWIND
    size_t depth = 0;
    _Unwind_Backtrace(depthCountCallback, static_cast<void*>(&depth));
    if (depth <= kSkipFrames) return {};
    result.resize(depth - kSkipFrames);

    Backtrace traceHolder(kSkipFrames, std_support::span<void*>(result.data(), result.size()));
    _Unwind_Backtrace(unwindCallback, static_cast<void*>(&traceHolder));
    RuntimeAssert(result.size() == traceHolder.currentSize, "Expected and collected sizes of the stacktrace differ");

    return result;
#else
    // Take into account this function and StackTrace::current.
    constexpr size_t maxSize = GetMaxStackTraceDepth<StackTraceCapacityKind::kDynamic>() + 2;
    result.resize(maxSize);
    auto size = static_cast<size_t>(backtrace(result.data(), static_cast<int>(result.size())));
    if (size <= kSkipFrames) return {};
    result.resize(size);

    // Drop first kSkipFrames elements.
    result.erase(result.begin(), std::next(result.begin(), kSkipFrames));
    return result;
#endif // !USE_GCC_UNWIND
#endif // !KONAN_NO_BACKTRACE
}

// TODO: this implementation is just a hack, e.g. the result is inexact;
// however it is better to have an inexact stacktrace than not to have any.
NO_INLINE size_t kotlin::internal::GetCurrentStackTrace(size_t skipFrames, std_support::span<void*> buffer) noexcept {
    NativeOrUnregisteredThreadGuard guard(true);
#if KONAN_NO_BACKTRACE
    return {};
#else

#if (__MINGW32__ || __MINGW64__)
    // Skip GetCurrentStackTrace, _Unwind_Backtrace + anything asked by the caller.
    const size_t kSkipFrames = 2 + skipFrames;
#else
    // Skip GetCurrentStackTrace + anything asked by the caller.
    const size_t kSkipFrames = 1 + skipFrames;
#endif

#if USE_GCC_UNWIND
    Backtrace traceHolder(kSkipFrames, buffer);
    _Unwind_Backtrace(unwindCallback, static_cast<void*>(&traceHolder));
    return traceHolder.currentSize;
#else
    // Take into account this function and StackTrace::current.
    constexpr size_t maxSize = GetMaxStackTraceDepth<StackTraceCapacityKind::kFixed>() + 2;
    void* tmpBuffer[maxSize];
    size_t size = backtrace(tmpBuffer, static_cast<int>(maxSize));
    if (size <= kSkipFrames) return 0;

    size_t elementsCount = std::min(buffer.size(), size - kSkipFrames);
    std::copy_n(std::begin(tmpBuffer) + kSkipFrames, elementsCount, std::begin(buffer));
    return elementsCount;
#endif // !USE_GCC_UNWIND
#endif // !KONAN_NO_BACKTRACE
}

#if ! KONAN_NO_BACKTRACE
#include <cstdarg>
#include <cstring>
#include "cpp_support/Span.hpp"
#include "Format.h"

#if __has_include("dlfcn.h")
#include <dlfcn.h>
#endif

__attribute__((format(printf, 6, 7)))
static size_t snprintf_with_addr(char* buf, size_t size, size_t frame, const void* addr, bool is_inline, const char *format, ...) {
    std_support::span<char> buffer{buf, size};
    const char* image = "???";
    char symbol[512];
    strcpy(symbol, "0x0");
    ptrdiff_t symbol_offset = reinterpret_cast<ptrdiff_t>(addr);

#if __has_include("dlfcn.h")
    Dl_info info;
    memset(&info, 0, sizeof(info));
    dladdr(addr, &info);

    if (info.dli_fname) {
        const char* tmp = strrchr(info.dli_fname, '/');
        if (tmp == nullptr) {
            image = info.dli_fname;
        } else {
            image = tmp + 1;
        }
    }
#endif

    AddressToSymbol(addr, symbol, sizeof(symbol), symbol_offset);

    buffer = FormatToSpan(buffer, "%-4zd%-35s %-18p %s + %td ", frame, image, addr, symbol, symbol_offset);
    if (is_inline) {
        buffer = FormatToSpan(buffer, "[inlined] ");
    }
    std::va_list args;
    va_start(args, format);
    buffer = VFormatToSpan(buffer, format, args);
    va_end(args);
    return size - buffer.size();
}
#endif // ! KONAN_NO_BACKTRACE


/*
 * This is hack for better traces.
 * In some cases backtrace function returns address after call instruction, while address detection need call instruction itself.
 * adjustAddressForSourceInfo function tries to fix it with some heuristics.
 *
 * For honest solution, we should distinguish backtrace symbols got from signal handlers frames, ordinary frames,
 * and addresses got from somewhere else. But for now, we assume all addresses are ordinary backtrace frames.
 */

#if (defined(KONAN_X64) || defined(KONAN_X86)) && !defined(KONAN_WINDOWS)
KNativePtr adjustAddressForSourceInfo(KNativePtr address) {
    return reinterpret_cast<KNativePtr>(reinterpret_cast<uintptr_t>(address) - 1);
}
#elif (defined(KONAN_ARM32) || defined(KONAN_ARM64)) && !defined(KONAN_WINDOWS)
KNativePtr adjustAddressForSourceInfo(KNativePtr address) {
    /*
     * On arm instructions are always 2-bytes aligned. But odd bit can be used to encode instruction set.
     * Not sure, if this can happen in our code, but let's just clear it.
     */
    return reinterpret_cast<KNativePtr>((reinterpret_cast<uintptr_t>(address) & ~1) - 1);
}
#else
KNativePtr adjustAddressForSourceInfo(KNativePtr address) { return address; }
#endif

KStdVector<KStdString> kotlin::GetStackTraceStrings(std_support::span<void* const> stackTrace) noexcept {
    NativeOrUnregisteredThreadGuard guard(true);
#if KONAN_NO_BACKTRACE
    KStdVector<KStdString> strings;
    strings.push_back("<UNIMPLEMENTED>");
    return strings;
#else
    size_t size = stackTrace.size();
    KStdVector<KStdString> strings;
    strings.reserve(size);
    if (size > 0) {
        SourceInfo buffer[10]; // outside of the loop to avoid calling constructors and destructors each time
        for (size_t index = 0; index < size; ++index) {
            KNativePtr address = stackTrace[index];
            if (!address || reinterpret_cast<uintptr_t>(address) == 1) continue;
            address = adjustAddressForSourceInfo(address);
            int frames_or_overflow = getSourceInfo(address, buffer, std::size(buffer));
            int frames = std::min<int>(frames_or_overflow, std::size(buffer));
            bool isSomethingPrinted = false;
            char line[1024];
            for (int frame = 0; frame < frames; frame++) {
                auto &sourceInfo = buffer[frame];
                if (!sourceInfo.getFileName().empty()) {
                    bool is_last = frame == frames - 1;
                    if (is_last && frames_or_overflow != frames) {
                        snprintf_with_addr(line, sizeof(line) - 1, strings.size(), address, false, "[some inlined frames skipped]");
                        strings.push_back(line);
                    }
                    if (sourceInfo.lineNumber != -1) {
                        if (sourceInfo.column != -1) {
                            snprintf_with_addr(
                                    line, sizeof(line) - 1, strings.size(), address, !is_last, "(%s:%d:%d)",
                                    sourceInfo.getFileName().c_str(), sourceInfo.lineNumber, sourceInfo.column);
                        } else {
                            snprintf_with_addr(
                                    line, sizeof(line) - 1, strings.size(), address, !is_last, "(%s:%d)",
                                    sourceInfo.getFileName().c_str(), sourceInfo.lineNumber);
                        }
                    } else {
                        snprintf_with_addr(
                                line, sizeof(line) - 1, strings.size(), address, !is_last, "(%s:<unknown>)",
                                sourceInfo.getFileName().c_str());
                    }
                    isSomethingPrinted = true;
                    strings.push_back(line);
                }
            }
            if (!isSomethingPrinted) {
                snprintf_with_addr(line, sizeof(line) - 1,  strings.size(), address, false, "%s", "");
                strings.push_back(line);
            }
        }
    }
    return strings;
#endif // !KONAN_NO_BACKTRACE
}

void kotlin::DisallowSourceInfo() {
    disallowSourceInfo = true;
}

NO_INLINE void kotlin::PrintStackTraceStderr() {
    // NOTE: This might be called from both runnable and native states (including in uninitialized runtime)
    // TODO: This is intended for runtime use. Try to avoid memory allocations and signal unsafe functions.

    // Skip this function.
    constexpr int kSkipFrames = 1;
    StackTrace trace = StackTrace<>::current(kSkipFrames);
    auto stackTraceStrings = GetStackTraceStrings(trace.data());
    for (auto& frame : stackTraceStrings) {
        konan::consoleErrorUtf8(frame.c_str(), frame.size());
        konan::consoleErrorf("\n");
    }
}
