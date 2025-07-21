//
// Created by Gabriele.Pappalardo on 14/07/2025.
//

#ifndef HOTRELOAD_HPP
#define HOTRELOAD_HPP

#include <string>
#include <vector>
#include <deque>

#include "main/cpp/Memory.h"

#include "SafePoint.hpp"

namespace kotlin::hot {
class HotReloader : private Pinned {
public:

    class SymbolManager : private Pinned {
        friend class HotReloader;
        using LibraryHandle = void*;
    private:
        void LoadLibraryFromPath(const std::string& fileName);

        TypeInfo* LookForTypeInfo(const std::string& mangledClassName);

        std::deque<LibraryHandle> handles{};
    };

    static HotReloader& Instance() noexcept;
    static void Init() noexcept;

    /// Start checking if a hot-reload request is pending.
    /// If that's the case, perform class hot-reloading, preserving the existing state.
    void Perform(ObjHeader* knHotReloaderObject) noexcept;

    std::string WaitForRecompilation();

    /// Given an instance provided by <code>existingObject</code>, create a new class of the
    /// type provided by <code>newTypeInfo</code>, while preserving existing properties.
    ObjHeader* StateTransfer(ObjHeader* existingObject, const TypeInfo* newTypeInfo);

    /// Search for all the classes with instance <code>oldTypeInfo</code> and create
    /// new instances provided by <code>newTypeInfo</code>.
    std::vector<ObjHeader*> FindObjectsToReload(const TypeInfo* oldTypeInfo);

    /// Perform a BFS on the GlobalRootSet and ThreadLocalSet to update the existing references
    /// to point at <code>newObject</code>.
    int UpdateHeapReferences(ObjHeader* oldObject, ObjHeader* newObject) const;

    void UpdateShadowStackReferences(const ObjHeader *oldObject, ObjHeader *newObject);

private:
    class HotReloadSafePointActivator final : public mm::ExtraSafePointActionActivator<HotReloadSafePointActivator> {};

    SymbolManager _symbolManager{};
};
} // namespace kotlin::hot

extern "C" {
    void Kotlin_native_internal_HotReload_perform(ObjHeader*);
    ObjHeader* Kotlin_native_internal_HotReload_forceReloadOf(ObjHeader* /*ignored*/, void*, void*);
}


#endif //HOTRELOAD_HPP
