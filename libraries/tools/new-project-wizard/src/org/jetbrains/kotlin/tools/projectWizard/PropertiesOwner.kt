/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard

import org.jetbrains.kotlin.tools.projectWizard.core.entity.properties.Property
import org.jetbrains.kotlin.tools.projectWizard.core.entity.properties.PropertyBuilder
import kotlin.properties.ReadOnlyProperty

interface PropertiesOwner {
    fun <T : Any> propertyDelegate(
        create: (path: String) -> PropertyBuilder<T>
    ): ReadOnlyProperty<Any, Property<T>>

    fun <T : Any> property(
        defaultValue: T,
        init: PropertyBuilder<T>.() -> Unit = {}
    ): ReadOnlyProperty<Any, Property<T>> =
        propertyDelegate { path -> PropertyBuilder(path, defaultValue).apply(init) }

    fun <T : Any> listProperty(vararg defaultValues: T, init: PropertyBuilder<List<T>>.() -> Unit = {}) =
        property(defaultValues.toList(), init)
}