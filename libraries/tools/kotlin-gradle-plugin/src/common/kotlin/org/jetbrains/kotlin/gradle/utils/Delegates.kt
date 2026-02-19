/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import kotlin.reflect.KProperty

class PropertyDelegate<T : Any>(
    private val defaultValueProvider: () -> T
) {
    private var backing: T? = null

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return backing ?: defaultValueProvider()
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        backing = value
    }
}

fun <T : Any> property(
    defaultValueProvider: () -> T
): PropertyDelegate<T> = PropertyDelegate(defaultValueProvider)

/**
 * Similar to [SynchronizedLazyImpl] but doesn't implement [Serializable] in the way
 * that value gets initialised upon serialisation.
 * It is intended that [initializer] gets serialised.
 * Reason: Sometimes Gradle Configuration Cache can't serialise some entities that
 * are produced by [initializer] but is okay serialising [initializer]
 */
internal class TransientLazy<T: Any>(
    private val initializer: () -> T
) : Lazy<T> {
    @Volatile
    @Transient
    private var _value: T? = null
    override fun isInitialized(): Boolean = _value != null
    override val value get(): T {
        val v1 = _value
        if (v1 != null) return v1

        return synchronized(this) {
            val v2 = _value
            if (v2 == null) {
                initializer().also { _value = it }
            } else {
                v2
            }
        }
    }
}