/**
* Copyright 2010-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
*/

#ifndef HOTRELOADUTILITY_HPP
#define HOTRELOADUTILITY_HPP

#include <chrono>
#include <cstdint>
#include <string>
#include <string_view>
#include <vector>

#include <Memory.h>
#include <Logging.hpp>

#define HRLogInfo(format, ...) RuntimeLogInfo({kotlin::kTagHotReloader}, format, ##__VA_ARGS__)
#define HRLogDebug(format, ...) RuntimeLogDebug({kotlin::kTagHotReloader}, format, ##__VA_ARGS__)
#define HRLogWarning(format, ...) RuntimeLogWarning({kotlin::kTagHotReloader}, format, ##__VA_ARGS__)
#define HRLogError(format, ...) RuntimeLogError({kotlin::kTagHotReloader}, format, ##__VA_ARGS__)

namespace kotlin::hot {

struct RequestTimings {
    int64_t parseNs = 0;
};

struct ReloadRequest {
    std::vector<std::string> objectPaths;
    RequestTimings timings;
};

struct ReloadTimings {
    int64_t loadNs = 0;
    int64_t stubsNs = 0;
    int64_t redirectNs = 0;
    int64_t stateTransferNs = 0;
    int64_t stwWaitNs = 0;
    int64_t totalNs = 0;
};

inline int64_t nanosecondsSince(const std::chrono::steady_clock::time_point t0) {
    return std::chrono::duration_cast<std::chrono::nanoseconds>(std::chrono::steady_clock::now() - t0).count();
}

template <typename Duration = std::chrono::nanoseconds>
class ScopeTimer {
public:
    ScopeTimer(const std::string_view label, int64_t& sink) noexcept
        : label_(label), sink_(sink), t0_(std::chrono::steady_clock::now()) {}

    ~ScopeTimer() {
        const int64_t elapsed = std::chrono::duration_cast<Duration>(
                std::chrono::steady_clock::now() - t0_).count();
        sink_ += elapsed;
        HRLogDebug("[timer] %.*s: %lld", static_cast<int>(label_.size()), label_.data(), static_cast<long long>(elapsed));
    }

    ScopeTimer(const ScopeTimer&) = delete;
    ScopeTimer& operator=(const ScopeTimer&) = delete;
    ScopeTimer(ScopeTimer&&) = delete;
    ScopeTimer& operator=(ScopeTimer&&) = delete;

private:
    std::string_view label_;
    int64_t& sink_;
    std::chrono::steady_clock::time_point t0_;
};

} // namespace kotlin::hot

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

inline static constexpr uint8_t kRuntimeTypeSize[] = {
        0, // INVALID
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

template <typename Duration = std::chrono::milliseconds>
inline typename Duration::rep currentEpoch() {
    return std::chrono::duration_cast<Duration>(std::chrono::system_clock::now().time_since_epoch()).count();
}

}; // namespace kotlin::hot::utility

#endif // HOTRELOADUTILITY_HPP
