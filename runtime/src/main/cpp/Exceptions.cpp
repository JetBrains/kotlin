#include "Assert.h"
#include "Exceptions.h"
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

#ifdef __cplusplus
extern "C" {
#endif

void ThrowException(KRef exception) {
  RuntimeAssert(exception != nullptr && IsInstance(exception, theThrowableTypeInfo),
                "Throwing something non-throwable");

  throw KotlinException(exception);
}


#ifdef __cplusplus
} // extern "C"
#endif
