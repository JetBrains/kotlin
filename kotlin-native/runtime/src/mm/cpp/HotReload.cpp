#include <iostream>
#include <unordered_set>
#include <queue>
#include <iomanip>
#include <chrono>
#include <fstream>
#include <sstream>

#include "Memory.h"
#include "Natives.h"
#include "GlobalData.hpp"
#include "ReferenceOps.hpp"
#include "ThreadRegistry.hpp"
#include "ShadowStack.hpp"
#include "ThreadData.hpp"

#include "HotReload.hpp"

using namespace kotlin;
namespace kotlin::hot::utility {

enum class LogLevel { DEBUG, INFO, WARNING, ERROR };

static void log(const std::string& message, const LogLevel level = LogLevel::INFO) {
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
            levelStr = "WARNING";
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

std::string GetFQName(const TypeInfo* typeInfo) {
    struct KLiteralString {
        void* classData_;
        int32_t size_;
        int32_t hash_;
        int16_t unknown_;
        char literal_[]; // utf16 string encoded in utf8 array

        [[nodiscard]] std::string toString() const {
            std::string name(static_cast<size_t>(size_) - 1, '\0');
            const size_t actualSize = (size_ - 1) * 2;
            for (size_t i = 0; i < actualSize; i += 2) {
                name[i / 2] = literal_[i];
            }
            return name;
        }
    };
    const auto packageName = reinterpret_cast<KLiteralString*>(typeInfo->packageName_)->toString();
    auto relativeName = reinterpret_cast<KLiteralString*>(typeInfo->relativeName_)->toString();
    [[unlikely]] if (packageName.empty())
        return relativeName;
    return packageName + "." + relativeName;
}



}; // namespace kotlin::hot::utility

namespace kotlin::gc {
void stopTheWorld(GCHandle gcHandle, const char* reason) noexcept;
void resumeTheWorld(GCHandle gcHandle) noexcept;
} // namespace kotlin::gc

namespace kotlin::hot {

template <typename F>
void traverseObjectFieldsInternal(ObjHeader* object, F process) noexcept(noexcept(process(std::declval<kotlin::mm::RefFieldAccessor>()))) {
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

void Kotlin_native_internal_HotReload_perform(ObjHeader* obj) {
    hot::HotReloader::Instance().Perform(obj);
}

/// TODO: should I initalize the component on Runtime.cpp?
void hot::HotReloader::Init() noexcept {
    hot::utility::log("initializing HotReloader component");
    globalDataInstance.construct();
}

hot::HotReloader& hot::HotReloader::Instance() noexcept {
    return *globalDataInstance;
}

void hot::HotReloader::Perform(ObjHeader* knHotReloaderObject) noexcept {
    utility::log("Performing hot-code reloading: " + utility::GetFQName(knHotReloaderObject->type_info()), utility::LogLevel::WARNING);
}

std::string hot::HotReloader::WaitForRecompilation() {
    return "path/to/compiled/library";
}

ObjHeader* hot::HotReloader::StateTransfer(ObjHeader* existingObject, TypeInfo* newTypeInfo) {
    return nullptr;
}

std::vector<ObjHeader*> hot::HotReloader::StateTransferAll(TypeInfo* oldTypeInfo, TypeInfo* newTypeInfo) {
    return {};
}

int hot::HotReloader::UpdateHeapReferences(ObjHeader* oldObject, ObjHeader* newObject) {
      // We need to perform a BFS, while ensuring that the world is stopped.
    // Let's collect the root set, and start the graph exploration.
    // At the moment, let's make things simple, and single-threaded (otherwise, well, headaches).
    int32_t updatedObjects{0};

    std::queue<ObjHeader*> objectsToVisit{};
    std::unordered_set<ObjHeader*> visitedObjects{};

    // Let's start collecting the root set
    // STOP-ZA-WARUDO!
    auto processObject = [&](ObjHeader* obj) {
        if (obj != nullptr) {
            std::cout << "[info] :: processing object of type: " << kotlin::hot::utility::GetFQName(obj->type_info()) << "\n";
        }

        auto visited = visitedObjects.find(obj);
        if (visited == visitedObjects.end()) return;

        if (obj == nullptr || isNullOrMarker(obj)) return;

        visitedObjects.insert(obj);
        objectsToVisit.push(obj);
    };

    const uint64_t epoch =
            std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch()).count();

    // 00000000000a281c T kotlin::mm::GlobalData::Instance()
    auto mainGCLock = mm::GlobalData::Instance().gc().gcLock();
    auto gcHandle = gc::GCHandle::create(epoch);

    // 00000000000afc78 T kotlin::gc::stopTheWorld(kotlin::gc::GCHandle, char const*)
    kotlin::gc::stopTheWorld(gcHandle, "[hot-reload] :: collecting root set to update obj refs");

    // Process thread roots

    // Directly iterate over ShadowStack:
    auto threadRegistry = mm::ThreadRegistry::Instance().LockForIter();
    for (auto& thread : threadRegistry) {
        auto& shadowStack = thread.shadowStack();
        for (auto& object : shadowStack) {
            std::cout << "[info] processing object from shadow stack\n";
            processObject(object);
        }
    }

    auto globalsIterable = mm::GlobalData::Instance().globalsRegistry().LockForIter();
    for (const auto& objRef : globalsIterable) {
        std::cout << "[info] processing object from global\n";
        processObject(*objRef);
    }

    processObject(oldObject);

    // 00000000000b0068 T kotlin::gc::resumeTheWorld(kotlin::gc::GCHandle)
    kotlin::gc::resumeTheWorld(gcHandle);

    while (!objectsToVisit.empty()) {
        const auto nextObject = objectsToVisit.front();
        objectsToVisit.pop();

        // NOTE: this is implemented in Kotlin/Native runtime

        // 000000000009f7a8 T void kotlin::traverseObjectFieldsInternal<
        //      void kotlin::traverseReferredObjects<kotlin::mm::MemoryDumper::DumpTransitively(ObjHeader*)::'lambda'(auto)
        //  >(ObjHeader*, auto)::'lambda'(auto)>(ObjHeader*, auto)
        traverseObjectFieldsInternal(nextObject, [&](kotlin::mm::RefFieldAccessor fieldAccessor) {
            ObjHeader* fieldValue = fieldAccessor.direct();
            if (fieldValue == oldObject) {
                fieldAccessor.store(newObject);
                updatedObjects++;
            }
            processObject(fieldValue);
        });
    }

    std::cout << "\n";
    std::cout << "[info] :: updated " << updatedObjects << " objects\n";
    return updatedObjects;
}