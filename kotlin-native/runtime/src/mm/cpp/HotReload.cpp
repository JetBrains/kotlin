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

#include "KString.h"
#include "hot/HotReloadServer.hpp"
#include "hot/HotReloadUtility.hpp"
#include "hot/MachOParser.hpp"

#include "hot/fishhook.h"

#include <set>
#include <utility>

using namespace kotlin;

/// Forward declarations for GC functions.
namespace kotlin::gc {
void stopTheWorld(GCHandle gcHandle, const char* reason) noexcept;
void resumeTheWorld(GCHandle gcHandle) noexcept;
} // namespace kotlin::gc

namespace {
[[clang::no_destroy]] ObjHeader* gOnSuccess = nullptr;
[[clang::no_destroy]] std::once_flag gOnSuccessRegOnce;
}

static uint64_t getCurrentEpoch() {
    return std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch()).count();
}

ManuallyScoped<HotReloader> globalDataInstance{};

const std::unordered_set<std::string_view> NON_RELOADABLE_CLASS_SYMBOLS = {"kclass:kotlin.Annotation"};

enum class Origin { Global, ShadowStack, ObjRef };

const char* originToString(const Origin origin) noexcept {
    switch (origin) {
        case Origin::Global:
            return "Global";
        case Origin::ShadowStack:
            return "ShadowStack";
        case Origin::ObjRef:
            return "Object Reference";
        default:
            return "Unknown";
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
        if (obj == nullptr || isNullOrMarker(obj)) return;

        // HRLogDebug("processing object of type %s from %s", obj->type_info()->fqName().c_str(), originString);
        if (const auto visited = visitedObjects.find(obj); visited != visitedObjects.end()) return;

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

    HRLogDebug("Starting object graph visit with %ld nodes", objectsToVisit.size());

    while (!objectsToVisit.empty()) {
        const auto nextObject = objectsToVisit.front();
        objectsToVisit.pop();
        processingFunction(nextObject, processObject);
    }

    return updatedObjects;
}

extern "C" {
    void Kotlin_native_internal_HotReload_perform(ObjHeader* obj, ObjHeader* dylibPathStr) {
        AssertThreadState(ThreadState::kRunnable);
        // TODO: segmentation fault :(
        const auto dylibPath = kotlin::to_string<KStringConversionMode::UNCHECKED>(dylibPathStr);
        HotReloader::Instance().reload(dylibPath);
    }

    RUNTIME_NOTHROW void Kotlin_native_internal_HotReload_registerSuccessCallback(ObjHeader* obj, ObjHeader* fn) {
        AssertThreadState(ThreadState::kRunnable);

        std::call_once(gOnSuccessRegOnce, []() noexcept {
            // Register the storage as a GC root once.
            InitAndRegisterGlobal(&gOnSuccess, nullptr);
        });

        // Replace the callback atomically with write barriers.
        HRLogDebug("Registering success callback: %p", fn);
        UpdateHeapRef(&gOnSuccess, fn);
    }
}

HotReloader::HotReloader() {
    HRLogInfo("Initializing HotReload module and server");
    if (_server.start()) {
        _server.run([this](const std::vector<std::string>& dylibPaths) {
            HRLogDebug("A new reload request has arrived, containing: ");
            for (auto& dylib : dylibPaths) HRLogDebug("\t* %s", dylib.c_str());
            HRLogWarning("Note that only dylib at time is supported right now");

            const auto& dylibPath = dylibPaths[0];
            reload(dylibPath);
        });
    }
}

void HotReloader::InitModule() noexcept {
    globalDataInstance.construct();
}

HotReloader& HotReloader::Instance() noexcept {
    return *globalDataInstance;
}

void HotReloader::interposeNewFunctionSymbols(const KotlinDynamicLibrary& kotlinDynamicLibrary) {
    std::vector<rebinding> rebindingsToPerform{};
    rebindingsToPerform.reserve(kotlinDynamicLibrary.functions.size());

    // std::vector<void*> originalFuncs{};
    // originalFuncs.resize(functions.size());

    for (const auto& mangledFunctionName : kotlinDynamicLibrary.functions) {
        void* symbolAddress = nullptr;

        for (auto& handle : _reloader.handles) {
            if (void* symbol = dlsym(handle.handle, mangledFunctionName.data()); symbol != nullptr) {
                symbolAddress = symbol;
                break;
            }
            HRLogWarning("dlerror: %s, symbol: %s not found in handle %p with epoch %llu", dlerror(), mangledFunctionName.data(), handle.handle, handle.epoch);
        }

        if (symbolAddress == nullptr) continue;

        rebinding reb{};
        reb.name = mangledFunctionName.data();
        reb.replacement = symbolAddress;
        // reb.replaced = &originalFuncs[index];

        rebindingsToPerform.push_back(reb);

        HRLogDebug("Function '%s' is going to be rebound at address %p", mangledFunctionName.data(), symbolAddress);
    }

    // Perform symbol interposition with the collected function symbols
    if (!rebindingsToPerform.empty()) {
        HRLogInfo("Performing rebinding of %ld symbols", rebindingsToPerform.size());
        statsCollector.registerReboundSymbols(static_cast<int>(rebindingsToPerform.size()));

        if (rebind_symbols(rebindingsToPerform.data(), rebindingsToPerform.size()) != 0) {
            HRLogError("Rebinding failed for an unknown reason");
        } else {
            HRLogInfo("Rebinding performed successfully");
        }
    }
}

void HotReloader::reload(const std::string& dylibPath) noexcept {

    // TODO: here there may be a concurrency issue, threads are not yet suspended!
    statsCollector.registerStart(static_cast<int64_t>(getCurrentEpoch()));
    statsCollector.registerLoadedLibrary(dylibPath);

    /// 1. Load the new library into memory with `dlopen`
    if (!_reloader.loadLibraryFromPath(dylibPath)) {
        HRLogError("Cannot load dylib in memory space!?");
        statsCollector.registerEnd(static_cast<int64_t>(getCurrentEpoch()));
        statsCollector.registerSuccessful(false);
        return;
    }

    /// 2. Locate the **new** TypeInfo (@"kclass:kotlin.Function0")
    auto parsedDynamicLib = dyld::parseDynamicLibrary(dylibPath);

    {
        // STOP-ZA-WARUDO!
        CalledFromNativeGuard guard(true);

        HRLogDebug("Switching to K/N state and requestion threads suspension");
        auto mainGCLock = mm::GlobalData::Instance().gc().gcLock(); // Serialize global State

        auto* currentThreadData = mm::ThreadRegistry::Instance().CurrentThreadData();
        currentThreadData->suspensionData().requestThreadsSuspension("Hot-Reload");

        CallsCheckerIgnoreGuard allowWait;

        try {
            mm::WaitForThreadsSuspension();
            perform(*currentThreadData, parsedDynamicLib);
            statsCollector.registerEnd(static_cast<int64_t>(getCurrentEpoch()));
            statsCollector.registerSuccessful(true);
            mm::ResumeThreads();

            if (gOnSuccess != nullptr) {
                HRLogDebug("Calling Kotlin success-callback: %p", gOnSuccess);
                Kotlin_native_internal_HotReload_invokeSuccessCallback(gOnSuccess);
            }
        } catch (const std::exception& e) {
            HRLogError("Hot-reload failed with exception: %s", e.what());
            statsCollector.registerEnd(static_cast<int64_t>(getCurrentEpoch()));
            statsCollector.registerSuccessful(false);
            mm::ResumeThreads();
        }
    }
}

void HotReloader::perform(mm::ThreadData& currentThreadData, const KotlinDynamicLibrary& libraryToLoad) noexcept {
    _processing = true; // TODO: should not be necessary if the pre-conditions are held

    HRLogInfo("Starting Hot-Reloading..." );

    for (const auto& classMangledName : libraryToLoad.classes) {
        if (NON_RELOADABLE_CLASS_SYMBOLS.find(classMangledName) != NON_RELOADABLE_CLASS_SYMBOLS.end()) {
            HRLogWarning("Cannot reload class of type: %s", classMangledName.data());
            continue;
        }

        auto temp = std::string{classMangledName}; // TODO: do not allocate new string, change lookForTypeInfo API

        const TypeInfo* oldTypeInfo = _reloader.lookForTypeInfo(temp, 0);
        if (oldTypeInfo == nullptr) {
            // HRLogWarning("Cannot find the new TypeInfo for class: %s", temp.data());
            continue;
        }

        const TypeInfo* newTypeInfo = _reloader.lookForTypeInfo(temp, 1);
        if (newTypeInfo == nullptr) {
            HRLogWarning("Cannot find the new TypeInfo for class: %s", temp.data());
            continue;
        }
        auto objectsToReload = findObjectsToReload(oldTypeInfo);
        /// 3. For each new redefined TypeInfo, reload the instances
        for (const auto& obj : objectsToReload) {
            const auto newInstance = stateTransfer(currentThreadData, obj, newTypeInfo);
            updateShadowStackReferences(obj, newInstance);
            updateHeapReferences(obj, newInstance);
        }
    }

    /// 4. Also, interpose new function symbols
    interposeNewFunctionSymbols(libraryToLoad);

    _processing = false;

    HRLogInfo("Ending Hot-Reloading..." );
}

ObjHeader* HotReloader::stateTransfer(mm::ThreadData& currentThreadData, ObjHeader* oldObject, const TypeInfo* newTypeInfo) {
    struct FieldData {
        std::string type;
        int32_t offset;
        Konan_RuntimeType runtimeType;

        FieldData() : offset(0), runtimeType(RT_INVALID) {}

        FieldData(std::string type, const int32_t offset, const Konan_RuntimeType _runtimeType) :
            type(std::move(type)), offset(offset), runtimeType(_runtimeType) {}
    };

    ObjHeader* newObject = currentThreadData.allocator().allocateObject(newTypeInfo);
    if (newObject == nullptr) {
        HRLogError("allocation of new object of type %s failed!?", newTypeInfo->fqName().c_str());
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
            HRLogDebug("Field '%s::%s' is new, it won't be copied", newFieldName.c_str(), newClassName.c_str());
            continue;
        }

        // Performs type-checking. Note that this type checking is shallow, i.e., it does not check the object classes.
        const auto& [oldFieldType, oldFieldOffset, oldFieldRuntimeType] = oldObjectFields[newFieldName];
        if (oldFieldType != newFieldType) {
            HRLogInfo("Failed type-checking: %s::%s:%s != %s::%s:%s",
                newClassName.c_str(), newFieldName.c_str(), utility::kTypeNames[oldFieldRuntimeType],
                oldClassName.c_str(), newFieldName.c_str(), utility::kTypeNames[oldFieldRuntimeType]);
            continue;
        }

        // Handle Kotlin Objects in a different way, the updates must be notified to the GC

        // TODO: investigate null discussion
        if (oldFieldRuntimeType == RT_OBJECT) {
            const auto oldFieldLocation = reinterpret_cast<ObjHeader**>(reinterpret_cast<uint8_t*>(oldObject) + oldFieldOffset);
            const auto newFieldLocation = reinterpret_cast<ObjHeader**>(reinterpret_cast<uint8_t*>(newObject) + newFieldOffset);

            UpdateHeapRef(newFieldLocation, *oldFieldLocation);
            *newFieldLocation = *oldFieldLocation; // Just copy the reference to the previous object

            // HRLogDebug("Copying reference field '%s'\n\t%s:%s = %p", newFieldName.c_str(), newFieldName.c_str(), utility::kTypeNames[oldFieldRuntimeType], *oldFieldLocation);
            HRLogDebug("Object reference updated from '%p' to '%p'", *oldFieldLocation, *newFieldLocation);
            continue;
        }

        const auto oldFieldData = reinterpret_cast<uint8_t*>(oldObject) + oldFieldOffset;
        const auto newFieldData = reinterpret_cast<uint8_t*>(newObject) + newFieldOffset;

        // HRLogDebug("copying field %s", utility::field2String(newFieldName.c_str(), oldFieldData, oldFieldRuntimeType).c_str());

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
            HRLogDebug("Instance of class '%s' at '%p', must be reloaded", nextObject->type_info()->fqName().c_str(), nextObject);
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
        if (obj == nullptr || isNullOrMarker(obj)) return;

        if (const auto visited = visitedObjects.find(obj); visited != visitedObjects.end()) return;
        HRLogDebug("processing object of type '%s' from %s", obj->type_info()->fqName().c_str(), originString);

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

    HRLogDebug("Updating Heap References");
    HRLogDebug("Starting visit with %ld objects", objectsToVisit.size());

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
        HRLogError("Could not open dylib at '%s'. Details: %s", fileName.c_str(), dlerror());
        return false;
    }

    const LibraryHandle newHandle{getCurrentEpoch(), libraryHandle, fileName};
    HRLogDebug("Loaded library from path: %s", fileName.c_str());

    handles.push_front(newHandle);
    return true;
}

TypeInfo* HotReloader::SymbolLoader::lookForTypeInfo(const std::string& mangledClassName, const int startingFrom = 0) const {
    // |LIB-A-2| <- |LIB-A-1| <- |LIB-A-0| <- BASE
    // The most recent library is loaded on top of the deque.

    const int actualOffset = startingFrom - 1;

    if (actualOffset == -1 && handles.size() == 1) { // we need this to handle the base case
        if (void* symbol = dlsym(RTLD_MAIN_ONLY, mangledClassName.c_str()); symbol != nullptr) return static_cast<TypeInfo*>(symbol);
        HRLogError("dlerror: %s", dlerror());
        return nullptr;
    }

    for (size_t i = actualOffset; i < handles.size(); i++) {
        HRLogDebug("Checking library at path '%s'", handles[i].path.c_str());
        if (void* symbol = dlsym(handles[i].handle, mangledClassName.c_str()); symbol != nullptr) return static_cast<TypeInfo*>(symbol);
        HRLogError("dlerror: %s", dlerror());
    }

    return nullptr;
}
