package org.jetbrains.kotlin.tools.projectWizard.core.entity

import org.jetbrains.kotlin.tools.projectWizard.core.Plugin
import kotlin.reflect.KProperty1

typealias PropertyReference<T> = KProperty1<out Plugin, Property<T>>

class PropertyContext : ValuedEntityContext<Property<Any>, PropertyReference<Any>>()


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
