/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner

import org.jetbrains.kotlin.arguments.collectProperties
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

internal fun <From : Any, To : From> mergeBeans(from: From, to: To): To {
    // TODO: rewrite when updated version of com.intellij.util.xmlb is available on TeamCity
    @Suppress("UNCHECKED_CAST")
    return copyProperties(from, to, false, collectProperties(from::class as KClass<From>, false))
}

internal fun <From : Any, To : Any> copyProperties(
    from: From,
    to: To,
    deepCopyWhenNeeded: Boolean,
    propertiesToCopy: List<KProperty1<From, Any?>>,
    filter: ((KProperty1<From, Any?>, Any?) -> Boolean)? = null
): To {
    if (from == to) return to

    val toMemberProperties = to::class.memberProperties.associateBy { it.name }

    for (fromProperty in propertiesToCopy) {
        @Suppress("UNCHECKED_CAST")
        val toProperty = toMemberProperties[fromProperty.name] as? KMutableProperty1<To, Any?>
            ?: continue
        val fromValue = fromProperty.get(from)
        if (filter != null && !filter(fromProperty, fromValue)) continue
        toProperty.set(to, if (deepCopyWhenNeeded) fromValue?.copyValueIfNeeded() else fromValue)
    }
    return to
}

private fun Any.copyValueIfNeeded(): Any {
    @Suppress("UNCHECKED_CAST")
    return when (this) {
        is ByteArray -> this.copyOf(size)
        is CharArray -> this.copyOf(size)
        is ShortArray -> this.copyOf(size)
        is IntArray -> this.copyOf(size)
        is LongArray -> this.copyOf(size)
        is FloatArray -> this.copyOf(size)
        is DoubleArray -> this.copyOf(size)
        is BooleanArray -> this.copyOf(size)

        is Array<*> -> java.lang.reflect.Array.newInstance(this::class.java.componentType, size).apply {
            this as Array<Any?>
            (this@copyValueIfNeeded as Array<Any?>).forEachIndexed { i, value -> this[i] = value?.copyValueIfNeeded() }
        }

        is MutableCollection<*> -> (this as Collection<Any?>).mapTo(this::class.java.createNewInstance() as MutableCollection<Any?>) { it?.copyValueIfNeeded() }

        is MutableMap<*, *> -> (this::class.java.createNewInstance() as MutableMap<Any?, Any?>).apply {
            for ([k, v] in this@copyValueIfNeeded.entries) {
                put(k?.copyValueIfNeeded(), v?.copyValueIfNeeded())
            }
        }

        else -> this
    }
}

private fun Class<*>.createNewInstance(): Any {
    val constructor = constructors.first { it.parameterCount == 0 }
    return constructor.newInstance()
}
