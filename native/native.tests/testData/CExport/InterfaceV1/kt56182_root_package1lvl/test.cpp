#include "kt56182_root_package1lvl_api.h"

#include <thread>

int main() {
    auto t = std::thread([] {
        auto lib = kt56182_root_package1lvl_symbols();

        // Initialize A and B.Companion and get their stable pointers.
        auto a = lib->kotlin.root.knlibrary.A._instance();
        auto bCompanion = lib->kotlin.root.knlibrary.B.Companion._instance();

        // Now, dispose of the stable pointers.
        lib->DisposeStablePointer(bCompanion.pinned);
        lib->DisposeStablePointer(a.pinned);

        // A and B.Companion now are owned by the global references only.

        // Initialize A and B.Companion and get their stable pointers.
        auto a2 = lib->kotlin.root.A._instance();
        auto bCompanion2 = lib->kotlin.root.B.Companion._instance();

        // Now, dispose of the stable pointers.
        lib->DisposeStablePointer(bCompanion2.pinned);
        lib->DisposeStablePointer(a2.pinned);
    });
    t.join();

    return 0;
}
