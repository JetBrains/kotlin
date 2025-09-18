#include "kt42397_api.h"

#include <thread>

int main() {
    auto t = std::thread([] {
        auto lib = kt42397_symbols();

        // Initialize A and B.Companion and get their stable pointers.
        auto a = lib->kotlin.root.knlibrary.A._instance();
        auto bCompanion = lib->kotlin.root.knlibrary.B.Companion._instance();

        // Now, dispose of the stable pointers.
        lib->DisposeStablePointer(bCompanion.pinned);
        lib->DisposeStablePointer(a.pinned);

        // A and B.Companion now are owned by the global references only.
    });
    t.join();

    return 0;
}
