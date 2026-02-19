#include <Foundation/Foundation.h>
#include <stdint.h>

NS_ASSUME_NONNULL_BEGIN

void flattened_testSuspendFunction(int32_t (^continuation)(int32_t), int32_t (^exception)(void * _Nullable ), void * cancellation);

NS_ASSUME_NONNULL_END
