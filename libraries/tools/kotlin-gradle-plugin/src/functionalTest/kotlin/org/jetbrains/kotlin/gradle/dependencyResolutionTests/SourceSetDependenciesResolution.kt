/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dependencyResolutionTests

import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.dsl.platformTargets
import org.jetbrains.kotlin.gradle.plugin.mpp.resolvableMetadataConfiguration
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.util.*
import org.jetbrains.kotlin.gradle.utils.targets
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.Rule
import org.junit.rules.TemporaryFolder

abstract class SourceSetDependenciesResolution {
    @get:Rule
    val tempFolder = TemporaryFolder()

    class SourceSetDependenciesDsl(
        private val project: Project
    ) {
        val mavenRepositoryMock = MavenRepositoryMock()

        fun mavenRepositoryMock(code: MavenRepositoryMockDsl.() -> Unit) =
            MavenRepositoryMockDsl(mavenRepositoryMock).run(code)

        fun api(sourceSetName: String, name: String, version: String): Unit =
            api(sourceSetName, "test:$name:$version")

        fun mockedDependency(name: String, version: String): String {
            return mavenRepositoryMock.module("test", name, version).asMavenNotation
        }

        fun api(sourceSetName: String, dependencyNotation: String): Unit = project
            .kotlinExtension
            .sourceSets
            .getByName(sourceSetName)
            .dependencies { api(mockedDependency(dependencyNotation)) }

        fun mockedDependency(dependencyNotation: String): String {
            mavenRepositoryMock.module(dependencyNotation)
            return dependencyNotation
        }

    }

    /**
     * If [withProject] is not null then dependencies of source sets in this projects will be verified against [expectedFilePath]
     *
     */
    fun assertSourceSetDependenciesResolution(
        expectedFilePath: String,
        withProject: ProjectInternal? = null,
        sanitize: String.() -> String = { this },
        configure: SourceSetDependenciesDsl.(Project) -> Unit
    ) {
        val repoRoot = tempFolder.newFolder()
        val project = withProject ?: buildProject {
            // Disable stdlib and kotlin-dom-api for default tests, as they just pollute dependencies dumps
            enableDefaultStdlibDependency(false)
            enableDefaultJsDomApiDependency(false)

            configureRepositoriesForTests()
            applyMultiplatformPlugin()
        }

        val dsl = SourceSetDependenciesDsl(project)
        dsl.configure(project)

        dsl.mavenRepositoryMock.applyToProject(project, repoRoot)

        project.evaluate()

        val actualResult = project.resolveAllSourceSetDependencies().sanitize()
        val expectedFile = resourcesRoot
            .resolve("dependenciesResolution")
            .resolve(this.javaClass.simpleName)
            .resolve(expectedFilePath)
        KotlinTestUtils.assertEqualsToFile(expectedFile, actualResult)
    }

    private fun Project.resolveAllSourceSetDependencies(): String {
        val allSourceSets = kotlinExtension.sourceSets.toSet()

        val platformSpecificSourceSets = kotlinExtension
            .targets
            .platformTargets
            .flatMap { it.compilations }
            .associate { it.defaultSourceSet to configurations.getByName(it.compileDependencyConfigurationName) }

        val commonSourceSetsWithoutMetadataCompilation = allSourceSets
            .minus(platformSpecificSourceSets.keys)
            .associateWith { it.internal.resolvableMetadataConfiguration }

        val actual = (platformSpecificSourceSets + commonSourceSetsWithoutMetadataCompilation).mapValues { (_, configuration) ->
            configuration.dumpResolvedDependencies().prependIndent("  ")
        }.entries.sortedBy { (sourceSet, _) -> sourceSet.name }.joinToString("\n") {
            it.key.name + "\n" + it.value
        }

        return actual
    }
}