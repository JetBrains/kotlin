//
// Created by Gabriele.Pappalardo on 27/07/2025.
//

#ifndef HOTRELOADUTILITY_HPP
#define HOTRELOADUTILITY_HPP

#include <cstdlib>
#include <string>
#include <fstream>
#include <sstream>

#define LOG_FILENAME "/tmp/kn_hot_reload.log"
#define ENV_LOG_PARAM "HOT_RELOAD_LOG"

namespace kotlin::hot::utility {

enum class LogLevel { DEBUG, INFO, WARN, ERROR };

static bool IsLogEnabled = false;

static void log(const std::string& message, const LogLevel level = LogLevel::INFO) {
    if (!IsLogEnabled) return;

    auto now = std::chrono::system_clock::now();
    auto timestamp = std::chrono::system_clock::to_time_t(now);
    std::stringstream ss;
    ss << std::put_time(std::localtime(&timestamp), "%Y-%m-%d %H:%M:%S");

    const char* levelStr;
    switch (level) {
        case LogLevel::DEBUG:
            levelStr = "DEBUG";
            break;
        case LogLevel::INFO:
            levelStr = "INFO";
            break;
        case LogLevel::WARN:
            levelStr = "WARN";
            break;
        case LogLevel::ERROR:
            levelStr = "ERROR";
            break;
    }

    std::string logMessage = "[" + ss.str() + "][kn-hot-reload][" + levelStr + "] :: " + message + "\n";
    std::cout << logMessage;

    std::ofstream logFile;
    logFile.open(LOG_FILENAME, std::ios::app);
    if (logFile.is_open()) {
        logFile << logMessage;
        logFile.close();
    }
}

static void InitializeHotReloadLogs() {
    if (const char* isLogEnabledEnv = std::getenv(ENV_LOG_PARAM); isLogEnabledEnv != nullptr) {
        const auto isLogEnabled = std::strtol(isLogEnabledEnv, nullptr, 10) == 1L;
        IsLogEnabled = isLogEnabled;
    }
}

/// Assuming the className is a fully-qualified-name
inline std::string KotlinClassNameAsTypeInfoName(const std::string& className) {
    return "kclass:" + className;
}

}


#endif //HOTRELOADUTILITY_HPP
