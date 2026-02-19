/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "Logging.hpp"

#include <array>
#include <cinttypes>
#include <map>
#include <optional>
#include <string>

#include "CallsChecker.hpp"
#include "Format.h"
#include "KAssert.h"
#include "Porting.h"

using namespace kotlin;

namespace {

class StderrLogger : public logging::internal::Logger {
public:
    void Log(logging::Level level, std_support::span<const logging::Tag> tags, std::string_view message) const noexcept override {
        konan::consoleErrorUtf8(message.data(), message.size());
    }
};

std_support::span<char> FormatLevel(std_support::span<char> buffer, logging::Level level) noexcept {
    return FormatToSpan(buffer, "[%s]", logging::internal::name(level));
}

std_support::span<char> FormatTags(std_support::span<char> buffer, std_support::span<const logging::Tag> tags) noexcept {
    // `tags` cannot be empty.
    auto firstTag = tags.front();
    buffer = FormatToSpan(buffer, "[%s", logging::internal::name(firstTag));
    for (auto tag : tags.subspan(1)) {
        buffer = FormatToSpan(buffer, ",%s", logging::internal::name(tag));
    }
    return FormatToSpan(buffer, "]");
}

std_support::span<char> FormatTimestamp(std_support::span<char> buffer, kotlin::nanoseconds timestamp) noexcept {
    auto s = static_cast<double>(timestamp.count().value) / 1'000'000'000;
    return FormatToSpan(buffer, "[%.3fs]", s);
}

std_support::span<char> FormatThread(std_support::span<char> buffer, int threadId) noexcept {
    return FormatToSpan(buffer, "[tid#%d]", threadId);
}

struct DefaultLogContext {
    StderrLogger logger;
    kotlin::steady_clock::time_point initialTimestamp = kotlin::steady_clock::now();
};

} // namespace

std::unique_ptr<logging::internal::Logger> logging::internal::CreateStderrLogger() noexcept {
    return std::make_unique<StderrLogger>();
}

std_support::span<char> logging::internal::FormatLogEntry(
        std_support::span<char> buffer,
        logging::Level level,
        std_support::span<const logging::Tag> tags,
        int threadId,
        kotlin::nanoseconds timestamp,
        const char* format,
        std::va_list args) noexcept {
    auto subbuffer = buffer.subspan(0, buffer.size() - 1);
    subbuffer = FormatLevel(subbuffer, level);
    subbuffer = FormatTags(subbuffer, tags);
    subbuffer = FormatThread(subbuffer, threadId);
    subbuffer = FormatTimestamp(subbuffer, timestamp);
    subbuffer = FormatToSpan(subbuffer, " ");
    subbuffer = VFormatToSpan(subbuffer, format, args);
    buffer = buffer.subspan(subbuffer.data() - buffer.data());
    buffer = FormatToSpan(buffer, "\n");
    return buffer;
}

void logging::internal::Log(
        const Logger& logger,
        Level level,
        std_support::span<const logging::Tag> tags,
        int threadId,
        kotlin::nanoseconds timestamp,
        const char* format,
        std::va_list args) noexcept {
    RuntimeAssert(enabled(level, tags, compiler::runtimeLogs()), "Caller must ensure that the logging requested is enabled");
    // TODO: This might be suboptimal.
    std::array<char, 1024> logEntry;
    auto rest = FormatLogEntry(logEntry, level, tags, threadId, timestamp, format, args);
    logger.Log(level, tags, std::string_view(logEntry.data(), rest.data() - logEntry.data()));
}

void logging::OnRuntimeInit() noexcept {
    if (internal::enabled(Level::kInfo, {Tag::kLogging})) {
        std::array<char, 1024> buf;
        std_support::span<char> span = buf;
        bool printedFirstTag = false;
        for (size_t tagOrd = 0; tagOrd < static_cast<std::size_t>(Tag::kEnumSize); ++tagOrd) {
            auto tag = static_cast<Tag>(tagOrd);
            auto maxLevel = internal::maxLevel(tag, compiler::runtimeLogs());
            if (maxLevel > Level::kNone) {
                if (printedFirstTag) {
                    span = FormatToSpan(span, ", ");
                }
                printedFirstTag = true;
                span = FormatToSpan(span, "%s = %s", internal::name(tag), internal::name(maxLevel));
            }
        }
        RuntimeAssert(printedFirstTag, "At least logging=info must be enabled and printed");
        Log(Level::kInfo, {logging::Tag::kLogging}, "Logging enabled for: %s", buf.data());
    }
}

void logging::Log(Level level, std::initializer_list<logging::Tag> tags, const char* format, ...) noexcept {
    std::va_list args;
    va_start(args, format);
    VLog(level, tags, format, args);
    va_end(args);
}

void logging::VLog(Level level, std::initializer_list<logging::Tag> tags, const char* format, std::va_list args) noexcept {
    CallsCheckerIgnoreGuard guard;

    [[clang::no_destroy]] static DefaultLogContext ctx;
    RuntimeAssert(tags.size() > 0, "Cannot Log without tags");
    std_support::span<const logging::Tag> tagsSpan(std::data(tags), std::size(tags));
    auto threadId = konan::currentThreadId();
    auto timestamp = kotlin::steady_clock::now();
    internal::Log(ctx.logger, level, tagsSpan, threadId, timestamp - ctx.initialTimestamp, format, args);
}
