/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_FORMAT_H
#define RUNTIME_FORMAT_H

#include <cstdarg>

#include "std_support/Span.hpp"

namespace kotlin {

/*
 * `FormatToSpan` is a `snprintf`-like API for formatting text.
 *
 * It formats text into a `span` buffer (it uses `snprintf` under the hood and so needs a contiguous memory buffer to write into)
 * and returns the unused portion of `span`.
 * When input `span` is empty it does nothing.
 * If `snprintf` fails it returns the buffer unmodified.
 * If the formatted text cannot fit into the buffer, it'll be truncated enough to fit.
 * If any character was written into the buffer, there will always be a null character appended, and the returned span will point to it.
 *
 * ```
 * auto output = FormatToSpan(input, "string");
 * ```
 * If `input` was a buffer filled with `'\1'` of size 10, by the end the memory would look something like:
 * ```
 * s t r i n g \0 \1 \1 \1
 * ^           ^
 * |           |
 * input       output
 * ```
 * and the output will be of size 4.
 * If `input` was of size 5 instead:
 * ```
 * s t r i \0
 * ^       ^
 * |       |
 * |       output
 * input
 * ```
 * and the output will be of size 1.
 *
 * This allows for a composable behaviour:
 * ```
 * auto buffer = ...;
 * buffer = FormatToSpan(buffer, "str");
 * buffer = FormatToSpan(buffer, "%d", 42);
 * buffer = FormatToSpan(buffer, "%s", "again");
 * ```
 * If `buffer` was of sufficient size, the result will be `str42again\0`.
 */

// Format snprintf-style message into a `buffer` and return the unused portion of a buffer.
std_support::span<char> FormatToSpan(std_support::span<char> buffer, const char* format, ...) noexcept
        __attribute__((format(printf, 2, 3)));

// Format vsnprintf-style message into a `buffer` and return the unused portion of a buffer.
std_support::span<char> VFormatToSpan(std_support::span<char> buffer, const char* format, std::va_list args) noexcept;

} // namespace kotlin

#endif // RUNTIME_FORMAT_H
