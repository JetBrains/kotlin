#include "hot_launcher.h"

#include "Memory.h"
#include "Natives.h"
#include "Runtime.h"
#include "KString.h"
#include "Types.h"
#include "Common.h"
#include "HotReload.hpp"
#include "HotReloadInternal.hpp"

using kotlin::hot::HotReload;
using kotlin::hot::HotReloadImpl;

// TODO: this needs extra checks (on macOS, it can be tailed down to the final executable)
// TODO: While, on iOS, it needs to be bundled, somehow?
constexpr auto EXPECTED_BOOTSTRAP_FILE_PATH = "./output.bootstrap.o";

OBJ_GETTER(setupArgs, const int argc, const char** argv) {
    if (argc > 0 && argv[0][0] != '\0') {
        // Don't set the programName to an empty string (by checking argv[0][0] != '\0') to make all platforms behave the same:
        // Linux would set argv[0] to "" in case no programName is passed, whereas Windows & macOS would set argc to 0.
        kotlin::programName = argv[0];
    }

    // The count is one less, because we skip argv[0] which is the binary name.
    ObjHeader* result = AllocArrayInstance(theArrayTypeInfo, std::max(0, argc - 1), OBJ_RESULT);
    ArrayHeader* array = result->array();
    for (int index = 1; index < argc; index++) {
        ObjHolder local;
        CreateStringFromCString(argv[index], local.slot());
        UpdateHeapRef(ArrayAddressOfElementAt(array, index - 1), local.obj());
    }
    return result;
}

extern "C" KInt Konan_run_start(const int argc, const char** argv) {

    ObjHolder args{};
    setupArgs(argc, argv, args.slot());

    // 1) Find bootstrap file, fail if this wasn't found.
    // 2) From the HotReload module, load the bootstrap file, and return the konan_start symbol
    const auto KonanStart = HotReloadImpl::Instance().LoadBootstrapFile(EXPECTED_BOOTSTRAP_FILE_PATH);

    // 3) Run the symbol if not null
    if (KonanStart != nullptr) {
        return KonanStart(args.obj());
    }

    // Something failed while loading the boostrap object, return a failure code
    std::fprintf(stderr, "error :: could not load expected bootstrap file %s\n", EXPECTED_BOOTSTRAP_FILE_PATH);
    return EXIT_FAILURE;
}

extern "C" RUNTIME_EXPORT int Init_and_run_start(const int argc, const char** argv, const int memoryDeInit) {
    Kotlin_initRuntimeIfNeeded();

    // TODO: let's initialize the hot-reload module here, but maybe it should be done globally
    HotReload::InitModule();

    Kotlin_mm_switchThreadStateRunnable();

    const KInt exitStatus = Konan_run_start(argc, argv);

    if (memoryDeInit) {
        Kotlin_shutdownRuntime();
    }

    kotlin::programName = nullptr; // argv[0] might not be valid after this point

    return exitStatus;
}

extern "C" RUNTIME_EXPORT int Konan_main(const int argc, const char** argv) {
    return Init_and_run_start(argc, argv, 1);
}
