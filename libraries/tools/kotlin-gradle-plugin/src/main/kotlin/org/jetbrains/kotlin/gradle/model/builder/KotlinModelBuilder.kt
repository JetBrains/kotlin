/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.model.builder

import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.tooling.provider.model.ToolingModelBuilder
import org.jetbrains.kotlin.gradle.model.CompilerArguments
import org.jetbrains.kotlin.gradle.model.ExperimentalFeatures
import org.jetbrains.kotlin.gradle.model.KotlinProject
import org.jetbrains.kotlin.gradle.model.SourceSet
import org.jetbrains.kotlin.gradle.model.impl.CompilerArgumentsImpl
import org.jetbrains.kotlin.gradle.model.impl.ExperimentalFeaturesImpl
import org.jetbrains.kotlin.gradle.model.impl.KotlinProjectImpl
import org.jetbrains.kotlin.gradle.model.impl.SourceSetImpl
import org.jetbrains.kotlin.gradle.plugin.KOTLIN_DSL_NAME
import org.jetbrains.kotlin.gradle.plugin.KOTLIN_JS_DSL_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.getConvention
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile

/**
 * [ToolingModelBuilder] for [KotlinProject] models.
 * This model builder is registered for base Kotlin JVM, Kotlin JS and Kotlin Common plugins.
 */
class KotlinModelBuilder(private val kotlinPluginVersion: String) : ToolingModelBuilder {

    override fun canBuild(modelName: String): Boolean {
        return modelName == KotlinProject::class.java.name
    }

    override fun buildAll(modelName: String, project: Project): Any? {
        if (modelName == KotlinProject::class.java.name) {
            val kotlinCompileTasks = project.tasks.withType(AbstractKotlinCompile::class.java)
            val projectType = getProjectType(project)
            return KotlinProjectImpl(
                project.name,
                kotlinPluginVersion,
                projectType,
                kotlinCompileTasks.mapNotNull { it.createSourceSet(project, projectType) },
                getExpectedByDependencies(project),
                kotlinCompileTasks.first()!!.createExperimentalFeatures()
            )
        }
        return null
    }

    companion object {

        private fun getProjectType(project: Project): KotlinProject.ProjectType {
            return if (project.plugins.hasPlugin("kotlin") || project.plugins.hasPlugin("kotlin-platform-jvm")) {
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
            val javaSourceSet =
                project.convention.findPlugin(JavaPluginConvention::class.java)?.sourceSets?.find { it.name == sourceSetName }
            val kotlinSourceSet =
                javaSourceSet?.getConvention(if (projectType == KotlinProject.ProjectType.PLATFORM_JS) KOTLIN_JS_DSL_NAME else KOTLIN_DSL_NAME) as? KotlinSourceSet
            return if (kotlinSourceSet != null) {
                SourceSetImpl(
                    sourceSetName,
                    if (sourceSetName.contains("test", true)) SourceSet.SourceSetType.TEST else SourceSet.SourceSetType.PRODUCTION,
                    findFriendSourceSets(),
                    kotlinSourceSet.kotlin.srcDirs,
                    javaSourceSet.resources.srcDirs,
                    destinationDir,
                    javaSourceSet.output.resourcesDir,
                    createCompilerArguments()
                )
            } else null
        }

        private fun AbstractKotlinCompile<*>.findFriendSourceSets(): Collection<String> {
            val friendSourceSets = ArrayList<String>()
            friendTask?.sourceSetName?.let { friendSourceSets.add(it) }
            return friendSourceSets
        }

        private fun AbstractKotlinCompile<*>.createCompilerArguments(): CompilerArguments {
            return CompilerArgumentsImpl(
                serializedCompilerArguments,
                defaultSerializedCompilerArguments,
                compileClasspath.toList()
            )
        }

        private fun AbstractKotlinCompile<*>.createExperimentalFeatures(): ExperimentalFeatures {
            return ExperimentalFeaturesImpl(coroutinesStr)
        }
    }
}