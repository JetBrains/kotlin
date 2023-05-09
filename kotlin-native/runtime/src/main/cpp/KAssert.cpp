/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "KAssert.h"

#include <array>
#include <cstdarg>

#include "std_support/Span.hpp"
#include "Format.h"
#include "Porting.h"
#include "StackTrace.hpp"

using namespace kotlin;

namespace {

THREAD_LOCAL_VARIABLE bool assertionReportInProgress = false;

void PrintAssert(bool allowStacktrace, const char* location, const char* format, std::va_list args) noexcept {
    if (assertionReportInProgress) {
        // WARNING: avoid anything that can assert ar panic here
        konan::consoleErrorf("An attempt to report an assertion lead to another failure:\n");
        // now try to print the information we have in the simplest way possible
        if (location != nullptr) {
            konan::consoleErrorf("%s: ", location);
        }
        // do not bother with format string expansion
        konan::consoleErrorf("%s\n", format);
        return;
    }
    AutoReset recursionGuard(&assertionReportInProgress, true);

    std::array<char, 1024> bufferStorage;
    std_support::span<char> buffer(bufferStorage);

    buffer = FormatToSpan(buffer, "[tid#%d] ", konan::currentThreadId());

    // Write the title with a source location.
    if (location != nullptr) {
        buffer = FormatToSpan(buffer, "%s: runtime assert: ", location);
    } else {
        buffer = FormatToSpan(buffer, "runtime assert: ");
    }

    // Write the message.
    buffer = VFormatToSpan(buffer, format, args);

    konan::consoleErrorUtf8(bufferStorage.data(), bufferStorage.size() - buffer.size());
    konan::consoleErrorf("\n");
    if (allowStacktrace) {
        kotlin::PrintStackTraceStderr();
    }
}

} // namespace

void internal::RuntimeAssertFailedLog(bool allowStacktrace, const char* location, const char* format, ...) {
    std::va_list args;
    va_start(args, format);
    PrintAssert(allowStacktrace, location, format, args);
    va_end(args);
}

RUNTIME_NORETURN void internal::RuntimeAssertFailedPanic(bool allowStacktrace, const char* location, const char* format, ...) {
    std::va_list args;
    va_start(args, format);
    PrintAssert(allowStacktrace, location, format, args);
    va_end(args);
    konan::abort();
}
