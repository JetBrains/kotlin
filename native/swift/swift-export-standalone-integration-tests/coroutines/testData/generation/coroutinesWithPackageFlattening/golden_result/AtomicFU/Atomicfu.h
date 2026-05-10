#include <Foundation/Foundation.h>
#include <stdint.h>

NS_ASSUME_NONNULL_BEGIN

void * kotlinx_atomicfu_locks_SynchronizedObject_init_allocate();

_Bool kotlinx_atomicfu_locks_SynchronizedObject_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(void * __kt);

_Bool kotlinx_atomicfu_locks_SynchronizedObject_lock(void * self);

_Bool kotlinx_atomicfu_locks_SynchronizedObject_tryLock(void * self);

_Bool kotlinx_atomicfu_locks_SynchronizedObject_unlock(void * self);

NS_ASSUME_NONNULL_END
