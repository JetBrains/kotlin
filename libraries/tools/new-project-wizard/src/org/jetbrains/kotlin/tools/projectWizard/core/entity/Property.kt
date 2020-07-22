package org.jetbrains.kotlin.tools.projectWizard.core.entity

class PropertyContext : ValuedEntityContext<Property<Any>>()

data class Property<out T : Any>(
    override val path: String,
    val defaultValue: T
) : EntityWithValue<T>() {
    class Builder<T : Any>(
        private val name: String,
        private val defaultValue: T
    ) {
        fun build(): Property<T> = Property(name, defaultValue)
    }
}
