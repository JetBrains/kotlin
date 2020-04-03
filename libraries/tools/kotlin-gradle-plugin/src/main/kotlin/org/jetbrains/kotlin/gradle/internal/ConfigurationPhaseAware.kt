/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal

import kotlin.reflect.KProperty

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

    inner class Property<T>(var value: T) {
        operator fun getValue(receiver: Any, property: KProperty<*>): T = value

        operator fun setValue(receiver: Any, property: KProperty<*>, value: T) {
            requireNotConfigured()
            this.value = value
        }
    }

    protected abstract fun finalizeConfiguration(): C
}