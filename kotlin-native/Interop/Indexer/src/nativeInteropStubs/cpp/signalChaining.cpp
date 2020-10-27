#if defined(__linux__) || defined(__APPLE__)
#include <dlfcn.h>
#if defined(__linux__)
#include <link.h>
#endif
#include <stdio.h>
#include <signal.h>

extern "C" void clang_toggleCrashRecovery(unsigned isEnabled);

constexpr int signalsToCover[] = {
  SIGILL, SIGFPE, SIGSEGV, SIGBUS, SIGUSR1, SIGUSR2
};

struct {
  void* handler;
  bool isSigaction;
} oldSignalHandlers[sizeof(signalsToCover)/sizeof(signalsToCover[0])] = { 0 };

static int mySigaction(int sig, const struct sigaction *act, struct sigaction * oact) {
  for (int i = 0; i < sizeof(signalsToCover)/sizeof(signalsToCover[0]); i++) {
    if (sig == signalsToCover[i]) return 0;
  }
  return sigaction(sig, act, oact);
}

static void checkSignalChaining() {
   struct sigaction oact;
   clang_toggleCrashRecovery(1);
   for (int i = 0; i < sizeof(signalsToCover)/sizeof(signalsToCover[0]); i++) {
     int sig = signalsToCover[i];
     if (sigaction(sig, nullptr, &oact) != 0) continue;
     if ((oact.sa_flags & SA_SIGINFO) == 0) {
       if (oldSignalHandlers[i].isSigaction) {
         fprintf(stderr, "ERROR: improperly changed signal flag for %d\n", sig);
         continue;
       }
       if (oldSignalHandlers[i].handler != (void*)oact.sa_handler) {
         fprintf(stderr, "ERROR: improperly changed signal handler for %d\n", sig);
         continue;
       }
     } else {
       if (!oldSignalHandlers[i].isSigaction) {
         fprintf(stderr, "ERROR: improperly changed signal flag for %d\n", sig);
         continue;
       }
       void* action = (void*)oact.sa_sigaction;
       if (oldSignalHandlers[i].handler != action) {
         Dl_info info;
         const char* soname = "<unknown>";
         if (dladdr(action, &info) != 0) {
           soname = info.dli_fname;
         }
         fprintf(stderr, "ERROR: changed signal handler for %d from %p to %p: coming from %s\n",
            sig, oldSignalHandlers[i].handler, action, soname);
       }
     }
   }
   clang_toggleCrashRecovery(0);
}

__attribute__((constructor))
static void initSignalChaining() {
  void** base = 0;
  Dl_info info;

  for (int i = 0; i < sizeof(signalsToCover)/sizeof(signalsToCover[0]); i++) {
    struct sigaction oact;
    int sig = signalsToCover[i];
    if (sigaction(signalsToCover[i], nullptr, &oact) == 0) {
      if ((oact.sa_flags & SA_SIGINFO) == 0) {
        oldSignalHandlers[i] = {(void*)oact.sa_handler, false};
      } else {
        oldSignalHandlers[i] = {(void*)oact.sa_sigaction, true};
      }
    }
  }


  if (dladdr((void*)&clang_toggleCrashRecovery, &info) == 0) return;
  base = (void**)info.dli_fbase;

  // Force resolving of lazy symbols.
  clang_toggleCrashRecovery(1);
  clang_toggleCrashRecovery(0);

  // And then patch GOT.
#if defined(__linux__)
  {
    // On Linux we have to be a bit tricky, as there's unmapped gap between code and GOT.
     struct link_map* linkmap = 0;
     if (dladdr1((void*)&clang_toggleCrashRecovery, &info, (void**)&linkmap, RTLD_DL_LINKMAP) == 0) return;
     base = (void**)linkmap->l_ld;
  }
#endif
  for (int index = 0, patched = 0; patched < 1; index++) {
    void* value = base[index];
    if (value == &sigaction) {
        base[index] = (void*)mySigaction;
        patched++;
    }
    if (value == mySigaction) {
        patched++;
    }
  }

  checkSignalChaining();
}

#endif // defined(__linux__) || defined(__APPLE__)
