#include <stdio.h>
#include <string.h>
#include <unistd.h>

#include "Assert.h"
#include "Exceptions.h"

#ifdef __cplusplus
extern "C" {
#endif
void ThrowNullPointerException() {
  void* pc = __builtin_return_address(0);
  char message[100];
  snprintf(message, sizeof(message), "NullPointerException at %p", pc);
  write(STDOUT_FILENO, message, strlen(message));
  // TODO: throw it for real.
  RuntimeAssert(false, "Throwing is unsupported");
}

void ThrowArrayIndexOutOfBoundsException() {
  void* pc = __builtin_return_address(0);
  char message[100];
  snprintf(message, sizeof(message), "ArrayIndexOutOfBoundsException at %p", pc);
  write(STDOUT_FILENO, message, strlen(message));
  // TODO: throw it for real.
  RuntimeAssert(false, "Throwing is unsupported");
}

void ThrowClassCastException() {
  void* pc = __builtin_return_address(0);
  char message[100];
  snprintf(message, sizeof(message), "ClassCastException at %p", pc);
  write(STDOUT_FILENO, message, strlen(message));
  // TODO: throw it for real.
  RuntimeAssert(false, "Throwing is unsupported");
}


#ifdef __cplusplus
} // extern "C"
#endif
