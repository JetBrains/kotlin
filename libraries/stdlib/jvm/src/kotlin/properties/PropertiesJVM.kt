/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package kotlin.properties

import java.lang.ref.Reference
import kotlin.reflect.KProperty

/**
 * An extension to delegate a read-only property of type T to an instance of Reference<T>.
 * This extension allows to use instances of Reference for property delegation:
 * `val property: String? by WeakReference(string)`
 */
@kotlin.internal.InlineOnly
inline operator fun <T> Reference<T>.getValue(thisRef: Any?, property: KProperty<*>): T? = get()

/**
 * An extension to delegate a read-only property of type T to an instance of Lazy<Reference<T>>.
 * This extension allows to use instances of Lazy<Reference<T>> for property delegation:
 * `val property: String? by lazy{ WeakReference(string) }`
 */
@kotlin.internal.InlineOnly
inline operator fun <T> Lazy<Reference<out T>>.getValue(thisRef: Any?, property: KProperty<*>): T? {
    return value.get()
}