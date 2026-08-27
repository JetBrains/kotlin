/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFederation

import org.gradle.api.plugins.ExtensionAware
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.jvmName

internal inline fun <R : ExtensionAware, reified T> extensionProperty(
    crossinline default: R.() -> T,
): ReadWriteProperty<R, T> {
    return object : ReadWriteProperty<R, T> {
        val name = this::class.jvmName

        override fun getValue(thisRef: R, property: KProperty<*>): T {
            if (!thisRef.extensions.extraProperties.has(name)) {
                thisRef.extensions.extraProperties.set(name, thisRef.default())
            }

            return thisRef.extensions.extraProperties.get(name) as T
        }

        override fun setValue(thisRef: R, property: KProperty<*>, value: T) {
            thisRef.extensions.extraProperties.set(property.name, value)
        }
    }
}
