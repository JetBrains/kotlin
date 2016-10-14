#include <stdio.h>
#include <stdlib.h>

void RuntimeAssertFailed(const char* location, const char* message) {
  // TODO: produce stacktrace and such.
  fprintf(stderr, "%s: runtime assert: %s\n", location, message);
  abort();
}
