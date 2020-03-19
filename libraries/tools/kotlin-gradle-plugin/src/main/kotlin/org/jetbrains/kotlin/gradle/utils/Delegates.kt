/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import kotlin.reflect.KProperty

open class ProviderDelegate<out T : Any>(
    private val defaultValueProvider: () -> T
) {
    protected open val backing: T? = null

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return backing ?: defaultValueProvider()
    }
}

class PropertyDelegate<T : Any>(
    defaultValueProvider: () -> T
) : ProviderDelegate<T>(defaultValueProvider) {
    override var backing: T? = null

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        backing = value
    }
}

fun <T : Any> provider(
    defaultValueProvider: () -> T
): ProviderDelegate<T> = ProviderDelegate(defaultValueProvider)

fun <T : Any> property(
    defaultValueProvider: () -> T
): PropertyDelegate<T> = PropertyDelegate(defaultValueProvider)