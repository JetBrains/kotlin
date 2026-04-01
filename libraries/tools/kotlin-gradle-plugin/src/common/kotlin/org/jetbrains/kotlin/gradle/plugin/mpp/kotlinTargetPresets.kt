/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.targets.android.internal.InternalKotlinTargetPreset

internal abstract class KotlinOnlyTargetPreset<R : KotlinOnlyTarget<T>, T : KotlinCompilation<Any>>(
    protected val project: Project,
) : InternalKotlinTargetPreset<R> {

    protected abstract fun createKotlinTargetConfigurator(): AbstractKotlinTargetConfigurator<R>

    protected open fun provideTargetDisambiguationClassifier(target: KotlinOnlyTarget<T>): String? =
        target.targetName

    protected abstract fun instantiateTarget(name: String): R

    override fun createTargetInternal(name: String): R {
        val result = instantiateTarget(name).apply {
            targetName = name
            disambiguationClassifier = provideTargetDisambiguationClassifier(this@apply)
            targetPreset = this@KotlinOnlyTargetPreset

            val compilationFactory = createCompilationFactory(this)
            compilations = project.objects.domainObjectContainer(compilationFactory.itemClass, compilationFactory)
        }

        createKotlinTargetConfigurator().configureTarget(result)
        return result
    }

    protected abstract fun createCompilationFactory(forTarget: R): KotlinCompilationFactory<T>
    protected abstract val platformType: KotlinPlatformType
}