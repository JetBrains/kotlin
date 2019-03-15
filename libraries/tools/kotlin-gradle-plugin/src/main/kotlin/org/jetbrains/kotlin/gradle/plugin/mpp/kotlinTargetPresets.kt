/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.internal.file.FileResolver
import org.gradle.internal.reflect.Instantiator
import org.jetbrains.kotlin.gradle.plugin.*

abstract class KotlinOnlyTargetPreset<T : KotlinCompilation<*>>(
    protected val project: Project,
    private val instantiator: Instantiator,
    private val fileResolver: FileResolver,
    protected val kotlinPluginVersion: String
) : KotlinTargetPreset<KotlinOnlyTarget<T>> {

    protected open fun createKotlinTargetConfigurator(): KotlinTargetConfigurator<T> =
        KotlinTargetConfigurator(createDefaultSourceSets = true, createTestCompilation = true)

    override fun createTarget(name: String): KotlinOnlyTarget<T> {
        val result = KotlinOnlyTarget<T>(project, platformType).apply {
            targetName = name
            disambiguationClassifier = name
            preset = this@KotlinOnlyTargetPreset

            val compilationFactory = createCompilationFactory(this)
            compilations = project.container(compilationFactory.itemClass, compilationFactory)
        }

        createKotlinTargetConfigurator().configureTarget(result)

        result.compilations.all { compilation ->
            buildCompilationProcessor(compilation).run()
            if (compilation.name == KotlinCompilation.MAIN_COMPILATION_NAME) {
                sourcesJarTask(compilation, result.targetName, result.targetName.toLowerCase())
            }
        }

        return result
    }

    protected abstract fun createCompilationFactory(forTarget: KotlinOnlyTarget<T>): KotlinCompilationFactory<T>
    protected abstract val platformType: KotlinPlatformType
    internal abstract fun buildCompilationProcessor(compilation: T): KotlinSourceSetProcessor<*>
}