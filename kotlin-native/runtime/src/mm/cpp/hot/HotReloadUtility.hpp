//
// Created by Gabriele.Pappalardo on 27/07/2025.
//

#ifndef HOTRELOADUTILITY_HPP
#define HOTRELOADUTILITY_HPP

#include <string>
#include <fstream>
#include <sstream>

#include <TypeInfo.h>

#define LOG_FILENAME "/tmp/kn_hot_reload.log"
#define ENV_LOG_PARAM "HOT_RELOAD_LOG"

namespace kotlin::hot::utility {

inline static constexpr const char* kTypeNames[] = {
        "__Invalid",
        "kotlin.Any",
        "kotlin.Char",
        "kotlin.Short",
        "kotlin.Int",
        "kotlin.Long",
        "kotlin.Float",
        "kotlin.Double",
        "kotlin.native.internal.NativePtr",
        "kotlin.Boolean",
        "kotlinx.cinterop.Vector128"};

inline static constexpr int kRuntimeTypeSize[] = {
        -1, // INVALID
        sizeof(ObjHeader*), // OBJECT
        1, // INT8
        2, // INT16
        4, // INT32
        8, // INT64
        4, // FLOAT32
        8, // FLOAT64
        sizeof(void*), // NATIVE_PTR
        1, // BOOLEAN
        16 // VECTOR128
};

/**
 * Defines logging levels for the hot reload system using bit flags.
 * Multiple log levels can be combined using bitwise operations.
 * Example: DEBUG + INFO = 6 (00110 in binary)
 */
enum class LogLevel : uint8_t {
    NONE = 0, ///< Disable all logging
    DEBUG = 1 << 1, ///< Debug level logging (2)
    INFO = 1 << 2, ///< Information level logging (4)
    WARN = 1 << 3, ///< Warning level logging (8)
    ERR = 1 << 4 ///< Error level logging (16)
};

static auto CurrentLogLevel = LogLevel::NONE;

inline void log(const std::string& message, const LogLevel level = LogLevel::INFO) {
    // if (!(static_cast<uint8_t>(CurrentLogLevel) & static_cast<uint8_t>(level)))
    //     return;

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
        case LogLevel::ERR:
            levelStr = "ERROR";
            break;
        default:
            levelStr = "UNKNW";
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

inline void initializeHotReloadLogs() {
    const char* logEnabledEnv = std::getenv(ENV_LOG_PARAM);
    if (logEnabledEnv == nullptr) return;
    const auto level = static_cast<LogLevel>(strtol(logEnabledEnv, nullptr, 10));
    CurrentLogLevel = level;
}

/// Assuming the className is a fully-qualified-name
inline std::string kotlinClassNameAsTypeInfoName(const std::string& className) {
    return "kclass:" + className;
}

inline std::string field2String(const char* fieldName, const uint8_t* fieldValue, const Konan_RuntimeType fieldType) {
    std::stringstream ss;
    ss << fieldName << ":" << kTypeNames[fieldType] << " = ";

    switch (fieldType) {
        case RT_INVALID:
            ss << "???";
            break;
        case RT_OBJECT:
            ss << "ObjHeader*";
            break;
        case RT_INT8:
            ss << *(reinterpret_cast<const int8_t*>(fieldValue));
            break;
        case RT_INT16:
            ss << *(reinterpret_cast<const int16_t*>(fieldValue));
            break;
        case RT_INT32:
            ss << *(reinterpret_cast<const int32_t*>(fieldValue));
            break;
        case RT_INT64:
            ss << *(reinterpret_cast<const int64_t*>(fieldValue));
            break;
        case RT_FLOAT32:
            ss << *(reinterpret_cast<const float*>(fieldValue));
            break;
        case RT_FLOAT64:
            ss << *(reinterpret_cast<const double*>(fieldValue));
            break;
        case RT_NATIVE_PTR:
            ss << *(reinterpret_cast<const uintptr_t*>(fieldValue));
            break;
        case RT_BOOLEAN:
            ss << *(reinterpret_cast<const bool*>(fieldValue));
            break;
        case RT_VECTOR128:
            ss << "vec128";
            break;
    }

    return ss.str();
}

}; // namespace kotlin::hot::utility

#endif // HOTRELOADUTILITY_HPP
