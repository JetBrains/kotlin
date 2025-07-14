//
// Created by Gabriele.Pappalardo on 14/07/2025.
//

#ifndef HOTRELOAD_HPP
#define HOTRELOAD_HPP

#include <string>
#include <vector>
#include <deque>

#include "main/cpp/Memory.h"

namespace kotlin::hot {
class HotReloader : private Pinned {
public:
    static HotReloader& Instance() noexcept;
    static void Init() noexcept;

    /// Start checking if a hot-reload request is pending.
    /// If that's the case, perform class hot-reloading, preserving the existing state.
    void Perform(ObjHeader* knHotReloaderObject) noexcept;
private:
    class SymbolManager : private Pinned {
        friend class HotReloader;
        using LibraryHandle = void*;
    private:
        void LoadLibraryFromPath(const std::string& fileName);

        TypeInfo* LookForTypeInfo(const std::string& mangledClassName);

        std::deque<LibraryHandle> handles{};
    };

    std::string WaitForRecompilation();

    /// Given an instance provided by <code>existingObject</code>, create a new class of the
    /// type provided by <code>newTypeInfo</code>, while preserving existing properties.
    ObjHeader* StateTransfer(ObjHeader* existingObject, TypeInfo* newTypeInfo);

    /// Search for all the classes with instance <code>oldTypeInfo</code> and create
    /// new instances provided by <code>newTypeInfo</code>.
    std::vector<ObjHeader*> StateTransferAll(TypeInfo* oldTypeInfo, TypeInfo* newTypeInfo);

    /// Perform a BFS on the GlobalRootSet and ThreadLocalSet to update the existing references
    /// to point at <code>newObject</code>.
    int UpdateHeapReferences(ObjHeader* oldObject, ObjHeader* newObject);

    SymbolManager _symbolManager{};
};
} // namespace kotlin::hot

extern "C" {
    void Kotlin_native_internal_HotReload_perform(ObjHeader*);
}


#endif //HOTRELOAD_HPP
