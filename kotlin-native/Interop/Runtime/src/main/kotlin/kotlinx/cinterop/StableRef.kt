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
@Suppress("NON_PUBLIC_PRIMARY_CONSTRUCTOR_OF_INLINE_CLASS", "OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
@ExperimentalForeignApi
public value class StableRef<out T : Any> @PublishedApi internal constructor(
        private val stablePtr: COpaquePointer
) {

    public companion object {

        /**
         * Creates a handle for given object.
         */
        public fun <T : Any> create(any: T): StableRef<T> = StableRef<T>(createStablePointer(any))

    }

    /**
     * Converts the handle to C pointer.
     * @see [asStableRef]
     */
    public fun asCPointer(): COpaquePointer = this.stablePtr

    /**
     * Disposes the handle. It must not be used after that.
     */
    public fun dispose() {
        disposeStablePointer(this.stablePtr)
    }

    /**
     * Returns the object this handle was [created][StableRef.create] for.
     */
    @Suppress("UNCHECKED_CAST")
    public fun get(): T = derefStablePointer(this.stablePtr) as T

}

/**
 * Converts to [StableRef] this opaque pointer produced by [StableRef.asCPointer].
 */
@ExperimentalForeignApi
public inline fun <reified T : Any> CPointer<*>.asStableRef(): StableRef<T> = StableRef<T>(this).also { it.get() }
