/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.model.builder

import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.file.SourceDirectorySet
import org.gradle.tooling.provider.model.ToolingModelBuilder
import org.jetbrains.kotlin.gradle.model.CompilerArguments
import org.jetbrains.kotlin.gradle.model.KotlinProject
import org.jetbrains.kotlin.gradle.model.SourceSet
import org.jetbrains.kotlin.gradle.model.impl.CompilerArgumentsImpl
import org.jetbrains.kotlin.gradle.model.impl.KotlinProjectImpl
import org.jetbrains.kotlin.gradle.model.impl.SourceSetImpl
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.internal.JavaSourceSetsAccessor
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile

/**
 * [ToolingModelBuilder] for [KotlinProject] models.
 * This model builder is registered for base Kotlin JVM (including Android), Kotlin JS and Kotlin Common plugins.
 *
 * [androidTarget] should always be null for none Android plugins.
 */
class KotlinModelBuilder(private val kotlinPluginVersion: String, private val androidTarget: KotlinAndroidTarget?) : ToolingModelBuilder {

    override fun canBuild(modelName: String): Boolean {
        return modelName == KotlinProject::class.java.name
    }

    override fun buildAll(modelName: String, project: Project): Any {
        require(canBuild(modelName)) { "buildAll(\"$modelName\") has been called while canBeBuild is false" }
        val kotlinCompileTasks = project.tasks.withType(AbstractKotlinCompile::class.java).toList()
        val projectType = getProjectType(project)
        return KotlinProjectImpl(
            project.name,
            kotlinPluginVersion,
            projectType,
            kotlinCompileTasks.mapNotNull {
                if (androidTarget != null) it.createAndroidSourceSet(androidTarget) else it.createSourceSet(project, projectType)
            },
            getExpectedByDependencies(project),
        )
    }

    companion object {
        private fun Project.isAndroid(): Boolean {
            return project.plugins.hasPlugin("kotlin-android")
        }

        private fun getProjectType(project: Project): KotlinProject.ProjectType {
            return if (project.plugins.hasPlugin("kotlin") || project.plugins.hasPlugin("kotlin-platform-jvm") ||
                project.isAndroid()
            ) {
                KotlinProject.ProjectType.PLATFORM_JVM
            } else if (project.plugins.hasPlugin("kotlin2js") || project.plugins.hasPlugin("kotlin-platform-js")) {
                KotlinProject.ProjectType.PLATFORM_JS
            } else {
                KotlinProject.ProjectType.PLATFORM_COMMON
            }
        }

        private fun getExpectedByDependencies(project: Project): Collection<String> {
            return listOf("expectedBy", "implement")
                .flatMap { project.configurations.findByName(it)?.dependencies ?: emptySet<Dependency>() }
                .filterIsInstance<ProjectDependency>()
                .mapNotNull { it.dependencyProject }
                .map { it.pathOrName() }
        }

        private fun Project.pathOrName() = if (path == ":") name else path

        private fun AbstractKotlinCompile<*>.createSourceSet(project: Project, projectType: KotlinProject.ProjectType): SourceSet? {
            val javaSourceSet = project
                .variantImplementationFactory<JavaSourceSetsAccessor.JavaSourceSetsAccessorVariantFactory>()
                .getInstance(project)
                .sourceSetsIfAvailable
                ?.find { it.name == sourceSetName.get() }
            @Suppress("DEPRECATION") val kotlinSourceSet: SourceDirectorySet? = javaSourceSet
                ?.getExtension(if (projectType == KotlinProject.ProjectType.PLATFORM_JS) KOTLIN_JS_DSL_NAME else KOTLIN_DSL_NAME)
            return if (kotlinSourceSet != null) {
                SourceSetImpl(
                    sourceSetName.get(),
                    if (sourceSetName.get().contains("test", true)) SourceSet.SourceSetType.TEST else SourceSet.SourceSetType.PRODUCTION,
                    friendSourceSets.get(),
                    kotlinSourceSet.srcDirs,
                    javaSourceSet.resources.srcDirs,
                    destinationDirectory.get().asFile,
                    javaSourceSet.output.resourcesDir!!,
                    buildCompilerArguments()
                )
            } else null
        }

        /**
         * Constructs the Android [SourceSet] that should be returned to the IDE for each compile task/variant.
         */
        private fun AbstractKotlinCompile<*>.createAndroidSourceSet(androidTarget: KotlinAndroidTarget): SourceSet {
            val variantName = sourceSetName.get()
            val compilation = androidTarget.compilations.getByName(variantName)
            // Merge all sources and resource dirs from the different Source Sets that make up this variant.
            val sources = compilation.allKotlinSourceSets.flatMap {
                it.kotlin.srcDirs
            }.distinctBy { it.absolutePath }
            val resources = compilation.allKotlinSourceSets.flatMap {
                it.resources.srcDirs
            }.distinctBy { it.absolutePath }
            return SourceSetImpl(
                sourceSetName.get(),
                if (sourceSetName.get().contains("test", true)) SourceSet.SourceSetType.TEST else SourceSet.SourceSetType.PRODUCTION,
                friendSourceSets.get(),
                sources,
                resources,
                destinationDirectory.get().asFile,
                compilation.output.resourcesDir,
                buildCompilerArguments()
            )
        }

        @Suppress("DEPRECATION_ERROR")
        private fun AbstractKotlinCompile<*>.buildCompilerArguments(): CompilerArguments {
            return CompilerArgumentsImpl(
                serializedCompilerArguments,
                defaultSerializedCompilerArguments,
                libraries.toList()
            )
        }
    }
}
