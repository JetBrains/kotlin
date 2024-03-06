/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dependencyResolutionTests

import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.kotlin.dsl.maven
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.platformTargets
import org.jetbrains.kotlin.gradle.plugin.mpp.resolvableMetadataConfiguration
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.util.*
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.Rule
import org.junit.rules.TemporaryFolder

abstract class SourceSetDependenciesResolution {
    @get:Rule
    val tempFolder = TemporaryFolder()

    class SourceSetDependenciesDsl(
        private val project: Project
    ) {
        val declaredDependencies = mutableSetOf<Pair<String, String>>() // list of name + versions

        /**
         * Declares an API dependency to test:[name]:[version] for [sourceSetName] source set
         */
        fun api(sourceSetName: String, name: String, version: String): Unit = project
            .kotlinExtension
            .sourceSets
            .getByName(sourceSetName)
            .dependencies { api(mockedDependency(name, version)) }

        fun mockedDependency(name: String, version: String): String {
            declaredDependencies.add(name to version)
            return "test:$name:$version"
        }
    }

    /**
     * If [withProject] is not null then dependencies of source sets in this projects will be verified against [expectedFilePath]
     *
     */
    fun assertSourceSetDependenciesResolution(
        expectedFilePath: String,
        withProject: ProjectInternal? = null,
        configure: SourceSetDependenciesDsl.(Project) -> Unit
    ) {
        val repoRoot = tempFolder.newFolder()
        val project = withProject ?: buildProject {
            applyMultiplatformPlugin()
        }

        project.allprojects {
            it.enableDefaultStdlibDependency(false)
            it.enableDependencyVerification(false)
            it.repositories.maven(repoRoot)
        }

        val dsl = SourceSetDependenciesDsl(project)
        dsl.configure(project)

        dsl.declaredDependencies.forEach { project.multiplatformExtension.publishAsMockedLibrary(repoRoot, it.first, it.second) }

        project.evaluate()

        val actualResult = project.resolveAllSourceSetDependencies()
        val expectedFile = resourcesRoot.resolve("dependenciesResolution").resolve(expectedFilePath)
        KotlinTestUtils.assertEqualsToFile(expectedFile, actualResult)
    }

    private fun Project.resolveAllSourceSetDependencies(): String {
        val allSourceSets = multiplatformExtension.sourceSets.toSet()

        val platformSpecificSourceSets = multiplatformExtension
            .targets
            .platformTargets
            .flatMap { it.compilations }
            .associate { it.defaultSourceSet to configurations.getByName(it.compileDependencyConfigurationName) }

        val commonSourceSetsWithoutMetadataCompilation = allSourceSets
            .minus(platformSpecificSourceSets.keys)
            .associateWith { it.internal.resolvableMetadataConfiguration }

        val actual = (platformSpecificSourceSets + commonSourceSetsWithoutMetadataCompilation).mapValues { (_, configuration) ->
            val resolutionResult = configuration.incoming.resolutionResult
            val rootComponent = resolutionResult.root
            val dependencies = resolutionResult.allComponents
                .minus(rootComponent)
                .map { it.id.displayName }
                .sorted()
            dependencies.joinToString("\n") { "  $it" }
        }.entries.sortedBy { (sourceSet, _) -> sourceSet.name }.joinToString("\n") {
            it.key.name + "\n" + it.value
        }

        return actual
    }
}