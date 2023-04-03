/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.ref

import kotlin.experimental.ExperimentalNativeApi

/**
 * Class WeakReference encapsulates weak reference to an object, which could be used to either
 * retrieve a strong reference to an object, or return null, if object was already destroyed by
 * the memory manager.
 */
@ExperimentalNativeApi
public class WeakReference<T : Any> {
    /**
     * Creates a weak reference object pointing to an object. Weak reference doesn't prevent
     * removing object, and is nullified once object is collected.
     */
    constructor(referred: T) {
        pointer = getWeakReferenceImpl(referred)
    }

    /**
     * Backing store for the object pointer, inaccessible directly.
     */
    @PublishedApi
    internal var pointer: WeakReferenceImpl?

    /**
     * Clears reference to an object.
     */
    public fun clear() {
        pointer = null
    }

    /**
     * Returns either reference to an object or null, if it was collected.
     */
    @Suppress("UNCHECKED_CAST")
    public fun get(): T? = pointer?.get() as T?

    /**
     * Returns either reference to an object or null, if it was collected.
     */
    public val value: T?
        get() = this.get()
}

