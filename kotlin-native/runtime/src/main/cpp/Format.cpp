/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "Format.h"

#include <cstdio>

#include "Porting.h"

using namespace kotlin;

std_support::span<char> kotlin::FormatToSpan(std_support::span<char> buffer, const char* format, ...) noexcept {
    std::va_list args;
    va_start(args, format);
    auto result = VFormatToSpan(buffer, format, args);
    va_end(args);
    return result;
}

std_support::span<char> kotlin::VFormatToSpan(std_support::span<char> buffer, const char* format, std::va_list args) noexcept {
    if (buffer.empty()) return buffer;
    if (buffer.size() == 1) {
        buffer.front() = '\0';
        return buffer;
    }
    int written = std::vsnprintf(buffer.data(), buffer.size(), format, args);
    // Consider this a failure, nothing has been written. TODO: Should this be an exception/RuntimeAssert?
    if (written < 0) return buffer;
    // If `written` is larger than the buffer size, just pretend we filled the entire buffer (ignoring the trailing \0).
    size_t writtenSize = std::min(static_cast<size_t>(written), buffer.size() - 1);
    return buffer.subspan(writtenSize);
}
