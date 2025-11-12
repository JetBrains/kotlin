#include <iostream>
#include <unordered_set>
#include <queue>
#include <iomanip>
#include <chrono>
#include <fstream>
#include <sstream>

#include <dlfcn.h>

#include "Memory.h"
#include "Natives.h"
#include "GlobalData.hpp"
#include "ReferenceOps.hpp"
#include "ThreadRegistry.hpp"
#include "ShadowStack.hpp"
#include "ThreadData.hpp"
#include "RootSet.hpp"
#include "ObjectTraversal.hpp"

#include "HotReload.hpp"

#include "hot/HotReloadServer.hpp"
#include "hot/HotReloadUtility.hpp"
#include "hot/MachOParser.hpp"

#include "hot/fishhook.h"

#include <set>

using namespace kotlin;

/// Forward declarations for GC functions.
namespace kotlin::gc {
void stopTheWorld(GCHandle gcHandle, const char* reason) noexcept;
void resumeTheWorld(GCHandle gcHandle) noexcept;
} // namespace kotlin::gc

ManuallyScoped<HotReloader> globalDataInstance{};

constexpr std::set<std::string_view> NON_RELOADABLE_CLASS_SYMBOLS = {
    "kclass:kotlin.Annotation"
};

enum class Origin {
    Global, ShadowStack, ObjRef
};

const char* originToString(const Origin origin) noexcept {
    switch (origin) {
        case Origin::Global: return "Global";
        case Origin::ShadowStack: return "ShadowStack";
        case Origin::ObjRef: return "Object Reference";
        default: return "Unknown";
    }
}

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

