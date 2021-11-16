/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.external

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.AbstractKotlinTargetConfigurator
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinCompilationFactory
import org.jetbrains.kotlin.gradle.targets.external.DefaultSourceSetNameOption.KotlinConvention

internal class KotlinJvmExternalCompilationFactory(
    val project: Project,
    val target: KotlinExternalTarget,
) : KotlinCompilationFactory<KotlinJvmExternalCompilation> {

    override val itemClass: Class<KotlinJvmExternalCompilation>
        get() = KotlinJvmExternalCompilation::class.java

    override fun create(name: String): KotlinJvmExternalCompilation {
        return create(name, defaultSourceSetNameOption = KotlinConvention)
    }

    internal fun create(
        name: String,
        defaultSourceSetNameOption: DefaultSourceSetNameOption = KotlinConvention
    ): KotlinJvmExternalCompilation {
        return KotlinJvmExternalCompilation(target, name, defaultSourceSetNameOption).also { compilation ->
            // Configurations
            AbstractKotlinTargetConfigurator.defineConfigurationsForCompilation(compilation)

            // Source Set
            val defaultSourceSet = project.kotlinExtension.sourceSets.maybeCreate(compilation.defaultSourceSetName)
            compilation.source(defaultSourceSet)

            // Dependencies
            compilation.compileDependencyFiles = project.configurations.getByName(compilation.compileDependencyConfigurationName)
        }
    }
}
