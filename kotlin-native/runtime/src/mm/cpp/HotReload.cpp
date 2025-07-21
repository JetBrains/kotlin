#include <iostream>
#include <unordered_set>
#include <queue>
#include <iomanip>
#include <chrono>
#include <fstream>
#include <sstream>
#include <cstdint>

#include "Memory.h"
#include "Natives.h"
#include "GlobalData.hpp"
#include "ReferenceOps.hpp"
#include "ThreadRegistry.hpp"
#include "ShadowStack.hpp"
#include "ThreadData.hpp"
#include "RootSet.hpp"

#include "HotReload.hpp"

#include "ObjectTraversal.hpp"

#define ENV_LOG_PARAM "HOT_RELOAD_LOG"

using namespace kotlin;
namespace kotlin::hot::utility {

static constexpr const char* TYPE_NAMES[] = {
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

static constexpr int RUNTIME_TYPE_SIZE[] = {
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

enum class LogLevel { DEBUG, INFO, WARNING, ERROR };

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
        case LogLevel::WARNING:
            levelStr = "WARN";
            break;
        case LogLevel::ERROR:
            levelStr = "ERROR";
            break;
    }

    std::string logMessage = "[" + ss.str() + "][kn-hot-reload][" + levelStr + "] :: " + message + "\n";
    std::cout << logMessage;

    std::ofstream logFile;
    logFile.open("/tmp/kn_hot_reload.log", std::ios::app);
    if (logFile.is_open()) {
        logFile << logMessage;
        logFile.close();
    }
}

void PrintFieldValue(const char* fieldName, const uint8_t* fieldValue, const Konan_RuntimeType fieldType) {
    std::cout << "[info] ::\t" << fieldName << ":" << TYPE_NAMES[fieldType] << " = ";
    switch (fieldType) {
        case RT_INVALID:
            std::cout << "invalid";
            break;
        case RT_OBJECT:
            std::cout << "ObjHeader*";
            break;
        case RT_INT8:
            std::cout << *(reinterpret_cast<const int8_t*>(fieldValue));
            break;
        case RT_INT16:
            std::cout << *(reinterpret_cast<const int16_t*>(fieldValue));
            break;
        case RT_INT32:
            std::cout << *(reinterpret_cast<const int32_t*>(fieldValue));
            break;
        case RT_INT64:
            std::cout << *(reinterpret_cast<const int64_t*>(fieldValue));
            break;
        case RT_FLOAT32:
            std::cout << *(reinterpret_cast<const float*>(fieldValue));
            break;
        case RT_FLOAT64:
            std::cout << *(reinterpret_cast<const double*>(fieldValue));
            break;
        case RT_NATIVE_PTR:
            std::cout << *(reinterpret_cast<const uintptr_t*>(fieldValue));
            break;
        case RT_BOOLEAN:
            std::cout << *(reinterpret_cast<const bool*>(fieldValue));
            break;
        case RT_VECTOR128:
            std::cout << "vec128";
            break;
    }

    std::cout << std::endl;
}

}; // namespace kotlin::hot::utility

namespace kotlin::gc {
void stopTheWorld(GCHandle gcHandle, const char* reason) noexcept;
void resumeTheWorld(GCHandle gcHandle) noexcept;
} // namespace kotlin::gc

namespace kotlin::hot {

template <typename F>
void traverseObjectFieldsInternal(ObjHeader* object, F process) noexcept(noexcept(process(std::declval<mm::RefFieldAccessor>()))) {
    const TypeInfo* typeInfo = object->type_info();
    // Only consider arrays of objects, not arrays of primitives.
    if (typeInfo != theArrayTypeInfo) {
        for (int index = 0; index < typeInfo->objOffsetsCount_; index++) {
            auto fieldPtr = reinterpret_cast<ObjHeader**>(reinterpret_cast<uintptr_t>(object) + typeInfo->objOffsets_[index]);
            process(mm::RefFieldAccessor(fieldPtr));
        }
    } else {
        ArrayHeader* array = object->array();
        for (uint32_t index = 0; index < array->count_; index++) {
            process(mm::RefFieldAccessor(ArrayAddressOfElementAt(array, index)));
        }
    }
}

} // namespace kotlin::hot

ManuallyScoped<hot::HotReloader> globalDataInstance{};

enum class Origin { Global, ShadowStack, ObjRef };

