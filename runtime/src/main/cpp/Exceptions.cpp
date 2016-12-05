#include <execinfo.h>
#include <stdlib.h>
#include <string.h>

#include "Assert.h"
#include "Exceptions.h"
#include "Natives.h"
#include "Types.h"

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

// TODO: it seems to be very common case; does C++ std library provide something like this?
class AutoFree {
 private:
  void* mem_;

 public:
  AutoFree(void* mem): mem_(mem) {}

  ~AutoFree() {
    free(mem_);
  }
};

// TODO: this method ignores the encoding
KString CreateKotlinStringFromCString(const char* str) {
  return AllocStringInstance(SCOPE_GLOBAL, str, strlen(str));
}

#ifdef __cplusplus
extern "C" {
#endif

// TODO: this implementation is just a hack, e.g. the result is inexact;
// however it is better to have an inexact stacktrace than not to have any.
KArrayRef GetCurrentStackTrace() {
  const int maxSize = 32;
  void* buffer[maxSize];

  int size = backtrace(buffer, maxSize);
  char** symbols = backtrace_symbols(buffer, size);
  RuntimeAssert(symbols != nullptr, "Not enough memory to retrieve the stacktrace");

  AutoFree autoFree(symbols);
  KArrayRef result = AllocArrayInstance(theArrayTypeInfo, SCOPE_GLOBAL, size);

  for (int i = 0; i < size; ++i) {
    KString symbol = CreateKotlinStringFromCString(symbols[i]);
    Kotlin_Array_set(result, i, symbol);
  }

  return result;
}

void ThrowException(KRef exception) {
  RuntimeAssert(exception != nullptr && IsInstance(exception, theThrowableTypeInfo),
                "Throwing something non-throwable");

  throw KotlinException(exception);
}


#ifdef __cplusplus
} // extern "C"
#endif