template <typename F>
int visitObjectGraph(ObjHeader* startObject, F processingFunction) {
    // We need to perform a BFS, while ensuring that the world is stopped.
    // Let's collect the root set, and start the graph exploration.
    // At the moment, let's make things simple, and single-threaded (otherwise, well, headaches).
    int32_t updatedObjects{0};

    std::queue<ObjHeader*> objectsToVisit{};
    std::unordered_set<ObjHeader*> visitedObjects{};

    // Let's start collecting the root set
    auto processObject = [&](ObjHeader* obj, Origin origin) {
        // const char* originString = originToString(origin);

        if (obj != nullptr) {
            // hot::utility::log("processing object of type: " + obj->type_info()->fqName() + " from: " + originString);
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

    utility::log("Starting visit with: " + std::to_string(objectsToVisit.size()), utility::LogLevel::DEBUG);

    while (!objectsToVisit.empty()) {
        const auto nextObject = objectsToVisit.front();
        objectsToVisit.pop();
        processingFunction(nextObject, processObject);
    }

    return updatedObjects;
}

void Kotlin_native_internal_HotReload_perform(ObjHeader* obj) {
    HotReloader::Instance().performIfNeeded(obj);
}

HotReloader::HotReloader() {
    utility::initializeHotReloadLogs();
    utility::log("Initializing HotReload module and server");
    if (_server.start()) {
        _server.run([this](const std::vector<std::string>& dylibPaths) {
            utility::log("A new reload request has arrived, containing: ", utility::LogLevel::DEBUG);
            for (auto& dylib : dylibPaths) utility::log("\t" + dylib, utility::LogLevel::DEBUG);
            utility::log("Note that only dylib at time is supported right now.", utility::LogLevel::WARN);
            _requests.emplace_front(dylibPaths);
        });
    }
}

void HotReloader::InitModule() noexcept {
    globalDataInstance.construct();
}

HotReloader& HotReloader::Instance() noexcept {
    return *globalDataInstance;
}

void HotReloader::interposeNewFunctionSymbols(const KotlinDynamicLibrary& kotlinDynamicLibrary) const {

    std::vector<rebinding> rebindingsToPerform{};
    // std::vector<void*> originalFuncs{};
    rebindingsToPerform.reserve(kotlinDynamicLibrary.functions.size()); // This is a pessimistic assumption
    // originalFuncs.resize(functions.size());

    for (const auto& mangledFunctionName : kotlinDynamicLibrary.functions) {

        void* symbolAddress = nullptr;

        for (auto& handle : _reloader.handles) {
            utility::log("Checking handle: " + handle.path + ", epoch: " + std::to_string(handle.epoch), utility::LogLevel::DEBUG);
            if (void* symbol = dlsym(handle.handle, mangledFunctionName.data()); symbol != nullptr) {
                symbolAddress = symbol;
                break;
            }
            utility::log("dlerror: " + std::string{dlerror()}, utility::LogLevel::WARN);
        }

        if (symbolAddress == nullptr) continue;

        rebinding reb{};
        reb.name = mangledFunctionName.data();
        reb.replacement = symbolAddress;
        // reb.replaced = &originalFuncs[index];

        rebindingsToPerform.push_back(reb);

        utility::log("Function '" + std::string{mangledFunctionName} + "' is going to be rebound.", utility::LogLevel::DEBUG);
    }

    // Perform symbol interposition with the collected function symbols
    if (!rebindingsToPerform.empty()) {
        utility::log("Performing rebinding of " + std::to_string(rebindingsToPerform.size()) + " symbols");

        if (rebind_symbols(rebindingsToPerform.data(), rebindingsToPerform.size()) != 0) {
            utility::log("Rebinding failed for an unknown reason", utility::LogLevel::ERR);
        } else {
            utility::log("Rebinding performed successfully!");
        }
    }
}

void HotReloader::performIfNeeded(ObjHeader* _) noexcept {
    if (_requests.empty()) {
        utility::log("Cannot perform hot-reloading since there is no upcoming request", utility::LogLevel::WARN);
        return;
    }

    if (_processing) {
        utility::log("Processing a previous request...", utility::LogLevel::WARN);
        return;
    }

    _processing = true;

    const ReloadRequest reloadRequest = _requests.front();
    _requests.pop_front();

    const std::string dylibPath = reloadRequest.dylibPathsToLoad[0]; // TODO: handle more dylibs

    /// 1. Load the new library into memory with `dlopen`
    if (!_reloader.loadLibraryFromPath(dylibPath)) {
        _processing = false;
        return;
    }

    const uint64_t epoch =
            std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch()).count();

    auto mainGCLock = mm::GlobalData::Instance().gc().gcLock();
    const auto gcHandle = gc::GCHandle::create(epoch);

    // STOP-ZA-WARUDO!
    utility::log("Starting reloading instances for new class layouts");

    kotlin::gc::stopTheWorld(gcHandle, "starting hot reloading");

    /// 2. Locate the **new** TypeInfo (@"kclass:kotlin.Function0")
    auto parsedDynamicLib = dyld::parseDynamicLibrary(dylibPath);

    for (const auto& classMangledName : parsedDynamicLib.classes) {
        if (NON_RELOADABLE_CLASS_SYMBOLS.find(classMangledName) != NON_RELOADABLE_CLASS_SYMBOLS.end()) {
            utility::log("Cannot reload class of type: " + std::string(classMangledName));
            continue;
        }
        
        auto temp = std::string{classMangledName};

        const TypeInfo* oldTypeInfo = _reloader.lookForTypeInfo(temp, 0);
        if (oldTypeInfo == nullptr) {
            // utility::log("Cannot find the old TypeInfo for: " + temp, utility::LogLevel::WARN);
            continue;
        }

        const TypeInfo* newTypeInfo = _reloader.lookForTypeInfo(temp, 1);
        if (newTypeInfo == nullptr) {
            utility::log("Cannot find the new TypeInfo for: " + temp, utility::LogLevel::WARN);
            continue;
        }
        auto objectsToReload = findObjectsToReload(oldTypeInfo);
        /// 3. For each new redefined TypeInfo, reload the instances
        for (const auto& obj : objectsToReload) {
            const auto newInstance = stateTransfer(obj, newTypeInfo);
            updateShadowStackReferences(obj, newInstance);
            updateHeapReferences(obj, newInstance);
        }
    }

        /// 5. Also, interpose new function symbols
        interposeNewFunctionSymbols(parsedDynamicLib);

        kotlin::gc::resumeTheWorld(gcHandle);
        _processing = false;

        utility::log("code has been hot-reloaded.");
    }

    ObjHeader* HotReloader::stateTransfer(ObjHeader * oldObject, const TypeInfo* newTypeInfo) {
        struct FieldData {
            std::string type;
            int32_t offset;
            Konan_RuntimeType runtimeType;

            FieldData() : offset(0), runtimeType(RT_INVALID) {}

            FieldData(const std::string type, const int32_t offset, Konan_RuntimeType _runtimeType) : type(type), offset(offset), runtimeType(_runtimeType) {}
        };

        // // TODO: Do we need two class tables here (the old and the new one)?
        //
        // // TODO: INHERITANCE
        // // TODO: At the moment we are not considering inheritance. We should consider three cases:
        // // TODO: a. Parent class does not change.
        // // TODO: b. Parent class changes.
        // // TODO: c. Parent class gets removed.

        const mm::ThreadRegistry& threadRegistry = mm::ThreadRegistry::Instance();
        mm::ThreadData* currentThreadData = threadRegistry.CurrentThreadData();

        ObjHeader* newObject = currentThreadData->allocator().allocateObject(newTypeInfo);
        if (newObject == nullptr) {
            utility::log("allocation of new object failed!?", utility::LogLevel::ERR);
            return nullptr;
        }

        const auto oldObjectTypeInfo = oldObject->type_info();
        const auto newClassName = newTypeInfo->fqName();
        const auto oldClassName = oldObjectTypeInfo->fqName();

        const ExtendedTypeInfo* oldObjExtendedInfo = oldObjectTypeInfo->extendedInfo_;
        const int32_t oldFieldsCount = oldObjExtendedInfo->fieldsCount_; // How many fields the old objects declared
        const char** oldFieldNames = oldObjExtendedInfo->fieldNames_; // field names are null-terminated
        const int32_t* oldFieldOffsets = oldObjExtendedInfo->fieldOffsets_;
        const auto oldFieldRuntimeTypes = oldObjExtendedInfo->fieldTypes_;
        const auto oldFieldTypes = oldObjExtendedInfo->getExtendedFieldTypes();

        std::unordered_map<std::string, FieldData> oldObjectFields{};

        for (int32_t i = 0; i < oldFieldsCount; i++) {
            std::string fieldName{oldFieldNames[i]};
            FieldData fieldData{oldFieldTypes[i], oldFieldOffsets[i], static_cast<Konan_RuntimeType>(oldFieldRuntimeTypes[i])};
            oldObjectFields[fieldName] = fieldData;
        }

        const ExtendedTypeInfo* newObjExtendedInfo = newObject->type_info()->extendedInfo_;
        const int32_t newFieldsCount = newObjExtendedInfo->fieldsCount_; // How many fields the old objects declared
        const char** newFieldNames = newObjExtendedInfo->fieldNames_; // field names are null-terminated
        const int32_t* newFieldOffsets = newObjExtendedInfo->fieldOffsets_;
        const auto newFieldTypes = newObjExtendedInfo->getExtendedFieldTypes();

        for (int32_t i = 0; i < newFieldsCount; i++) {
            const std::string newFieldName{newFieldNames[i]};

            const auto& newFieldType = newFieldTypes[i];
            const uint8_t newFieldOffset = newFieldOffsets[i];

            if (const auto foundField = oldObjectFields.find(newFieldName); foundField == oldObjectFields.end()) {
                utility::log("field `" + newFieldName + "` is new field in '" + newClassName + "'. It won't be copied.");
                continue;
            }

            // Performs type-checking. Note that this type checking is shallow, i.e., it does not check the object classes.
            const auto& [oldFieldType, oldFieldOffset, oldFieldRuntimeType] = oldObjectFields[newFieldName];
            if (oldFieldType != newFieldType) {
                std::stringstream ss;
                ss << "failed type-checking: " << newClassName << "::" << newFieldName << ":" << utility::kTypeNames[oldFieldRuntimeType];
                ss << " != ";
                ss << oldClassName << "::" << newFieldName << ":" << utility::kTypeNames[oldFieldRuntimeType];
                utility::log(ss.str());
                continue;
            }

            // Handle Kotlin Objects in a different way, the updates must be notified to the GC
            if (oldFieldRuntimeType == RT_OBJECT) {
                const auto oldFieldLocation = reinterpret_cast<ObjHeader**>(reinterpret_cast<uint8_t*>(oldObject) + oldFieldOffset);
                const auto newFieldLocation = reinterpret_cast<ObjHeader**>(reinterpret_cast<uint8_t*>(newObject) + newFieldOffset);

                // std::stringstream ss;
                // ss << "copying reference field '" << newFieldName << '\n';
                // ss << "\t" << newFieldName << ':' << actualNewFieldType << " = ";
                // ss << std::showbase << std::hex << *oldFieldLocation << std::dec << std::endl;
                // utility::log(ss.str());

                UpdateHeapRef(newFieldLocation, *oldFieldLocation);
                *newFieldLocation = *oldFieldLocation; // just copy the reference to the previous object
                utility::log("Obj reference updated!", utility::LogLevel::DEBUG);

                continue;
            }

            const auto oldFieldData = reinterpret_cast<uint8_t*>(oldObject) + oldFieldOffset;
            const auto newFieldData = reinterpret_cast<uint8_t*>(newObject) + newFieldOffset;

            // utility::log(
            //     "copying field " + utility::field2String(newFieldName.c_str(), oldFieldData, oldFieldType),
            //     utility::LogLevel::DEBUG);

            // Perform byte-copy of the field
            std::memcpy(newFieldData, oldFieldData, utility::kRuntimeTypeSize[oldFieldRuntimeType]);
        }

        return newObject;
    }

std::vector<ObjHeader*> HotReloader::findObjectsToReload(const TypeInfo* oldTypeInfo) const {
    std::vector<ObjHeader*> existingObjects{};
    std::string oldTypeFqName = oldTypeInfo->fqName();

    visitObjectGraph(nullptr, [&existingObjects, &oldTypeFqName](ObjHeader* nextObject, auto processObject) {
        // Traverse object references inside class properties
        traverseObjectFieldsInternal(nextObject, [&](const mm::RefFieldAccessor& fieldAccessor) {
            processObject(fieldAccessor.direct(), Origin::ObjRef);
        });

        if (nextObject->type_info()->fqName() == oldTypeFqName) {
            utility::log("Instance of " + nextObject->type_info()->fqName() + " must change!", utility::LogLevel::DEBUG);
            existingObjects.emplace_back(nextObject);
        }
    });
    return existingObjects;
}

int HotReloader::updateHeapReferences(ObjHeader* oldObject, ObjHeader* newObject) {
    int32_t updatedObjects{0};

    std::queue<ObjHeader*> objectsToVisit{};
    std::unordered_set<ObjHeader*> visitedObjects{};

    // Let's start collecting the root set
    auto processObject = [&](ObjHeader* obj, Origin origin) {
        const char* originString = originToString(origin);

        if (obj != nullptr) {
            utility::log("processing object of type: " + obj->type_info()->fqName() + " from: " + originString, utility::LogLevel::DEBUG);
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

    utility::log("Updating Heap References - Starting visit with: " + std::to_string(objectsToVisit.size()), utility::LogLevel::DEBUG);

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

void HotReloader::updateShadowStackReferences(const ObjHeader* oldObject, ObjHeader* newObject) {
    auto& threadRegistry = mm::ThreadRegistry::Instance();
    for (mm::ThreadData& threadData : threadRegistry.LockForIter()) {
        mm::ShadowStack& shadowStack = threadData.shadowStack();
        for (auto it = shadowStack.begin(); it != shadowStack.end(); ++it) {
            if (ObjHeader*& currentRef = *it; currentRef == oldObject) {
                currentRef = newObject;
            }
        }
    }
}

// Reloader

bool HotReloader::SymbolLoader::loadLibraryFromPath(const std::string& fileName) {
    void* libraryHandle = dlopen(fileName.c_str(), RTLD_LAZY);
    if (libraryHandle == nullptr) {
        utility::log("An error occurred while loading library from: " + fileName, utility::LogLevel::ERR);
        utility::log("Details: " + std::string{dlerror()}, utility::LogLevel::ERR);
        return false;
    }

    const uint64_t epoch = std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch()).count();

    const LibraryHandle newHandle{epoch, libraryHandle, fileName};
    utility::log("Loaded library from path: " + fileName, utility::LogLevel::DEBUG);

    handles.push_front(newHandle);
    return true;
}

TypeInfo* HotReloader::SymbolLoader::lookForTypeInfo(const std::string& mangledClassName, const int startingFrom = 0) const {
    // |LIB-A-2| <- |LIB-A-1| <- |LIB-A-0| <- BASE
    // The most recent library is loaded on top of the deque.

    const int actualOffset = startingFrom - 1;

    if (actualOffset == -1 && handles.size() == 1) { // we need this to handle the base case
        if (void* symbol = dlsym(RTLD_MAIN_ONLY, mangledClassName.c_str()); symbol != nullptr)
            return static_cast<TypeInfo*>(symbol);
        utility::log("dlerror: " + std::string{dlerror()}, utility::LogLevel::ERR);
        return nullptr;
    }

    for (size_t i = actualOffset; i < handles.size(); i++) {
        utility::log("Checking library at path: " + handles[i].path, utility::LogLevel::DEBUG);

        void* symbol = dlsym(handles[i].handle, mangledClassName.c_str());
        if (symbol != nullptr) return static_cast<TypeInfo*>(symbol);
        utility::log("dlerror: " + std::string{dlerror()}, utility::LogLevel::ERR);
    }

    return nullptr;
}
