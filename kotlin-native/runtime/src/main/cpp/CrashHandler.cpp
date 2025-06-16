#include "CrashHandler.hpp"

#include <csignal>
#include <cstdlib>
#include <array>
#include <mutex>

#include "KAssert.h"
#include "Porting.h"
#include "Logging.hpp"

#if KONAN_APPLE
#include "client/mac/handler/minidump_generator.h"
#include <mach/exception_types.h>
#include <mach/mach_init.h>
#endif

namespace {
    constexpr int kSignals[] = {
            //SIGABRT,
            SIGBUS,
            SIGFPE,
            SIGILL,
            SIGSEGV,
            //SIGTRAP
    };
    constexpr auto kNumSignals = std::size(kSignals);
    struct sigaction prevActions[kNumSignals];

    // TODO what if we crush from two threads simultaneously?

    bool minidumpsEnabled() {
        return kotlin::compiler::miniDumpFile() != nullptr;
    }

#if KONAN_APPLE
    bool WriteMinidumpWithException(breakpad_ucontext_t *task_context) {
        google_breakpad::MinidumpGenerator md(mach_task_self(), MACH_PORT_NULL);
        md.SetTaskContext(task_context);
        // FIXME macOS version of minidump generator expect us to catch mach exceptions instead of POSIX signals
        md.SetExceptionInformation(EXC_SOFTWARE, MD_EXCEPTION_CODE_MAC_ABORT, 0, mach_thread_self());
        const char* dumpFile = kotlin::compiler::miniDumpFile();
        if (dumpFile == nullptr) {
            konan::consoleErrorf("No minidump will be written\n");
            return false;
        }

        bool written = md.Write(dumpFile);
        if (written) {
            konan::consoleErrorf("Minidump written to \"%s\"\n", dumpFile);
        } else {
            konan::consoleErrorf("Failed to write minidump to \"%s\"\n", dumpFile);
        }
        return written;
    }

    void CrashRecoverySignalHandler(int signal, siginfo_t* info, void* uc) {
        // TODO llvm checks for context here

        // unblock the signal handling
        sigset_t sigMask;
        sigemptyset(&sigMask);
        sigaddset(&sigMask, signal);
        sigprocmask(SIG_UNBLOCK, &sigMask, nullptr); // TODO ONSTACK?

        // TODO doublecheck that we are not out of bounds here
        konan::consoleErrorf("A fatal error has been detected: %s\n", sys_siglist[signal]);
        // TODO chack manually, that a SEGFAULT somewhere here will be properly handled
        WriteMinidumpWithException(static_cast<breakpad_ucontext_t*>(uc));
        std::abort();
    }

    void installExceptionOrSignalHandlers() {
        struct sigaction handler{};
        handler.sa_sigaction = CrashRecoverySignalHandler;
        handler.sa_flags = SA_SIGINFO;
        sigemptyset(&handler.sa_mask);

        for (unsigned i = 0; i != kNumSignals; ++i) {
            sigaction(kSignals[i], &handler, &prevActions[i]);
        }
    }

    // TODO should we do this somewhere?
    [[maybe_unused]] void uninstallExceptionOrSignalHandlers() {
        for (unsigned i = 0; i != kNumSignals; ++i) {
            sigaction(kSignals[i], &prevActions[i], nullptr);
        }
    }
#endif
}

void kotlin::crashHandlerInit() noexcept {
    if (minidumpsEnabled()) {
#if KONAN_APPLE
        RuntimeLogInfo({logging::Tag::kRT}, "Initializing crash handler, minidumps will be written to \"%s\"", compiler::miniDumpFile());
        installExceptionOrSignalHandlers();
#endif
    }
}

void kotlin::writeMinidump() noexcept {
    if (minidumpsEnabled()) {
#if KONAN_APPLE
        WriteMinidumpWithException(nullptr);
#endif
    }
}
