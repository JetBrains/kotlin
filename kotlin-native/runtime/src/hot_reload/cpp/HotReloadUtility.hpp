//
// Created by Gabriele.Pappalardo on 27/07/2025.
//

#ifndef HOTRELOADUTILITY_HPP
#define HOTRELOADUTILITY_HPP

#ifdef KONAN_HOT_RELOAD

#include <string>
#include <chrono>
#include <Logging.hpp>

#define HRLogInfo(format, ...) RuntimeLogInfo({kotlin::kTagHotReloader}, format, ##__VA_ARGS__)
#define HRLogDebug(format, ...) RuntimeLogDebug({kotlin::kTagHotReloader}, format, ##__VA_ARGS__)
#define HRLogWarning(format, ...) RuntimeLogWarning({kotlin::kTagHotReloader}, format, ##__VA_ARGS__)
#define HRLogError(format, ...) RuntimeLogError({kotlin::kTagHotReloader}, format, ##__VA_ARGS__)

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

inline uint64_t getCurrentEpoch() {
    return std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch()).count();
}

enum class ReferenceOrigin { Global, ShadowStack, ObjRef };

inline const char* referenceOriginToString(const ReferenceOrigin origin) noexcept {
    switch (origin) {
        case ReferenceOrigin::Global:
            return "Global";
        case ReferenceOrigin::ShadowStack:
            return "ShadowStack";
        case ReferenceOrigin::ObjRef:
            return "Object Reference";
        default:
            return "Unknown";
    }
}

}; // namespace kotlin::hot::utility

#endif

#endif // HOTRELOADUTILITY_HPP