template <typename F>
int VisitObjectGraph(ObjHeader* startObject, F processingFunction) {
    // We need to perform a BFS, while ensuring that the world is stopped.
    // Let's collect the root set, and start the graph exploration.
    // At the moment, let's make things simple, and single-threaded (otherwise, well, headaches).
    int32_t updatedObjects{0};

    std::queue<ObjHeader*> objectsToVisit{};
    std::unordered_set<ObjHeader*> visitedObjects{};

    // Let's start collecting the root set
    auto processObject = [&](ObjHeader* obj, Origin origin) {
        const char* originString = "Unknown";
        switch (origin) {
            case Origin::Global:
                originString = "Global";
                break;
            case Origin::ShadowStack:
                originString = "ShadowStack";
                break;
            case Origin::ObjRef:
                originString = "Object Reference";
                break;
            default:
                break;
        }

        if (obj != nullptr) {
            hot::utility::log("processing object of type: " + obj->type_info()->fqName() + " from: " + originString);
        }

        if (const auto visited = visitedObjects.find(obj); visited != visitedObjects.end()) return;
        if (obj == nullptr || isNullOrMarker(obj)) return;

        visitedObjects.insert(obj);
        objectsToVisit.push(obj);
    };

    for (auto& thread : mm::ThreadRegistry::Instance().LockForIter()) {
        auto& shadowStack = thread.shadowStack();
        for (const auto& object : shadowStack) {
            processObject(object, Origin::ShadowStack);
        }
    }

    for (const auto& objRef : mm::GlobalData::Instance().globalsRegistry().LockForIter()) {
        if (objRef != nullptr) {
            processObject(*objRef, Origin::Global);
        }
    }

    processObject(startObject, Origin::ObjRef);

    hot::utility::log("Starting visit with: " + std::to_string(objectsToVisit.size()));

    while (!objectsToVisit.empty()) {
        const auto nextObject = objectsToVisit.front();
        objectsToVisit.pop();
        processingFunction(nextObject, processObject);
    }

    return updatedObjects;
}

void Kotlin_native_internal_HotReload_perform(ObjHeader* obj) {
    hot::HotReloader::Instance().Perform(obj);
}

ObjHeader* Kotlin_native_internal_HotReload_forceReloadOf(ObjHeader* /*ignored*/, void* oldTypeInfo, void* newTypeInfo) {
    struct UnnamedConstant {
        ObjHeader header;
        ObjHeader* klazz;
    };
    const auto* uc = static_cast<const UnnamedConstant*>(newTypeInfo);
    const auto newType = uc->klazz->type_info();

    const auto* uz = static_cast<const UnnamedConstant*>(oldTypeInfo);
    const auto oldType = uz->klazz->type_info();

    const uint64_t epoch =
            std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch()).count();
    auto mainGCLock = mm::GlobalData::Instance().gc().gcLock();
    const auto gcHandle = gc::GCHandle::create(epoch);
    // STOP-ZA-WARUDO!
    kotlin::gc::stopTheWorld(gcHandle, "starting hot reloading");

    hot::utility::log("Reloading instances of: " + oldType->fqName());
    auto objectsToReload = hot::HotReloader::Instance().FindObjectsToReload(oldType);

    for (const auto& obj : objectsToReload) {
        const auto newInstance = hot::HotReloader::Instance().StateTransfer(obj, newType);
        hot::HotReloader::Instance().UpdateShadowStackReferences(obj, newInstance);
        hot::HotReloader::Instance().UpdateHeapReferences(obj, newInstance);
    }

    kotlin::gc::resumeTheWorld(gcHandle);

    return nullptr;
}

void hot::HotReloader::Init() noexcept {
    if (const char* isLogEnabledEnv = std::getenv(ENV_LOG_PARAM); isLogEnabledEnv != nullptr) {
        const auto isLogEnabled = std::strtol(isLogEnabledEnv, nullptr, 10) == 1L;
        utility::IsLogEnabled = isLogEnabled;
    }
    utility::log("initializing HotReloader component");
    globalDataInstance.construct();
}

hot::HotReloader& hot::HotReloader::Instance() noexcept {
    return *globalDataInstance;
}

void hot::HotReloader::Perform(ObjHeader* knHotReloaderObject) noexcept {
    // TODO
}

std::string hot::HotReloader::WaitForRecompilation() {
    // TODO
    return "";
}

