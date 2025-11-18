//
// Created by Gabriele.Pappalardo on 14/07/2025.
//

#ifndef HOTRELOAD_HPP
#define HOTRELOAD_HPP

#include <string>
#include <vector>
#include <deque>
#include <atomic>

#include "hot/HotReloadServer.hpp"
#include "hot/MachOParser.hpp"

namespace kotlin::hot {

class HotReloader : Pinned {
public:
    class SymbolLoader : Pinned {
        friend class HotReloader;

        struct LibraryHandle {
            uint64_t epoch; // when the library was loaded (debugging purposes)
            void* handle;
            std::string path;

            LibraryHandle(uint64_t epoch, void* handle, const std::string& path) : epoch(epoch), handle(handle), path(path) {}
        };

        bool loadLibraryFromPath(const std::string& fileName);

        [[nodiscard]] TypeInfo* lookForTypeInfo(const std::string& mangledClassName, int startingFrom) const;

        std::deque<LibraryHandle> handles{};
    };

    struct ReloadRequest {
        friend class HotReloader;
        std::vector<std::string> artifactOutputs;
        explicit ReloadRequest(const std::vector<std::string>& artifact_outputs) : artifactOutputs(artifact_outputs) {}
    };

    static HotReloader& Instance() noexcept;
    HotReloader();
    static void InitModule() noexcept;

    /// Start checking if a hot-reload request is pending.
    /// If that's the case, perform class hot-reloading, preserving the existing state.
    void performIfNeeded(mm::ThreadData& currentThreadData) noexcept;

private:

    void interposeNewFunctionSymbols(const KotlinDynamicLibrary& kotlinDynamicLibrary) const;

    /// Given an instance provided by <code>existingObject</code>, create a new class of the
    /// type provided by <code>newTypeInfo</code>, while preserving existing properties.
    static ObjHeader* stateTransfer(mm::ThreadData& currentThreadData, ObjHeader* existingObject, const TypeInfo* newTypeInfo);

    /// Search for all the classes with instance <code>oldTypeInfo</code> and create
    /// new instances provided by <code>newTypeInfo</code>.
    std::vector<ObjHeader*> findObjectsToReload(const TypeInfo* oldTypeInfo) const;

    /// Perform a BFS on the GlobalRootSet and ThreadLocalSet to update the existing references
    /// to point at <code>newObject</code>.
    static int updateHeapReferences(ObjHeader* oldObject, ObjHeader* newObject);

    static void updateShadowStackReferences(const ObjHeader* oldObject, ObjHeader* newObject);

    SymbolLoader _reloader{};
    HotReloadServer _server{};

    std::deque<ReloadRequest> _requests{};
    std::atomic_bool _processing{};
};

} // namespace kotlin::hot

extern "C" {
void Kotlin_native_internal_HotReload_perform(ObjHeader*);
}

#endif // HOTRELOAD_HPP
