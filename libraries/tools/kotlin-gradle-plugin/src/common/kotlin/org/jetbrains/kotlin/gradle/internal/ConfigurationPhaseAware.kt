/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal

import kotlin.reflect.KProperty

/**
 * Legacy property delegate for configuration phase awareness.
 * It was introduced before Gradle's Provider API was available.
 *
 * It should no longer be used. New properties must use the Provider API.
 * TODO: KT-70826
 */
abstract class ConfigurationPhaseAware<C : Any> {

    private var configured: C? = null

    @Synchronized
    fun requireConfigured(): C {
        if (configured == null) {
            configured = finalizeConfiguration()
        }

        return configured!!
    }

    protected fun requireNotConfigured() {
        check(configured == null) { "Configuration already finalized for previous property values" }
    }

    @Deprecated("Internal KGP utility. Scheduled for removal in 2.7.0")
    inner class Property<T>(var value: T) {
        operator fun getValue(receiver: Any, property: KProperty<*>): T = value

        operator fun setValue(receiver: Any, property: KProperty<*>, value: T) {
            requireNotConfigured()
            this.value = value
        }
    }

    /**
     * Adapter to migrate from legacy property delegate to Provider API.
     */
    internal inner class LegacyProperty<T : Any>(
        val getter: org.gradle.api.provider.Provider<T>,
        val setter: (T) -> Unit,
    ) {
        constructor(
            property: org.gradle.api.provider.Property<T>,
        ) : this(
            getter = property,
            setter = property::set
        )

        operator fun getValue(receiver: Any, property: KProperty<*>): T =
            getter.orNull ?: error("Property $property is not initialized")

        operator fun setValue(receiver: Any, property: KProperty<*>, value: T) {
            requireNotConfigured()
            setter(value)
        }
    }

    protected abstract fun finalizeConfiguration(): C
}
