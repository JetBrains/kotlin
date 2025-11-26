#include "../../common/cpp/CrashHandler.hpp"

#include <csignal>
#include <cerrno>
#include <cstring>
#include <cstdlib>
#include <array>
#include <mutex>
#include <unistd.h>

#include "KAssert.h"
#include "Porting.h"
#include "Logging.hpp"

#if KONAN_OSX

// TODO include no_mach on TVOS/WATCHOS
#include "client/mac/handler/exception_handler.h"

#include <mach/exception_types.h>
#include <mach/mach_init.h>
#include <sys/sysctl.h>
#endif

namespace {

// copy&pasted from `exception_handler_no_mach.cc`
std::array kExceptionSignals = {
        // Core-generating signals.
        SIGABRT, SIGBUS, SIGFPE, SIGILL, SIGQUIT, SIGSEGV, SIGSYS, SIGTRAP, SIGEMT,
        SIGXCPU, SIGXFSZ,
        // Non-core-generating but terminating signals.
        SIGALRM, SIGHUP, SIGINT, SIGPIPE, SIGPROF, SIGTERM, SIGUSR1, SIGUSR2,
        SIGVTALRM, SIGXCPU, SIGXFSZ, SIGIO,
};

void minidumpSigaction(int sig, siginfo_t*, void*) {
    konan::consoleErrorf("Signal %s received. Generating minidump.\n", sys_signame[sig]);
    kotlin::writeMinidump();
    fflush(stderr);
    exit(1);
}

bool installMinidumpSigaction() {
    for (auto signal : kExceptionSignals) {
        struct sigaction sa{};
        memset(&sa, 0, sizeof(sa));
        sigemptyset(&sa.sa_mask);
        sigaddset(&sa.sa_mask, signal);
        sa.sa_sigaction = minidumpSigaction;
        sa.sa_flags = SA_ONSTACK | SA_SIGINFO;

        if (sigaction(signal, &sa, nullptr) == -1) {
            return false;
        }
    }
    return true;
}

#if KONAN_OSX
    bool minidumpsEnabled() {
        return kotlin::compiler::minidumpLocation() != nullptr;
    }

    bool isDebuggerAttached() {
        int mib[4];
        struct kinfo_proc info;
        size_t size;

        mib[0] = CTL_KERN;
        mib[1] = KERN_PROC;
        mib[2] = KERN_PROC_PID;
        mib[3] = getpid();

        size = sizeof(info);
        if (sysctl(mib, 4, &info, &size, nullptr, 0) == 0) {
            return (info.kp_proc.p_flag & P_TRACED) != 0;
        }
        return false;
    }

    std::unique_ptr<google_breakpad::ExceptionHandler> gExceptionHandler = nullptr;

    bool exceptionFilter(void* context) {
        return true;
    }

    bool minidumpCallback(const char* dump_dir,
                          const char* minidump_id,
                          void* context,
                          bool succeeded) {
        if (succeeded) {
            konan::consoleErrorf("Minidump written to \"%s/%s.dmp\"\n", dump_dir, minidump_id);
        } else {
            konan::consoleErrorf("Failed to write minidump to \"%s/%s.dmp\"\n", dump_dir, minidump_id);
            konan::consoleErrorf("%s\n", std::strerror(errno));
        }
        return false; // false -> do not abort the process and let other handlers to be called
    }
#endif
}

void kotlin::crashHandlerInit() noexcept {
#if KONAN_OSX
    if (minidumpsEnabled()) {
        // Don't initialize crash handler if we're running under a debugger
        // to avoid interfering with debugger breakpoints
        if (isDebuggerAttached()) {
            RuntimeLogInfo({logging::Tag::kRT}, "Debugger detected, skipping crash handler initialization");
            return;
        }

        if (compiler::minidumpOnSignal()) {
            installMinidumpSigaction();
        }

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
    }
#endif
}

void kotlin::writeMinidump() noexcept {
#if KONAN_OSX
    if (gExceptionHandler != nullptr) {
        gExceptionHandler->WriteMinidump();
    }
#endif
}
