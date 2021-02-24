/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinOnlyTargetConfigurator
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetPreset

abstract class KotlinOnlyTargetPreset<R : KotlinOnlyTarget<T>, T : KotlinCompilation<*>>(
    protected val project: Project,
    protected val kotlinPluginVersion: String
) : KotlinTargetPreset<R> {

    protected abstract fun createKotlinTargetConfigurator(): KotlinOnlyTargetConfigurator<T, R>

    protected open fun provideTargetDisambiguationClassifier(target: KotlinOnlyTarget<T>): String? =
        target.targetName

    // This function is used in IDE import in order to indicate that sourceSetName=disambiguationClassifier+compilationName
    protected open fun useDisambiguationClassifierAsSourceSetNamePrefix() = true

    // This function is used in IDE import in order to override sourceSetName
    protected open fun overrideDisambiguationClassifierOnIdeImport(name: String): String? = null

    abstract protected fun instantiateTarget(name: String): R

    override fun createTarget(name: String): R {
        val result = instantiateTarget(name).apply {
            targetName = name
            disambiguationClassifier = provideTargetDisambiguationClassifier(this@apply)
            useDisambiguationClassifierAsSourceSetNamePrefix = useDisambiguationClassifierAsSourceSetNamePrefix()
            overrideDisambiguationClassifierOnIdeImport = overrideDisambiguationClassifierOnIdeImport(name)
            preset = this@KotlinOnlyTargetPreset

            val compilationFactory = createCompilationFactory(this)
            compilations = project.container(compilationFactory.itemClass, compilationFactory)
        }

        createKotlinTargetConfigurator().configureTarget(result)

        return result
    }

    protected abstract fun createCompilationFactory(forTarget: R): KotlinCompilationFactory<T>
    protected abstract val platformType: KotlinPlatformType
}