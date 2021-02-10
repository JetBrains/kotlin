/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.plugin.Kotlin2JvmSourceSetProcessor
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.addSourcesToKotlinCompileTask
import org.jetbrains.kotlin.gradle.tasks.KotlinTasksProvider
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.utils.dashSeparatedName
import org.jetbrains.kotlin.project.model.refinesClosure
import org.jetbrains.kotlin.project.model.utils.variantsContainingFragment
import java.util.concurrent.Callable

open class KotlinJvmVariantFactory(module: KotlinGradleModule) :
    AbstractKotlinGradleVariantWithRuntimeDependenciesFactory<KotlinJvmVariant>(module) {
    override fun instantiateFragment(name: String) = KotlinJvmVariant(module, name)

    // FIXME expose the JAR with the artifacts API
    private fun getOrCreateJarTask(fragment: KotlinJvmVariant): TaskProvider<Jar> {
        val jarTaskName = fragment.disambiguateName("jar")
        return project.locateOrRegisterTask(jarTaskName) {
            it.from(fragment.compilationOutputs.allOutputs)
            it.archiveClassifier.set(dashSeparatedName(fragment.name, module.moduleClassifier))
        }
    }

    override fun createElementsConfigurations(fragment: KotlinJvmVariant) {
        super.createElementsConfigurations(fragment)
        listOf(fragment.apiElementsConfigurationName, fragment.runtimeElementsConfigurationName).forEach {
            project.artifacts.add(it, getOrCreateJarTask(fragment))
        }
    }

    override fun configureKotlinCompilation(fragment: KotlinJvmVariant) {
        val compilationData = KotlinJvmVariantCompilationData(fragment)

        val classesTaskName = compilationData.compileAllTaskName
        project.tasks.register(classesTaskName) { classesTask ->
            classesTask.dependsOn(fragment.compilationOutputs.allOutputs)
        }

        Kotlin2JvmSourceSetProcessor(KotlinTasksProvider(), compilationData, project.getKotlinPluginVersion() ?: "unknown"/*TODO*/).run()

        val allSources = { project.files(Callable { fragment.refinesClosure.map { it.kotlinSourceRoots } }) }
        val commonSources = {
            project.files(Callable {
                fragment.refinesClosure.filter { module.variantsContainingFragment(it).count() > 1 }.map { it.kotlinSourceRoots }
            })
        }

        addSourcesToKotlinCompileTask(
            project, compilationData.compileKotlinTaskName, /*FIXME*/ emptyList(), allSources,
            addAsCommonSources = lazyOf(false)
        )
        addSourcesToKotlinCompileTask(
            project, compilationData.compileKotlinTaskName, /*FIXME*/ emptyList(), commonSources,
            addAsCommonSources = lazyOf(true)
        )
    }
}

//endregion Variant