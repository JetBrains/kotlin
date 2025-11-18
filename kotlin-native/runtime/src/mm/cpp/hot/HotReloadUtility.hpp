//
// Created by Gabriele.Pappalardo on 27/07/2025.
//

#ifndef HOTRELOADUTILITY_HPP
#define HOTRELOADUTILITY_HPP

#include <string>
#include <sstream>

#include <TypeInfo.h>
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
