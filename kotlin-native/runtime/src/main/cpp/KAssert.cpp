/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include <cstdarg>

#include "Porting.h"
#include "StackTrace.hpp"

namespace {

// TODO: Enable stacktraces for asserts when stacktrace printing is more mature.
inline constexpr bool kEnableStacktraces = false;

} // namespace

RUNTIME_NORETURN void RuntimeAssertFailed(const char* location, const char* format, ...) {
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
    if constexpr (kEnableStacktraces) {
        kotlin::PrintStackTraceStderr();
    }
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
