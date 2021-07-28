/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include <cstdarg>

#include "Porting.h"
#include "StackTrace.hpp"

using namespace kotlin;

namespace {

void PrintAssert(bool allowStacktrace, const char* location, const char* format, std::va_list args) noexcept {
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
        konan::vsnprintf(buf + written, sizeof(buf) - written, format, args);
    }

    konan::consoleErrorUtf8(buf, konan::strnlen(buf, sizeof(buf)));
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
