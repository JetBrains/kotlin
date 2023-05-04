/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.cinterop

/**
 * This class provides a way to create a stable handle to any Kotlin object.
 * After [converting to CPointer][asCPointer] it can be safely passed to native code e.g. to be received
 * in a Kotlin callback.
 *
 * Any [StableRef] should be manually [disposed][dispose]
 */
@Suppress("NON_PUBLIC_PRIMARY_CONSTRUCTOR_OF_INLINE_CLASS")
@kotlin.jvm.JvmInline
@ExperimentalForeignApi
public value class StableRef<out T : Any> @PublishedApi internal constructor(
        private val stablePtr: COpaquePointer
) {

    companion object {

        /**
         * Creates a handle for given object.
         */
        fun <T : Any> create(any: T) = StableRef<T>(createStablePointer(any))

    }

    /**
     * Converts the handle to C pointer.
     * @see [asStableRef]
     */
    fun asCPointer(): COpaquePointer = this.stablePtr

    /**
     * Disposes the handle. It must not be used after that.
     */
    fun dispose() {
        disposeStablePointer(this.stablePtr)
    }

    /**
     * Returns the object this handle was [created][StableRef.create] for.
     */
    @Suppress("UNCHECKED_CAST")
    fun get() = derefStablePointer(this.stablePtr) as T

}

/**
 * Converts to [StableRef] this opaque pointer produced by [StableRef.asCPointer].
 */
@ExperimentalForeignApi
inline fun <reified T : Any> CPointer<*>.asStableRef(): StableRef<T> = StableRef<T>(this).also { it.get() }
