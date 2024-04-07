#include "kt56182_root_package1lvl_api.h"

#include <thread>

int main() {
    auto t = std::thread([] {
        auto lib = kt56182_root_package1lvl_symbols();

        lib->kotlin.root.knlibrary.enableMemoryChecker();

        // Initialize A and B.Companion and get their stable pointers.
        auto a = lib->kotlin.root.knlibrary.A._instance();
        auto bCompanion = lib->kotlin.root.knlibrary.B.Companion._instance();

        // Now, dispose of the stable pointers.
        lib->DisposeStablePointer(bCompanion.pinned);
        lib->DisposeStablePointer(a.pinned);

        // A and B.Companion now are owned by the global references only.

        // Now same actions for objects in the root package
        lib->kotlin.root.enableMemoryChecker();

        // Initialize A and B.Companion and get their stable pointers.
        auto a2 = lib->kotlin.root.A._instance();
        auto bCompanion2 = lib->kotlin.root.B.Companion._instance();

        // Now, dispose of the stable pointers.
        lib->DisposeStablePointer(bCompanion2.pinned);
        lib->DisposeStablePointer(a2.pinned);
    });

    // This causes Kotlin runtime full deinitialization, because `t` is the only thread
    // with the Kotlin runtime. So, all the globals will get deinitialized and memory
    // leak checker will get executed (because .kt code is compiled with -g).
    t.join();

    return 0;
}