ObjHeader* hot::HotReloader::StateTransfer(ObjHeader* oldObject, const TypeInfo* newTypeInfo) {
    struct FieldData {
        uint8_t type; // Konan_RuntimeType
        int32_t offset; // Offset in memory
    };

    // We need to access the ThreadData to access the allocator
    // TODO: is it important to understand which thread is allocating the object?
    // TODO: getting the current thread...

    // TODO: well, we need two class tables here (the old and the new one)
    // TODO: at the moment we're not considering inheritance

    const mm::ThreadRegistry& threadRegistry = mm::ThreadRegistry::Instance();
    mm::ThreadData* currentThreadData = threadRegistry.CurrentThreadData();

    ObjHeader* newObject = currentThreadData->allocator().allocateObject(newTypeInfo);
    if (newObject == nullptr) {
        utility::log("allocation of new object failed!?", utility::LogLevel::ERROR);
        return nullptr;
    }

    const auto oldObjectTypeInfo = oldObject->type_info();

    const auto newClassName = newTypeInfo->fqName();
    const auto oldClassName = oldObjectTypeInfo->fqName();

    const ExtendedTypeInfo* oldObjExtendedInfo = oldObjectTypeInfo->extendedInfo_;
    const int32_t oldFieldsCount = oldObjExtendedInfo->fieldsCount_; // How many fields the old objects declared
    const char** oldFieldNames = oldObjExtendedInfo->fieldNames_; // field names are null-terminated
    const int32_t* oldFieldOffsets = oldObjExtendedInfo->fieldOffsets_;
    const uint8_t* oldFieldTypes = oldObjExtendedInfo->fieldTypes_;

    std::unordered_map<std::string, FieldData> oldObjectFields{};

    for (int32_t i = 0; i < oldFieldsCount; i++) {
        std::string fieldName{oldFieldNames[i]};
        FieldData fieldData{};
        fieldData.offset = oldFieldOffsets[i];
        fieldData.type = oldFieldTypes[i];
        oldObjectFields[fieldName] = fieldData;
    }

    const ExtendedTypeInfo* newObjExtendedInfo = newObject->type_info()->extendedInfo_;
    const int32_t newFieldsCount = newObjExtendedInfo->fieldsCount_; // How many fields the old objects declared
    const char** newFieldNames = newObjExtendedInfo->fieldNames_; // field names are null-terminated
    const int32_t* newFieldOffsets = newObjExtendedInfo->fieldOffsets_;
    const uint8_t* newFieldTypes = newObjExtendedInfo->fieldTypes_;

    // const auto oldClassProperties = classTable.getClassProperties(oldClassName);
    // const auto newClassProperties = classTable.getClassProperties(newClassName);

    for (int32_t i = 0; i < newFieldsCount; i++) {
        const std::string newFieldName{newFieldNames[i]};

        const uint8_t newFieldType = newFieldTypes[i];
        const uint8_t newFieldOffset = newFieldOffsets[i];

        if (const auto foundField = oldObjectFields.find(newFieldName); foundField == oldObjectFields.end()) {
            utility::log("field `" + newFieldName + "` is new field in '" + newClassName + "'. It won't be copied.");
            continue;
        }

        // Performs type-checking. Note that this type checking is shallow, i.e.,
        // it does not check the object classes.
        const auto& [oldFieldType, oldFieldOffset] = oldObjectFields[newFieldName];
        if (oldFieldType != newFieldType) {
            std::string msg = "failed type-checking: " + newClassName + "::" + newFieldName + ":" + utility::TYPE_NAMES[newFieldType];
            msg += " != ";
            msg += oldClassName + "::" + newFieldName + ":" + utility::TYPE_NAMES[oldFieldType];
            utility::log(msg);
            continue;
        }

        // Handle Kotlin Objects in a different way, the updates must be notified to the GC
        if (oldFieldType == RT_OBJECT) {
            log("Object references not yet supported...", utility::LogLevel::WARNING);
            // const auto actualNewFieldType = newClassProperties->get().find(newFieldName)->second;
            // const auto actualOldFieldType = oldClassProperties->get().find(newFieldName)->second;
            // if (actualNewFieldType != actualOldFieldType) {
            //     // naive type-checking (not covering typealiases and so on)
            //     std::cout << "[warn] :: object references have different types. ";
            //     std::cout << "old: " << actualOldFieldType << ", new: " << actualNewFieldType << "\n";
            //     continue;
            // }
            //
            // const auto oldFieldLocation = reinterpret_cast<ObjHeader **>(
            //     reinterpret_cast<uint8_t *>(oldObject) + oldFieldOffset
            // );
            // const auto newFieldLocation = reinterpret_cast<ObjHeader **>(
            //     reinterpret_cast<uint8_t *>(newObject) + newFieldOffset
            // );
            //
            // std::cout << "[info] :: copying reference field '" << newFieldName << '\n';
            // std::cout << "[info] :: \t" << newFieldName << ':' << actualNewFieldType << " = ";
            // std::cout << std::showbase << std::hex << *oldFieldLocation << std::dec << std::endl;
            //
            // UpdateHeapRef(newFieldLocation, *oldFieldLocation);
            // *newFieldLocation = *oldFieldLocation; // just copy the reference to the previous object

            continue;
        }

        const auto oldFieldData = reinterpret_cast<uint8_t*>(oldObject) + oldFieldOffset;
        const auto newFieldData = reinterpret_cast<uint8_t*>(newObject) + newFieldOffset;

        utility::log("copying field '" + newFieldName + "'");
        utility::PrintFieldValue(newFieldName.c_str(), oldFieldData, static_cast<Konan_RuntimeType>(oldFieldType));

        std::memcpy(newFieldData, oldFieldData, utility::RUNTIME_TYPE_SIZE[oldFieldType]);
    }

    return newObject;
}

