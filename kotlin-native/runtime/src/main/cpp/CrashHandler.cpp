#include "CrashHandler.hpp"

#include <csignal>
#include <cstdlib>
#include <array>
#include <mutex>

#include "KAssert.h"
#include "Porting.h"
#include "Logging.hpp"

#if KONAN_APPLE

// TODO include no_mach on TVOS/WATCHOS
#include "client/mac/handler/exception_handler.h"

#include <mach/exception_types.h>
#include <mach/mach_init.h>
#endif

namespace {
    [[maybe_unused]] int kHandledSignals[] = {
            //SIGABRT,
            SIGBUS,
            SIGFPE,
            SIGILL,
            SIGSEGV,
            //SIGTRAP
    };

    bool minidumpsEnabled() {
        return kotlin::compiler::minidumpLocation() != nullptr;
    }

#if KONAN_APPLE
    std::unique_ptr<google_breakpad::ExceptionHandler> gExceptionHandler = nullptr;

    bool exceptionFilter(void* context) {
        // TODO handle only certain signals?
        return true;
    }

    bool minidumpCallback(const char* dump_dir,
                          const char* minidump_id,
                          void* context,
                          bool succeeded) {
        // TODO handle only certain signals?
        if (succeeded) {
            konan::consoleErrorf("Minidump written to \"%s/%s.dmp\"\n", dump_dir, minidump_id);
        } else {
            konan::consoleErrorf("Failed to write minidump to \"%s/%s.dmp\"\n", dump_dir, minidump_id);
        }
        return false; // false -> do not abort the process and let other handlers to be called
    }
#endif
}

void kotlin::crashHandlerInit() noexcept {
    if (minidumpsEnabled()) {
#if KONAN_APPLE
        RuntimeLogInfo({logging::Tag::kRT}, "Initializing crash handler, minidumps will be written to \"%s\"",
                       compiler::minidumpLocation());

        gExceptionHandler = std::make_unique<google_breakpad::ExceptionHandler>(
                kotlin::compiler::minidumpLocation(),
                exceptionFilter,
                minidumpCallback,
                nullptr,
                true,
                nullptr // in-process generation
        );
#endif
    }
}

void kotlin::writeMinidump() noexcept {
    if (minidumpsEnabled()) {
#if KONAN_APPLE
        gExceptionHandler->WriteMinidump();
#endif
    }
}
