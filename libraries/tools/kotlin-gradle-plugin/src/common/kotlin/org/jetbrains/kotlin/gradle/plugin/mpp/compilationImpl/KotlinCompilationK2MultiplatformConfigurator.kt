/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.SetProperty
import org.jetbrains.kotlin.gradle.dependencies.dependencies
import org.jetbrains.kotlin.gradle.dsl.usesK2
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.launch
import org.jetbrains.kotlin.gradle.plugin.mpp.awaitAllKotlinSourceSets
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.factory.KotlinCompilationImplFactory
import org.jetbrains.kotlin.gradle.plugin.sources.android.androidSourceSetInfoOrNull
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.tasks.K2MultiplatformCompilationTask
import org.jetbrains.kotlin.gradle.tasks.K2MultiplatformStructure

internal object KotlinCompilationK2MultiplatformConfigurator : KotlinCompilationImplFactory.PreConfigure {
    /**
     * Returns fragment name of [this]
     * by default it is name of [KotlinSourceSet] but for android it should name of compilation's default source set.
     * i.e. all android-specific source sets (fragments) should be combined into one.
     * See KT-62508 for detailed explanation
     */
    private fun KotlinSourceSet.fragmentName(): String =
        if (androidSourceSetInfoOrNull != null) {
            androidSourceSetInfoOrNull!!.androidVariantType.name
        } else {
            name
        }

    private fun <T : Any> K2MultiplatformCompilationTask.setIfK2Enabled(
        propertyGetter: K2MultiplatformStructure.() -> SetProperty<T>,
        value: () -> Iterable<T>,
    ): SetProperty<T> {
        val property = multiplatformStructure.propertyGetter()
        property.set(compilerOptions.usesK2.map {
            @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS") // looks like a bug
            if (it) value() else null
        })
        return property
    }

    private fun KotlinCompilationImpl.fetchRefinesEdges(): List<K2MultiplatformStructure.RefinesEdge> {
        return allKotlinSourceSets.flatMap { sourceSet ->
            sourceSet.dependsOn.mapNotNull { dependsOn ->
                val from = sourceSet.fragmentName()
                val to = dependsOn.fragmentName()
                if (from == to) return@mapNotNull null
                K2MultiplatformStructure.RefinesEdge(from, to)
            }
        }
    }

    override fun configure(compilation: KotlinCompilationImpl) {
        val project = compilation.project
        project.launch {
            val fragments = compilationFragments(compilation, project)
            compilation.compileTaskProvider.configure configureTask@{ compileTask ->
                if (compileTask !is K2MultiplatformCompilationTask) return@configureTask
                compileTask.setIfK2Enabled(K2MultiplatformStructure::refinesEdges) { compilation.fetchRefinesEdges() }
                compileTask.multiplatformStructure.fragments.set(fragments)
                compileTask.multiplatformStructure.defaultFragmentName.set(compilation.defaultSourceSet.fragmentName())
            }
        }
    }

    private suspend fun compilationFragments(
        compilation: KotlinCompilationImpl,
        project: Project,
    ): List<K2MultiplatformStructure.Fragment> {
        return compilation.awaitAllKotlinSourceSets()
            .flatMap { it.internal.withDependsOnClosure }
            .groupBy { it.fragmentName() }
            .map { (fragmentName, sourceSets) ->
                val sourceFiles = sourceSets.map { it.kotlin.asFileTree }.reduce { acc, fileTree -> acc + fileTree }
                val dependencies = if (project.kotlinPropertiesProvider.separateKmpCompilation.get()) {
                    project.fragmentDependencies(sourceSets)
                } else {
                    project.files()
                }

                K2MultiplatformStructure.Fragment(
                    fragmentName,
                    sourceFiles,
                    dependencies
                )
            }
    }

    private suspend fun Project.fragmentDependencies(sourceSets: List<KotlinSourceSet>): FileCollection = sourceSets
        .flatMap { it.dependencies() }
        .fold(objects.fileCollection()) { fileCollection, dependency -> fileCollection.from(dependency.files) }
}
