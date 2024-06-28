/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "Logging.hpp"

#include <vector>

#include "gmock/gmock.h"
#include "gtest/gtest.h"

using namespace kotlin;

using ::testing::_;

namespace {

std_support::span<char> FormatLogEntry(
        std_support::span<char> buffer,
        logging::Level level,
        std::initializer_list<logging::Tag> tags,
        int threadId,
        kotlin::nanoseconds timestamp,
        const char* format,
        ...) {
    std_support::span<const logging::Tag> tagsSpan(std::data(tags), std::size(tags));
    std::va_list args;
    va_start(args, format);
    auto result = logging::internal::FormatLogEntry(buffer, level, tagsSpan, threadId, timestamp, format, args);
    va_end(args);
    return result;
}

} // namespace

TEST(LoggingTest, FormatLogEntry_Debug_OneTag) {
    std::array<char, 1024> buffer;
    FormatLogEntry(buffer, logging::Level::kDebug, {logging::Tag::kRT}, 123, kotlin::nanoseconds(42'500'000'000), "Log #%d", 42);
    EXPECT_THAT(buffer.data(), testing::StrEq("[DEBUG][rt][tid#123][42.500s] Log #42\n"));
}

TEST(LoggingTest, FormatLogEntry_Debug_TwoTags) {
    std::array<char, 1024> buffer;
    FormatLogEntry(buffer, logging::Level::kDebug, {logging::Tag::kRT, logging::Tag::kGC}, 123, kotlin::nanoseconds(42'500'000'000), "Log #%d", 42);
    EXPECT_THAT(buffer.data(), testing::StrEq("[DEBUG][rt,gc][tid#123][42.500s] Log #42\n"));
}

TEST(LoggingTest, FormatLogEntry_Info_OneTag) {
    std::array<char, 1024> buffer;
    FormatLogEntry(buffer, logging::Level::kInfo, {logging::Tag::kRT}, 123, kotlin::nanoseconds(42'500'000'000), "Log #%d", 42);
    EXPECT_THAT(buffer.data(), testing::StrEq("[INFO][rt][tid#123][42.500s] Log #42\n"));
}

TEST(LoggingTest, FormatLogEntry_Info_TwoTags) {
    std::array<char, 1024> buffer;
    FormatLogEntry(buffer, logging::Level::kInfo, {logging::Tag::kRT, logging::Tag::kGC}, 123, kotlin::nanoseconds(42'500'000'000), "Log #%d", 42);
    EXPECT_THAT(buffer.data(), testing::StrEq("[INFO][rt,gc][tid#123][42.500s] Log #42\n"));
}

TEST(LoggingTest, FormatLogEntry_Warning_OneTag) {
    std::array<char, 1024> buffer;
    FormatLogEntry(buffer, logging::Level::kWarning, {logging::Tag::kRT}, 123, kotlin::nanoseconds(42'500'000'000), "Log #%d", 42);
    EXPECT_THAT(buffer.data(), testing::StrEq("[WARNING][rt][tid#123][42.500s] Log #42\n"));
}

TEST(LoggingTest, FormatLogEntry_Warning_TwoTags) {
    std::array<char, 1024> buffer;
    FormatLogEntry(buffer, logging::Level::kWarning, {logging::Tag::kRT, logging::Tag::kGC}, 123, kotlin::nanoseconds(42'500'000'000), "Log #%d", 42);
    EXPECT_THAT(buffer.data(), testing::StrEq("[WARNING][rt,gc][tid#123][42.500s] Log #42\n"));
}

TEST(LoggingTest, FormatLogEntry_Error_OneTag) {
    std::array<char, 1024> buffer;
    FormatLogEntry(buffer, logging::Level::kError, {logging::Tag::kRT}, 123, kotlin::nanoseconds(42'500'000'000), "Log #%d", 42);
    EXPECT_THAT(buffer.data(), testing::StrEq("[ERROR][rt][tid#123][42.500s] Log #42\n"));
}

TEST(LoggingTest, FormatLogEntry_Error_TwoTags) {
    std::array<char, 1024> buffer;
    FormatLogEntry(buffer, logging::Level::kError, {logging::Tag::kRT, logging::Tag::kGC}, 123, kotlin::nanoseconds(42'500'000'000), "Log #%d", 42);
    EXPECT_THAT(buffer.data(), testing::StrEq("[ERROR][rt,gc][tid#123][42.500s] Log #42\n"));
}

TEST(LoggingTest, FormatLogEntry_Overflow) {
    std::array<char, 20> buffer;
    FormatLogEntry(buffer, logging::Level::kError, {logging::Tag::kRT, logging::Tag::kGC}, 123, kotlin::nanoseconds(42'500'000'000), "Log #%d", 42);
    // Only 18 characters are used for the log string contents, another 2 are \n and \0.
    EXPECT_THAT(buffer.data(), testing::StrEq("[ERROR][rt,gc][tid\n"));
}

TEST(LoggingDeathTest, StderrLogger) {
    auto logger = logging::internal::CreateStderrLogger();
    EXPECT_DEATH(
            {
                logger->Log(logging::Level::kInfo, {}, "Message for the log");
                std::abort();
            },
            "Message for the log");
}

namespace {

class LogFilter {
public:
    explicit LogFilter(std::map<logging::Tag, logging::Level> tagToLevel) {
        logLevels_.resize(static_cast<size_t>(logging::Tag::kEnumSize));
        for (auto [tag, level] : tagToLevel) {
            logLevels_[static_cast<size_t>(tag)] = static_cast<int32_t>(level);
        }
    }

    bool Empty() const {
        return std::all_of(logLevels_.begin(), logLevels_.end(), [](int32_t levelOrd){
            return static_cast<logging::Level>(levelOrd) == logging::Level::kNone;
        });
    }

    bool Enabled(logging::Level level, std::initializer_list<logging::Tag> tags) const {
        std_support::span<const logging::Tag> tagsSpan(std::data(tags), std::size(tags));
        return logging::internal::enabled(level, tagsSpan, logLevels_.data());
    }

private:
    std::vector<int32_t> logLevels_;
};

}

TEST(LoggingTest, LogFilter_EnableOne) {
    LogFilter filter({{logging::Tag::kRT, logging::Level::kInfo}});
    EXPECT_FALSE(filter.Empty());

    EXPECT_FALSE(filter.Enabled(logging::Level::kDebug, {logging::Tag::kRT}));
    EXPECT_TRUE(filter.Enabled(logging::Level::kInfo, {logging::Tag::kRT}));
    EXPECT_TRUE(filter.Enabled(logging::Level::kWarning, {logging::Tag::kRT}));
    EXPECT_TRUE(filter.Enabled(logging::Level::kError, {logging::Tag::kRT}));

    EXPECT_FALSE(filter.Enabled(logging::Level::kDebug, {logging::Tag::kGC}));
    EXPECT_FALSE(filter.Enabled(logging::Level::kInfo, {logging::Tag::kGC}));
    EXPECT_FALSE(filter.Enabled(logging::Level::kWarning, {logging::Tag::kGC}));
    EXPECT_FALSE(filter.Enabled(logging::Level::kError, {logging::Tag::kGC}));

    EXPECT_FALSE(filter.Enabled(logging::Level::kDebug, {logging::Tag::kRT, logging::Tag::kGC}));
    EXPECT_TRUE(filter.Enabled(logging::Level::kInfo, {logging::Tag::kRT, logging::Tag::kGC}));
    EXPECT_TRUE(filter.Enabled(logging::Level::kWarning, {logging::Tag::kRT, logging::Tag::kGC}));
    EXPECT_TRUE(filter.Enabled(logging::Level::kError, {logging::Tag::kRT, logging::Tag::kGC}));

    EXPECT_FALSE(filter.Enabled(logging::Level::kDebug, {logging::Tag::kGC, logging::Tag::kRT}));
    EXPECT_TRUE(filter.Enabled(logging::Level::kInfo, {logging::Tag::kGC, logging::Tag::kRT}));
    EXPECT_TRUE(filter.Enabled(logging::Level::kWarning, {logging::Tag::kGC, logging::Tag::kRT}));
    EXPECT_TRUE(filter.Enabled(logging::Level::kError, {logging::Tag::kGC, logging::Tag::kRT}));
}

TEST(LoggingTest, LogFilter_EnableTwo) {
    LogFilter filter({{logging::Tag::kRT, logging::Level::kInfo}, {logging::Tag::kGC, logging::Level::kWarning}});
    EXPECT_FALSE(filter.Empty());

    EXPECT_FALSE(filter.Enabled(logging::Level::kDebug, {logging::Tag::kRT}));
    EXPECT_TRUE(filter.Enabled(logging::Level::kInfo, {logging::Tag::kRT}));
    EXPECT_TRUE(filter.Enabled(logging::Level::kWarning, {logging::Tag::kRT}));
    EXPECT_TRUE(filter.Enabled(logging::Level::kError, {logging::Tag::kRT}));

    EXPECT_FALSE(filter.Enabled(logging::Level::kDebug, {logging::Tag::kGC}));
    EXPECT_FALSE(filter.Enabled(logging::Level::kInfo, {logging::Tag::kGC}));
    EXPECT_TRUE(filter.Enabled(logging::Level::kWarning, {logging::Tag::kGC}));
    EXPECT_TRUE(filter.Enabled(logging::Level::kError, {logging::Tag::kGC}));

    EXPECT_FALSE(filter.Enabled(logging::Level::kDebug, {logging::Tag::kRT, logging::Tag::kGC}));
    EXPECT_TRUE(filter.Enabled(logging::Level::kInfo, {logging::Tag::kRT, logging::Tag::kGC}));
    EXPECT_TRUE(filter.Enabled(logging::Level::kWarning, {logging::Tag::kRT, logging::Tag::kGC}));
    EXPECT_TRUE(filter.Enabled(logging::Level::kError, {logging::Tag::kRT, logging::Tag::kGC}));

    EXPECT_FALSE(filter.Enabled(logging::Level::kDebug, {logging::Tag::kGC, logging::Tag::kRT}));
    EXPECT_TRUE(filter.Enabled(logging::Level::kInfo, {logging::Tag::kGC, logging::Tag::kRT}));
    EXPECT_TRUE(filter.Enabled(logging::Level::kWarning, {logging::Tag::kGC, logging::Tag::kRT}));
    EXPECT_TRUE(filter.Enabled(logging::Level::kError, {logging::Tag::kGC, logging::Tag::kRT}));
}
