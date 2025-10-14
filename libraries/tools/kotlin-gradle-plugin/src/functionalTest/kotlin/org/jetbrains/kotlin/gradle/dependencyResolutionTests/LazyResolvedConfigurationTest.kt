/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.dependencyResolutionTests

import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.internal.component.external.model.DefaultModuleComponentSelector
import org.gradle.kotlin.dsl.project
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.kotlinToolingVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.util.applyMultiplatformPlugin
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.enableDependencyVerification
import org.jetbrains.kotlin.gradle.utils.LazyResolvedConfigurationWithArtifacts
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class LazyResolvedConfigurationTest {

    @Test
    fun `test - creating LazyResolvedConfiguration - will not resolve source configuration`() {
        val project = buildProject()
        val configuration = project.configurations.create("forTest")
        LazyResolvedConfigurationWithArtifacts(configuration)

        assertEquals(
            Configuration.State.UNRESOLVED, configuration.state,
            "Expected construction of 'LazyResolvedConfiguration' to not cause resolution of source configuration"
        )
    }

    @Test
    fun `test - okio - getArtifacts`() {
        val project = buildProject {
            enableDependencyVerification(false)
            repositories.mavenLocal { repo ->
                repo.mavenContent { it.includeGroupByRegex(".*jetbrains.*") }
            }
            repositories.mavenCentral()
            applyMultiplatformPlugin()
        }

        val kotlin = project.multiplatformExtension
        kotlin.jvm()
        kotlin.linuxX64()

        kotlin.sourceSets.getByName("commonMain").dependencies {
            implementation("com.squareup.okio:okio:3.3.0")
        }

        project.evaluate()

        val commonMainCompileDependencies = kotlin.metadata().compilations.getByName("commonMain")
            .internal.configurations.compileDependencyConfiguration

        val lazyCommonMainCompileDependencies = LazyResolvedConfigurationWithArtifacts(commonMainCompileDependencies)

        assertEquals(
            commonMainCompileDependencies.incoming.resolutionResult.allDependencies,
            lazyCommonMainCompileDependencies.allDependencies
        )

        assertEquals(lazyCommonMainCompileDependencies.allDependencies, lazyCommonMainCompileDependencies.allResolvedDependencies)
        if (lazyCommonMainCompileDependencies.allResolvedDependencies.isEmpty()) fail("Expected some resolved dependencies")

        /* Check stdlib-common dependency on commonMainCompileDependencies */
        run {
            val resolvedStdlib = lazyCommonMainCompileDependencies.allResolvedDependencies.filter { dependencyResult ->
                dependencyResult.resolvedVariant.owner.let { id -> id is ModuleComponentIdentifier && id.module == "kotlin-stdlib" }
            }

            if (resolvedStdlib.isEmpty()) fail("Expected kotlin-stdlib in resolved dependencies")
            resolvedStdlib.forEach { dependencyResult ->
                val artifacts = lazyCommonMainCompileDependencies.getArtifacts(dependencyResult)
                if (artifacts.isEmpty()) fail("Expected some artifacts resolved for $dependencyResult")
                artifacts.forEach { artifact ->
                    assertEquals(artifact.file.name, "kotlin-stdlib-${project.kotlinToolingVersion}-all.jar")
                }
            }
        }

        /* Check okio dependency on commonMainCompileDependencies */
        run {
            val resolvedOkio = lazyCommonMainCompileDependencies.allResolvedDependencies.filter { dependencyResult ->
                dependencyResult.resolvedVariant.owner.let { id -> id is ModuleComponentIdentifier && id.module == "okio" }
            }

            if (resolvedOkio.isEmpty()) fail("Expected okio in resolved dependencies")
            resolvedOkio.forEach { dependencyResult ->
                val artifacts = lazyCommonMainCompileDependencies.getArtifacts(dependencyResult)
                if (artifacts.isEmpty()) fail("Expected some artifacts resolved for $dependencyResult")
                artifacts.forEach { artifact ->
                    assertEquals("okio-metadata-3.3.0-all.jar", artifact.file.name)
                }
            }
        }

        /* Check okio dependency on linuxX64MainCompile */
        run {
            val lazyLinuxX64CompileDependencies = LazyResolvedConfigurationWithArtifacts(
                kotlin.linuxX64().compilations.getByName("main").internal.configurations.compileDependencyConfiguration
            )

            val resolvedOkio = lazyLinuxX64CompileDependencies.allResolvedDependencies.filter { dependencyResult ->
                dependencyResult.resolvedVariant.owner.let { id -> id is ModuleComponentIdentifier && id.module == "okio" }
            }
            if (resolvedOkio.isEmpty()) fail("Expected okio in resolved dependencies")
            resolvedOkio.forEach { dependencyResult ->
                val artifacts = lazyLinuxX64CompileDependencies.getArtifacts(dependencyResult)
                if (artifacts.isEmpty()) fail("Expected some artifacts resolved for $dependencyResult")
                artifacts.forEach { artifact ->
                    val artifactComponentIdentifier = artifact.id.componentIdentifier as ModuleComponentIdentifier
                    assertEquals("okio-linuxx64", artifactComponentIdentifier.module, "Expected linux specific component identifier")
                }
            }
        }
    }

    @Test
    fun `test - unresolved dependency`() {
        val project = buildProject()
        val configuration = project.configurations.create("forTest")
        val unresolvedDependency = project.dependencies.create("unresolved:dependency")
        configuration.dependencies.add(unresolvedDependency)

        val lazyConfiguration = LazyResolvedConfigurationWithArtifacts(configuration)
        if (lazyConfiguration.files.toList().isNotEmpty()) fail("Expected no files to be resolved")
        if (lazyConfiguration.resolvedArtifacts.toList().isNotEmpty()) fail("Expected no artifacts to be resolved")
        if (lazyConfiguration.allResolvedDependencies.isNotEmpty()) fail("Expected no resolved dependencies")

        if (lazyConfiguration.resolutionFailures.size != 1) fail("Expected one resolution failure: ${lazyConfiguration.resolutionFailures}")
        val failure = lazyConfiguration.resolutionFailures.first()
        if ("unresolved:dependency" !in failure.message.orEmpty()) fail("Expected dependency mentioned in failure: ${failure.message}")

        if (lazyConfiguration.allDependencies.size != 1) fail("Expected one dependency: ${lazyConfiguration.allDependencies}")
        val resolvedDependencyResult = lazyConfiguration.allDependencies.first()
        val moduleIdentifier = (resolvedDependencyResult.requested as DefaultModuleComponentSelector).moduleIdentifier
        assertEquals("unresolved", moduleIdentifier.group)
        assertEquals("dependency", moduleIdentifier.name)

        if (lazyConfiguration.allResolvedDependencies.isNotEmpty()) fail("Expected no resolved dependencies")
    }

    @Test
    fun `test - circular dependency handling`() {
        val project = buildProject()
        val configuration = project.configurations.create("forTest")
        configuration.isCanBeConsumed = true
        // add dependency to itself
        project.dependencies.add(configuration.name, project.dependencies.project(":", configuration = configuration.name))
        project.artifacts.add(configuration.name, project.file("artifact.tmp"))

        val lazyConfiguration = LazyResolvedConfigurationWithArtifacts(configuration)

        val dependency = lazyConfiguration.allResolvedDependencies.singleOrNull() ?: fail("Expected to have single dependency")
        val id = dependency.resolvedVariant.owner
        if (id !is ProjectComponentIdentifier || id.projectPath != ":") fail("Expected project(:) dependency")

        val artifactName = lazyConfiguration
            .resolvedArtifacts
            .map { it.file.name }
            .singleOrNull()
            ?: fail("Expected to have single artifact")

        assertEquals("artifact.tmp", artifactName)
    }
}
