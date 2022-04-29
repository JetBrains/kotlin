/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "Logging.hpp"

#include <array>
#if __has_include(<optional>)
#include <optional>
#elif __has_include(<experimental/optional>)
// TODO: Remove when wasm32 is gone.
#include <experimental/optional>
namespace std {
template <typename T>
using optional = std::experimental::optional<T>;
inline constexpr auto nullopt = std::experimental::nullopt;
} // namespace std
#else
#error "No <optional>"
#endif

#include "Format.h"
#include "KAssert.h"
#include "Porting.h"
#include "std_support/Map.hpp"
#include "std_support/String.hpp"

using namespace kotlin;

namespace {

template <typename T>
struct ParseResult {
    std::optional<T> value;
    std::string_view rest;
};

ParseResult<std::string_view> ParseTag(std::string_view input) noexcept {
    auto position = input.find('=');
    if (position == std::string_view::npos || position == 0) {
        return {std::nullopt, input};
    }
    return {input.substr(0, position), input.substr(position + 1)};
}

ParseResult<std::string_view> ParseLevelString(std::string_view input) noexcept {
    auto position = input.find(',');
    if (position == 0) {
        return {std::nullopt, input};
    }
    if (position == std::string_view::npos) {
        return {input, std::string_view()};
    }
    return {input.substr(0, position), input.substr(position + 1)};
}

std::optional<logging::Level> ParseLevel(std::string_view levelString) noexcept {
    if (levelString == "debug") return logging::Level::kDebug;
    if (levelString == "info") return logging::Level::kInfo;
    if (levelString == "warning") return logging::Level::kWarning;
    if (levelString == "error") return logging::Level::kError;
    return std::nullopt;
}

std_support::map<std_support::string, logging::Level> ParseTagsFilter(std::string_view tagsFilter) noexcept {
    if (tagsFilter.empty()) return {};
    std_support::map<std_support::string, logging::Level> result;
    std::string_view rest = tagsFilter;
    while (!rest.empty()) {
        auto tag = ParseTag(rest);
        rest = tag.rest;
        if (tag.value == std::nullopt) {
            konan::consoleErrorf("Failed to parse tag at: '");
            konan::consoleErrorUtf8(rest.data(), rest.size());
            konan::consoleErrorf("'. No logging will be performed\n");
            return {};
        }
        auto levelString = ParseLevelString(rest);
        rest = levelString.rest;
        auto level = levelString.value ? ParseLevel(*levelString.value) : std::nullopt;
        if (level == std::nullopt) {
            konan::consoleErrorf("Failed to parse level at: '");
            konan::consoleErrorUtf8(rest.data(), rest.size());
            konan::consoleErrorf("'. No logging will be performed\n");
            return {};
        }
        result.emplace(std_support::string(tag.value->data(), tag.value->size()), *level);
    }
    return result;
}

class LogFilter : public logging::internal::LogFilter {
public:
    explicit LogFilter(std::string_view tagsFilter) noexcept : tagLevelMap_(ParseTagsFilter(tagsFilter)) {}

    bool Empty() const noexcept override { return tagLevelMap_.empty(); }

    bool Enabled(logging::Level level, std_support::span<const char* const> tags) const noexcept override {
        for (auto tag : tags) {
            auto it = tagLevelMap_.find(tag);
            if (it != tagLevelMap_.end()) {
                if (it->second <= level) {
                    return true;
                }
            }
        }
        return false;
    }

private:
    // TODO: Make it more efficient.
    std_support::map<std_support::string, logging::Level> tagLevelMap_;
};

class StderrLogger : public logging::internal::Logger {
public:
    void Log(logging::Level level, std_support::span<const char* const> tags, std::string_view message) const noexcept override {
        konan::consoleErrorUtf8(message.data(), message.size());
    }
};

std_support::span<char> FormatLevel(std_support::span<char> buffer, logging::Level level) noexcept {
    switch (level) {
        case logging::Level::kDebug:
            return FormatToSpan(buffer, "[DEBUG]");
        case logging::Level::kInfo:
            return FormatToSpan(buffer, "[INFO]");
        case logging::Level::kWarning:
            return FormatToSpan(buffer, "[WARN]");
        case logging::Level::kError:
            return FormatToSpan(buffer, "[ERROR]");
    }
}

std_support::span<char> FormatTags(std_support::span<char> buffer, std_support::span<const char* const> tags) noexcept {
    // `tags` cannot be empty.
    auto firstTag = tags.front();
    buffer = FormatToSpan(buffer, "[%s", firstTag);
    for (auto tag : tags.subspan(1)) {
        buffer = FormatToSpan(buffer, ",%s", tag);
    }
    return FormatToSpan(buffer, "]");
}

} // namespace

std_support::unique_ptr<logging::internal::LogFilter> logging::internal::CreateLogFilter(std::string_view tagsFilter) noexcept {
    return std_support::make_unique<::LogFilter>(tagsFilter);
}

std_support::unique_ptr<logging::internal::Logger> logging::internal::CreateStderrLogger() noexcept {
    return std_support::make_unique<StderrLogger>();
}

std_support::span<char> logging::internal::FormatLogEntry(
        std_support::span<char> buffer,
        logging::Level level,
        std_support::span<const char* const> tags,
        const char* format,
        std::va_list args) noexcept {
    auto subbuffer = buffer.subspan(0, buffer.size() - 1);
    subbuffer = FormatLevel(subbuffer, level);
    subbuffer = FormatTags(subbuffer, tags);
    subbuffer = FormatToSpan(subbuffer, " ");
    subbuffer = VFormatToSpan(subbuffer, format, args);
    buffer = buffer.subspan(subbuffer.data() - buffer.data());
    buffer = FormatToSpan(buffer, "\n");
    return buffer;
}

void logging::internal::Log(
        const LogFilter& logFilter,
        const Logger& logger,
        Level level,
        std_support::span<const char* const> tags,
        const char* format,
        std::va_list args) noexcept {
    if (!logFilter.Enabled(level, tags)) return;
    // TODO: This might be suboptimal.
    std::array<char, 1024> logEntry;
    auto rest = FormatLogEntry(logEntry, level, tags, format, args);
    logger.Log(level, tags, std::string_view(logEntry.data(), rest.data() - logEntry.data()));
}

void logging::Log(Level level, std::initializer_list<const char*> tags, const char* format, ...) noexcept {
    std::va_list args;
    va_start(args, format);
    VLog(level, tags, format, args);
    va_end(args);
}

void logging::VLog(Level level, std::initializer_list<const char*> tags, const char* format, std::va_list args) noexcept {
    [[clang::no_destroy]] static auto logFilter = internal::CreateLogFilter(compiler::runtimeLogs());
    [[clang::no_destroy]] static auto logger = internal::CreateStderrLogger();
    RuntimeAssert(tags.size() > 0, "Cannot Log without tags");
    std_support::span<const char* const> tagsSpan(std::data(tags), std::size(tags));
    internal::Log(*logFilter, *logger, level, tagsSpan, format, args);
}
