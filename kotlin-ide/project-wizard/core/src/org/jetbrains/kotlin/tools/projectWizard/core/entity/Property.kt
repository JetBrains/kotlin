package org.jetbrains.kotlin.tools.projectWizard.core.entity

class PropertyContext : ValuedEntityContext<Property<Any>>()

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

class PropertyBuilder<T : Any>(
    private val name: String,
    private val defaultValue: T
) {
    fun build(): Property<T> = InternalProperty(name, defaultValue)
}