#include <Foundation/Foundation.h>
#include <stdint.h>

NS_ASSUME_NONNULL_BEGIN

void *__root___SwiftJob_init_allocate();

void __root___SwiftJob_init_initialize(void *, void * cancellationCallback);

void __root___SwiftJob_cancelExternally(void *);

void _kotlin_swift_SwiftFlowIterator_cancel(void * self);

void _kotlin_swift_SwiftFlowIterator_next(void * self, void * continuation, void * exception, void * cancellation);

// Reverse bridges: implemented in Swift, called from Kotlin.
void _kotlin_swift_invokeCancellationCallback(void * pointerToClosure);
void _kotlin_swift_invokeFlowContinuation(void * pointerToClosure, _Bool hasValue, void * _Nullable value);
void _kotlin_swift_invokeFlowException(void * pointerToClosure, void * _Nullable error);

void *_kotlin_swift_SwiftFlowIterator_init_allocate();

void _kotlin_swift_SwiftFlowIterator_init_initialize(void * __kt, void * flow);

void __root___SwiftJob_setCallback(void *, void * cancellationCallback);

void * _Nullable _kotlin_swift_StateFlow_value_get(void * self);

NSArray<NSValue *> * _kotlin_swift_SharedFlow_replayCache_get(void * self);

NS_ASSUME_NONNULL_END
