#include <stdio.h>
#include <string.h>
#include <unistd.h>

#include "Assert.h"
#include "Exceptions.h"
#include "Types.h"

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

class KotlinException {
 public:

  KRef exception_;

  KotlinException(KRef exception) : exception_(exception) {
      ::AddRef(exception_->container());
  };

  ~KotlinException() {
      ::Release(exception_->container());
  };
};

void ThrowException(KRef exception) {
  RuntimeAssert(exception != nullptr && IsInstance(exception, theThrowableTypeInfo),
                "Throwing something non-throwable");

  throw KotlinException(exception);
}


#ifdef __cplusplus
} // extern "C"
#endif
