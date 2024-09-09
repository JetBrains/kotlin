// Use fenv.h as a small, but relatively sophisticated C standard library header.
// This test intentionally tests against a header from Xcode sysroots on macOS.
#include <fenv.h>

fenv_t useFenvType;