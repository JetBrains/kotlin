/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty

open class ProviderDelegate<out T : Any>(
    protected open val backing: KProperty<T?>,
    private val defaultValueProvider: () -> T
) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return backing.getter.call() ?: defaultValueProvider()
    }
}

class PropertyDelegate<T : Any>(
    override val backing: KMutableProperty<T?>,
    defaultValueProvider: () -> T
) : ProviderDelegate<T>(backing, defaultValueProvider) {
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        backing.setter.call(value)
    }
}

fun <T : Any> provider(
    backing: KProperty<T?>,
    defaultValueProvider: () -> T
): ProviderDelegate<T> = ProviderDelegate(backing, defaultValueProvider)

fun <T : Any> property(
    backing: KMutableProperty<T?>,
    defaultValueProvider: () -> T
): PropertyDelegate<T> = PropertyDelegate(backing, defaultValueProvider)