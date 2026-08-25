#include "CommonLauncher.hpp"

#include <cstdio>
#include <cstdlib>

#include "Common.h"
#include "HotReload.hpp"

using kotlin::hot::HotReload;

// Must be kept in sync with BootstrapMetadata.kt.
extern "C" RUNTIME_WEAK const uint8_t* bootStartManifest;

extern "C" KInt Konan_run_start(const ObjHeader* args) {
    HotReload& hotReload = HotReload::Instance();
    hotReload.LoadBootstrap(bootStartManifest);

    const auto Konan_start = hotReload.LookupForKonanStart();
    if (Konan_start != nullptr) {
        return Konan_start(args);
    }
    std::fprintf(stderr, "error: could not load Konan_start from the embedded bootstrap\n");

    return EXIT_FAILURE;
}

extern "C" RUNTIME_EXPORT int Konan_main(const int argc, const char** argv) {
    return kotlin::executableEntryPoint(argc, argv, Konan_run_start);
}
