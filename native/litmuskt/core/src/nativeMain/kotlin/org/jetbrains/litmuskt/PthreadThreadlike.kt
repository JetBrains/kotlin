package org.jetbrains.litmuskt

import kotlinx.cinterop.*
import kpthread.k_pthread_create
import kpthread.k_pthread_join
import kpthread.k_pthread_t_alloc
import kpthread.k_pthread_t_free

@OptIn(ExperimentalForeignApi::class)
class PthreadThreadlike : Threadlike {

    val pthreadPtr = k_pthread_t_alloc() ?: error("could not allocate pthread_t pointer")

    private class ThreadData<A : Any>(val args: A, val function: (A) -> Unit)

    override fun <A : Any> start(args: A, function: (A) -> Unit): BlockingFuture {
        val threadData = ThreadData(args, function)
        val threadDataRef = StableRef.create(threadData)

        k_pthread_create(
            pthreadPtr,
            staticCFunction<COpaquePointer?, COpaquePointer?> {
                val data = it!!.asStableRef<ThreadData<A>>().get()
                data.function(data.args)
                return@staticCFunction null
            },
            threadDataRef.asCPointer()
        ).syscallCheck()

        return BlockingFuture {
            k_pthread_join(pthreadPtr, null).syscallCheck()
            threadDataRef.dispose()
        }
    }

    override fun dispose() {
        k_pthread_t_free(pthreadPtr)
    }
}
