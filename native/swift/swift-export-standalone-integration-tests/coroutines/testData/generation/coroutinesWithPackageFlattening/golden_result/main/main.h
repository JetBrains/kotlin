#include <Foundation/Foundation.h>
#include <stdint.h>

NS_ASSUME_NONNULL_BEGIN

_Bool flattened_testSuspendFunction(_Bool (^continuation)(int32_t), _Bool (^exception)(void * _Nullable ), void * cancellation);

NS_ASSUME_NONNULL_END
