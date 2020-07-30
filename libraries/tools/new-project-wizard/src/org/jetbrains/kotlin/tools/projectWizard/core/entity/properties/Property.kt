/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.core.entity.properties

import org.jetbrains.kotlin.tools.projectWizard.core.entity.Entity
import org.jetbrains.kotlin.tools.projectWizard.core.entity.EntityWithValue
import org.jetbrains.kotlin.tools.projectWizard.core.entity.ValuedEntityContext

class PropertyContext : ValuedEntityContext<Property<Any>>() {
    @Suppress("UNCHECKED_CAST")
    operator fun <V : Any> get(entity: PropertyReference<V>) =
        values[entity.path] as? V

    operator fun <V : Any> set(entity: PropertyReference<V>, value: Any) {
        values[entity.path] = value
    }
}

interface Property<out T : Any> : Entity {
    val defaultValue: T
}

data class InternalProperty<out T : Any>(
    override val path: String,
    override val defaultValue: T,
) : Property<T>, EntityWithValue<T>()

sealed class PropertyImpl<out T : Any> : Property<T>

class PluginProperty<out T : Any>(
    internal: Property<T>
) : PropertyImpl<T>(), Property<T> by internal

class ModuleConfiguratorProperty<out T : Any>(
    internal: Property<T>
) : PropertyImpl<T>(), Property<T> by internal


class PropertyBuilder<T : Any>(
    private val name: String,
    private val defaultValue: T
) {
    fun build(): Property<T> = InternalProperty(name, defaultValue)
}