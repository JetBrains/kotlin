/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package sample.videoplayer

import kotlinx.cinterop.*

/**
 * Disposable class manages and owns all its native resources. It allocates some resources
 * during construction and may allocate some additional resources during operation.
 * It must free all its resource once [dispose] is invoked. "Disposed" is a final state of the disposable
 * class. It is not supposed to be used after being disposed.
 */
interface Disposable {
    /**
     * Disposes all native resources owned by this class. This function must be invoked
     * exactly once as the last operation on the corresponding class.
     */
    fun dispose()
}

/**
 * Helper class to implement [Disposable] interface. It contains an [arena] for native
 * memory allocations and a number of helper methods to simplify management of other
 * kinds of native resources.
 *
 * It is important to wrap all potentially exception-throwing code in the class constructor
 * into [tryConstruct] invocation, because, when object construction fails with exception,
 * it ensures that all the resource that were allocated so far will get freed.
 */
abstract class DisposableContainer : Disposable {
    val arena = Arena()
    
    override fun dispose() {
        arena.clear()
    }

    inline fun <T> tryConstruct(init: () -> T): T =
        try { init() }
        catch (e: Throwable) {
            dispose()
            throw e
        }

    inline fun <T> disposable(
        message: String = "disposable",
        create: () -> T?,
        crossinline dispose: (T) -> Unit
    ): T =
        tryConstruct {
            create()?.also {
                arena.defer { dispose(it) }
            } ?: throw Error(message)
        }

    inline fun <T : Disposable> disposable(create: () -> T): T =
        disposable(
            create = create,
            dispose = { it.dispose() })

    inline fun <T : CPointed> sdlDisposable(
            message: String,
            ptr: CPointer<T>?,
            crossinline dispose: (CPointer<T>) -> Unit): CPointer<T> =
        disposable(
            create = { ptr ?: throwSDLError(message) },
            dispose = dispose)
}
