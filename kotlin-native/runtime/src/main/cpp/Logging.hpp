/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_LOGGING_H
#define RUNTIME_LOGGING_H

#include <cstdarg>
#include <initializer_list>
#include <memory>
#include <string_view>

#include "Clock.hpp"
#include "CompilerConstants.hpp"
#include "std_support/Span.hpp"

namespace kotlin {
namespace logging {

// Must match LoggingLevel in RuntimeLogging.kt
enum class Level : int32_t {
    kNone = 0,
    kError = 1,
    kWarning = 2,
    kInfo = 3,
    kDebug = 4,
};

// Must match LoggingTag in RuntimeLogging.kt
enum class Tag : int32_t {
    kLogging = 0,
    kRT = 1,
    kGC = 2,
    kMM = 3,
    kTLS = 4,
    kPause = 5,
    kAlloc = 6,
    kBalancing = 7,
    kBarriers = 8,
    kGCMark = 9,

    kEnumSize = 10
};

namespace internal {

inline const char* name(Level level) {
    switch (level) {
        case Level::kNone: return "NONE";
        case Level::kError: return "ERROR";
        case Level::kWarning: return "WARNING";
        case Level::kInfo: return "INFO";
        case Level::kDebug: return "DEBUG";
    }
}

inline const char* name(Tag tag) {
    switch (tag) {
        case Tag::kLogging: return "logging";
        case Tag::kRT: return "rt";
        case Tag::kGC: return "gc";
        case Tag::kMM: return "mm";
        case Tag::kTLS: return "tls";
        case Tag::kPause: return "pause";
        case Tag::kAlloc: return "alloc";
        case Tag::kBalancing: return "balancing";
        case Tag::kBarriers: return "barriers";
        case Tag::kGCMark: return "gcMark";

        case Tag::kEnumSize: break;
    }
    RuntimeFail("Unexpected logging tag %d", tag);
}

ALWAYS_INLINE inline Level maxLevel(Tag tag, const int32_t logLevels[]) {
    return static_cast<logging::Level>(logLevels[static_cast<int>(tag)]);
}

ALWAYS_INLINE inline bool enabled(logging::Level level, std_support::span<const logging::Tag> tags, const int32_t logLevels[]) {
    for (auto tag: tags) {
        if (level <= maxLevel(tag, logLevels)) {
            return true;
        }
    }
    return false;
}

ALWAYS_INLINE inline bool enabled(logging::Level level, std::initializer_list<const logging::Tag> tags) noexcept {
    std_support::span<const logging::Tag> tagsSpan(std::data(tags), std::size(tags));
    return enabled(level, tagsSpan, compiler::runtimeLogs());
}

class Logger {
public:
    virtual ~Logger() = default;

    virtual void Log(Level level, std_support::span<const Tag> tags, std::string_view message) const noexcept = 0;
};

std::unique_ptr<Logger> CreateStderrLogger() noexcept;

std_support::span<char> FormatLogEntry(
        std_support::span<char> buffer,
        Level level,
        std_support::span<const Tag> tags,
        int threadId,
        kotlin::nanoseconds timestamp,
        const char* format,
        std::va_list args) noexcept;

void Log(
        const Logger& logger,
        Level level,
        std_support::span<const Tag> tags,
        int threadId,
        kotlin::nanoseconds timestamp,
        const char* format,
        std::va_list args) noexcept;

} // namespace internal

void OnRuntimeInit() noexcept;

__attribute__((format(printf, 3, 4)))
void Log(Level level, std::initializer_list<Tag> tags, const char* format, ...) noexcept;
void VLog(Level level, std::initializer_list<Tag> tags, const char* format, std::va_list args) noexcept;

} // namespace logging

// Well known tags.
// These are defined outside of logging namespace for simpler usage.
inline constexpr auto kTagGC = logging::Tag::kGC;
inline constexpr auto kTagMM = logging::Tag::kMM;
inline constexpr auto kTagTLS = logging::Tag::kTLS;
inline constexpr auto kTagBalancing = logging::Tag::kBalancing;

} // namespace kotlin

#endif // RUNTIME_LOGGING_H

// Using macros to simplify forwarding of varargs without breaking __attribute__((format)) and to avoid
// evaluating args in `...` if logging is disabled.

#define RuntimeLog(level, tags, format, ...) \
    do { \
        if (::kotlin::logging::internal::enabled(level, tags)) { \
            ::kotlin::logging::Log(level, tags, format, ##__VA_ARGS__); \
        } \
    } while (false)

#define RuntimeVLog(level, tags, format, args) \
    do { \
        if (::kotlin::logging::internal::enabled(level, tags)) { \
            ::kotlin::logging::VLog(level, tags, format, args); \
        } \
    } while (false)

#define RuntimeLogDebug(tags, format, ...) RuntimeLog(::kotlin::logging::Level::kDebug, tags, format, ##__VA_ARGS__)
#define RuntimeLogInfo(tags, format, ...) RuntimeLog(::kotlin::logging::Level::kInfo, tags, format, ##__VA_ARGS__)
#define RuntimeLogWarning(tags, format, ...) RuntimeLog(::kotlin::logging::Level::kWarning, tags, format, ##__VA_ARGS__)
#define RuntimeLogError(tags, format, ...) RuntimeLog(::kotlin::logging::Level::kError, tags, format, ##__VA_ARGS__)

#define RuntimeVLogDebug(tags, format, args) RuntimeVLog(::kotlin::logging::Level::kDebug, tags, format, args)
#define RuntimeVLogInfo(tags, format, args) RuntimeVLog(::kotlin::logging::Level::kInfo, tags, format, args)
#define RuntimeVLogWarning(tags, format, args) RuntimeVLog(::kotlin::logging::Level::kWarning, tags, format, args)
#define RuntimeVLogError(tags, format, args) RuntimeVLog(::kotlin::logging::Level::kError, tags, format, args)
