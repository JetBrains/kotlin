/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include <cstdarg>
#include <cstdlib>
#include "Porting.h"

#if !KONAN_NO_EXCEPTIONS
#if USE_GCC_UNWIND
// GCC unwinder for backtrace.
#include <unwind.h>
// AddressToSymbol mapping.
#include "ExecFormat.h"
#else
// Glibc backtrace() function.
#include <execinfo.h>
#endif // USE_GCC_UNWIND
#endif // !KONAN_NO_EXCEPTIONS


// The stacktrace printing below partially duplicates the logic that forms stacktraces for Kotlin exceptions (see Exceptions.cpp).
// TODO: Unify this code with the exceptions machinery.
namespace {

#if !KONAN_NO_EXCEPTIONS

#if (__MINGW32__ || __MINGW64__)
// Skip the stack frames related to `printStackTrace` and `_Unwind_Backtrace`
constexpr int kSkipFrames = 2;
#else
// Skip the stack frame related to the `printStackTrace` call.
constexpr int kSkipFrames = 1;
#endif

struct TraceData {
    static constexpr int kBufferCapacity = 32;

    int size = 0;
    void* buffer[kBufferCapacity] = { nullptr };
};

#if USE_GCC_UNWIND

_Unwind_Ptr getUnwindPtr(_Unwind_Context* context) {
#if (__MINGW32__ || __MINGW64__)
    return _Unwind_GetRegionStart(context);
#else
    return _Unwind_GetIP(context);
#endif
}

NO_INLINE void printStackTrace() {
    _Unwind_Trace_Fn unwindCallback = [](_Unwind_Context* context, void* arg) {
        auto* data = static_cast<TraceData*>(arg);
        // We do not allocate a dynamic storage for the stacktrace so store only first `kBufferCapacity` elements.
        if (data->size < TraceData::kBufferCapacity) {
            data->buffer[data->size++] = reinterpret_cast<void*>(getUnwindPtr(context));
        }
        return _URC_NO_REASON;
    };

    TraceData data;
    _Unwind_Backtrace(unwindCallback, &data);

    for (int i = kSkipFrames; i < data.size; ++i) {
        char symbol[512];
        void* address = data.buffer[i];

        if (!AddressToSymbol(address, symbol, sizeof(symbol))) {
            // Make an empty string.
            symbol[0] = '\0';
        }
        konan::consoleErrorf("%p %s\n", address, symbol);
    }
}

#else // USE_GCC_UNWIND

NO_INLINE void printStackTrace() {
    TraceData data;
    data.size = backtrace(data.buffer, TraceData::kBufferCapacity);
    char** symbols = backtrace_symbols(data.buffer, data.size);

    if (symbols == nullptr) {
        konan::consoleErrorf("Cannot allocate memory for the stacktrace.\n");
        return;
    }

    for (int i = kSkipFrames; i < data.size; ++i) {
        konan::consoleErrorf("%s\n (%p)", symbols[i], data.buffer[i]);
    }
    free(symbols);
}

#endif // USE_GCC_UNWIND

#else // !KONAN_NO_EXCEPTIONS

void printStackTrace() { /* No-op */ }

#endif // !KONAN_NO_EXCEPTIONS

} // namespace

RUNTIME_NORETURN NO_INLINE void RuntimeAssertFailed(const char* location, const char* format, ...) {
    char buf[1024];
    int written = -1;

    // Write the title with a source location.
    if (location != nullptr) {
        written = konan::snprintf(buf, sizeof(buf), "%s: runtime assert: ", location);
    } else {
        written = konan::snprintf(buf, sizeof(buf), "runtime assert: ");
    }

    // Write the message.
    if (written >= 0 && static_cast<size_t>(written) < sizeof(buf)) {
        std::va_list args;
        va_start(args, format);
        konan::vsnprintf(buf + written, sizeof(buf) - written, format, args);
        va_end(args);
    }

    konan::consoleErrorUtf8(buf, konan::strnlen(buf, sizeof(buf)));
    konan::consoleErrorf("\n");
    printStackTrace();
    konan::abort();
}

// TODO: this function is not used by runtime, but apparently there are
// third-party libraries that use it (despite the fact it is not a public API).
// Keeping the function here for now for backward compatibility, to be removed later.
RUNTIME_NORETURN void RuntimeAssertFailed(const char* location, const char* message) {
  char buf[1024];
  if (location != nullptr)
      konan::snprintf(buf, sizeof(buf), "%s: runtime assert: %s\n", location, message);
  else
      konan::snprintf(buf, sizeof(buf), "runtime assert: %s\n", message);
  konan::consoleErrorUtf8(buf, konan::strnlen(buf, sizeof(buf)));
  konan::abort();
}
