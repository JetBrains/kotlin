/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.utils

import org.jetbrains.kotlin.idea.frontend.api.Invalidatable
import java.lang.ref.WeakReference
import kotlin.reflect.KProperty

class ReadOnlyWeakRef<V : Any>(value: V, val invalidatable: Invalidatable) {
    val weakRef = WeakReference(value)

    @Suppress("NOTHING_TO_INLINE")
    inline operator fun getValue(thisRef: Any, property: KProperty<*>): V =
        weakRef.get() ?: if (invalidatable.isValid()) {
            error("Cone type was garbage collected while analysis session is still valid")
        } else {
            error("Accessing the invalid coneType")
        }
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <V : Any> Invalidatable.weakRef(value: V) = ReadOnlyWeakRef(value, this)