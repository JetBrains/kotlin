/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.internal

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.VariantImplementationFactories
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinTarget

/**
 * Additional configuration for 'maven' Gradle plugin.
 * Documentation: https://docs.gradle.org/6.0/userguide/maven_plugin.html
 */
interface MavenPluginConfigurator {
    fun applyConfiguration(
        project: Project,
        target: AbstractKotlinTarget,
        shouldRewritePoms: Provider<Boolean>
    )

    interface MavenPluginConfiguratorVariantFactory : VariantImplementationFactories.VariantImplementationFactory {
        fun getInstance(): MavenPluginConfigurator
    }

    class DefaultMavenPluginConfiguratorVariantFactory : MavenPluginConfiguratorVariantFactory {
        override fun getInstance(): MavenPluginConfigurator = object : MavenPluginConfigurator {
            override fun applyConfiguration(
                project: Project,
                target: AbstractKotlinTarget,
                shouldRewritePoms: Provider<Boolean>
            ) = Unit
        }
    }
}
