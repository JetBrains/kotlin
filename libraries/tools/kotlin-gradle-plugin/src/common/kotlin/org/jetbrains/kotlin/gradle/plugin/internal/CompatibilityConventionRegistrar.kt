/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.internal

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.VariantImplementationFactories
import org.jetbrains.kotlin.gradle.plugin.variantImplementationFactory

/**
 * The concept of conventions in Gradle is outdated and superseded by extensions.
 * However, KGP has to maintain a few conventions for compatibility with old versions of AGP and the `kotlin-dsl` plugin.
 * This registrar adds conventions for the Gradle versions < 8.2, while for Gradle 8.2+ it does nothing.
 */
internal interface CompatibilityConventionRegistrar {
    fun addConvention(subject: Any, name: String, plugin: Any)

    interface Factory : VariantImplementationFactories.VariantImplementationFactory {
        fun getInstance(): CompatibilityConventionRegistrar
    }
}

internal class DefaultCompatibilityConventionRegistrar : CompatibilityConventionRegistrar {
    override fun addConvention(subject: Any, name: String, plugin: Any) {
        // no-op
    }

    class Factory : CompatibilityConventionRegistrar.Factory {
        override fun getInstance() = DefaultCompatibilityConventionRegistrar()

    }
}

internal val Project.compatibilityConventionRegistrar
    get() = variantImplementationFactory<CompatibilityConventionRegistrar.Factory>()
        .getInstance()