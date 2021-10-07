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
    Backtrace(int count, int skip) : skipCount(skip) {
        uint32_t size = count - skipCount;
        if (size < 0) {
            size = 0;
        }
        array.reserve(size);
    }

    void setNextElement(_Unwind_Ptr element) { array.push_back(reinterpret_cast<void*>(element)); }

    int skipCount;
    KStdVector<void*> array;
};

_Unwind_Reason_Code depthCountCallback(struct _Unwind_Context* context, void* arg) {
    int* result = reinterpret_cast<int*>(arg);
    (*result)++;
    return _URC_NO_REASON;
}

_Unwind_Reason_Code unwindCallback(struct _Unwind_Context* context, void* arg) {
    Backtrace* backtrace = reinterpret_cast<Backtrace*>(arg);
    if (backtrace->skipCount > 0) {
        backtrace->skipCount--;
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

THREAD_LOCAL_VARIABLE bool disallowSourceInfo = false;

#if !KONAN_NO_BACKTRACE
int getSourceInfo(void* symbol, SourceInfo *result, int result_len) {
    return disallowSourceInfo ? 0 : compiler::getSourceInfo(symbol, result, result_len);
}
#endif

} // namespace

// TODO: this implementation is just a hack, e.g. the result is inexact;
// however it is better to have an inexact stacktrace than not to have any.
NO_INLINE KStdVector<void*> kotlin::GetCurrentStackTrace(int extraSkipFrames) noexcept {
#if KONAN_NO_BACKTRACE
    return {};
#else
    // Skips this function frame + anything asked by the caller.
    const int kSkipFrames = 1 + extraSkipFrames;
#if USE_GCC_UNWIND
    int depth = 0;
    _Unwind_Backtrace(depthCountCallback, static_cast<void*>(&depth));
    Backtrace result(depth, kSkipFrames);
    if (result.array.capacity() > 0) {
        _Unwind_Backtrace(unwindCallback, static_cast<void*>(&result));
    }
    return std::move(result.array);
#else
    const int maxSize = 32;
    void* buffer[maxSize];

    int size = backtrace(buffer, maxSize);
    if (size < kSkipFrames) return {};

    KStdVector<void*> result;
    result.reserve(size - kSkipFrames);
    for (int index = kSkipFrames; index < size; ++index) {
        result.push_back(buffer[index]);
    }
    return result;
#endif
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

KStdVector<KStdString> kotlin::GetStackTraceStrings(void* const* stackTrace, size_t stackTraceSize) noexcept {
#if KONAN_NO_BACKTRACE
    KStdVector<KStdString> strings;
    strings.push_back("<UNIMPLEMENTED>");
    return strings;
#else
    KStdVector<KStdString> strings;
    strings.reserve(stackTraceSize);
    if (stackTraceSize > 0) {
        SourceInfo buffer[10]; // outside of the loop to avoid calling constructors and destructors each time
        for (size_t index = 0; index < stackTraceSize; ++index) {
            KNativePtr address = stackTrace[index];
            if (!address || reinterpret_cast<uintptr_t>(address) == 1) continue;
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

    // TODO: This might have to go into `GetCurrentStackTrace`, but this changes the generated stacktrace for
    //       `Throwable`.
#if KONAN_WINDOWS
    // Skip this function and `_Unwind_Backtrace`.
    constexpr int kSkipFrames = 2;
#else
    // Skip this function.
    constexpr int kSkipFrames = 1;
#endif
    auto stackTrace = GetCurrentStackTrace(kSkipFrames);
    auto stackTraceStrings = GetStackTraceStrings(stackTrace.data(), stackTrace.size());
    for (auto& frame : stackTraceStrings) {
        konan::consoleErrorUtf8(frame.c_str(), frame.size());
        konan::consoleErrorf("\n");
    }
}
