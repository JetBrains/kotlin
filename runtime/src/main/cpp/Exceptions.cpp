#include <stdio.h>

#include "Assert.h"
#include "Exceptions.h"

#ifdef __cplusplus
extern "C" {
#endif
void ThrowNullPointerException() {
  void* pc = __builtin_return_address(1);
  char message[100];
  snprintf(message, sizeof(message), "NullPointerException at %p", pc);
  fprintf(stderr, "%s\n", message);
  // TODO: throw it for real.
  RuntimeAssert(false, "Throwing is unsupported");
}

void ThrowArrayIndexOutOfBoundsException() {
  void* pc = __builtin_return_address(1);
  char message[100];
  snprintf(message, sizeof(message), "ArrayIndexOutOfBoundsException at %p", pc);
  fprintf(stderr, "%s\n", message);
  // TODO: throw it for real.
  RuntimeAssert(false, "Throwing is unsupported");
}

#ifdef __cplusplus
} // extern "C"
#endif