std::vector<ObjHeader*> hot::HotReloader::FindObjectsToReload(const TypeInfo* oldTypeInfo) {
    std::vector<ObjHeader*> existingObjects{};
    std::string oldTypeFqName = oldTypeInfo->fqName();

    VisitObjectGraph(nullptr, [&existingObjects, &oldTypeFqName](ObjHeader* nextObject, auto processObject) {
        // Traverse object references inside class properties
        traverseObjectFieldsInternal(
                nextObject, [&](const mm::RefFieldAccessor& fieldAccessor) { processObject(fieldAccessor.direct(), Origin::ObjRef); });

        if (nextObject->type_info()->fqName() == oldTypeFqName) {
            utility::log("Instance of " + nextObject->type_info()->fqName() + " must change!");
            existingObjects.emplace_back(nextObject);
        }
    });
    return existingObjects;
}

int hot::HotReloader::UpdateHeapReferences(ObjHeader* oldObject, ObjHeader* newObject) const {
    int32_t updatedObjects{0};

    std::queue<ObjHeader*> objectsToVisit{};
    std::unordered_set<ObjHeader*> visitedObjects{};

    // Let's start collecting the root set
    auto processObject = [&](ObjHeader* obj, Origin origin) {
        const char* originString = "Unknown";
        switch (origin) {
            case Origin::Global:
                originString = "Global";
                break;
            case Origin::ShadowStack:
                originString = "ShadowStack";
                break;
            case Origin::ObjRef:
                originString = "Object Reference";
                break;
            default:
                break;
        }

        if (obj != nullptr) {
            utility::log("processing object of type: " + obj->type_info()->fqName() + " from: " + originString);
        }

        if (const auto visited = visitedObjects.find(obj); visited != visitedObjects.end()) return;
        if (obj == nullptr || isNullOrMarker(obj)) return;

        visitedObjects.insert(obj);
        objectsToVisit.push(obj);
    };

    auto threadRegistry = mm::ThreadRegistry::Instance().LockForIter();
    for (auto& thread : threadRegistry) {
        for (const auto& objectLocation : thread.tls()) {
            if (*objectLocation == oldObject) {
                *objectLocation = newObject;
                UpdateHeapRef(objectLocation, newObject);
                updatedObjects++;
            }
            processObject(*objectLocation, Origin::Global);
        }
    }

    auto globalsIterable = mm::GlobalData::Instance().globalsRegistry().LockForIter();
    for (const auto& objectLocation : globalsIterable) {
        if (*objectLocation == oldObject) {
            *objectLocation = newObject;
            UpdateHeapRef(objectLocation, newObject);
            updatedObjects++;
        }

        processObject(*objectLocation, Origin::Global);
    }

    for (auto& thread : mm::ThreadRegistry::Instance().LockForIter()) {
        auto& shadowStack = thread.shadowStack();
        for (const auto& object : shadowStack) {
            processObject(object, Origin::ShadowStack);
        }
    }

    processObject(oldObject, Origin::ObjRef);

    utility::log("Starting visit with: " + std::to_string(objectsToVisit.size()));

    while (!objectsToVisit.empty()) {
        const auto nextObject = objectsToVisit.front();
        objectsToVisit.pop();

        traverseObjectFields(nextObject, [&](mm::RefFieldAccessor fieldAccessor) {
            ObjHeader* fieldValue = fieldAccessor.direct();
            if (fieldValue == oldObject) {
                fieldAccessor.store(newObject);
                updatedObjects++;
            }
            processObject(fieldValue, Origin::ObjRef);
        });
    }

    return updatedObjects;
}

void hot::HotReloader::UpdateShadowStackReferences(const ObjHeader* oldObject, ObjHeader* newObject) {
    auto& threadRegistry = mm::ThreadRegistry::Instance();
    for (mm::ThreadData& threadData : threadRegistry.LockForIter()) {
        mm::ShadowStack& shadowStack = threadData.shadowStack();
        for (mm::ShadowStack::Iterator it = shadowStack.begin(); it != shadowStack.end(); ++it) {
            if (ObjHeader*& currentRef = *it; currentRef == oldObject) {
                currentRef = newObject;
            }
        }
    }
}
