#include <Foundation/Foundation.h>
#include <stdint.h>

NS_ASSUME_NONNULL_BEGIN

void *__root___SwiftJob_init_allocate();

void __root___SwiftJob_init_initialize(void *, bool (^)(bool));

void __root___SwiftJob_cancelExternally(void *);

void _kotlin_swift_SwiftFlowIterator_cancel(void * self);

void _kotlin_swift_SwiftFlowIterator_next(void * self, int32_t (^continuation)(void * _Nullable ), int32_t (^exception)(void * _Nullable ), void * cancellation);

void *_kotlin_swift_SwiftFlowIterator_init_allocate();

void _kotlin_swift_SwiftFlowIterator_init_initialize(void * __kt, void * flow);

NS_ASSUME_NONNULL_END
