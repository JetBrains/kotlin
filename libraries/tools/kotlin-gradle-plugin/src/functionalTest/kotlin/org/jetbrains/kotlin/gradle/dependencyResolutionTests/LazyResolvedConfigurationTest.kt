/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dependencyResolutionTests

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.DependencyResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.internal.component.external.model.DefaultModuleComponentSelector
import org.gradle.kotlin.dsl.project
import org.jetbrains.kotlin.gradle.cache.kotlinGradleTaskExecutionCache
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.internal.BuildIdentifierAccessor
import org.jetbrains.kotlin.gradle.plugin.kotlinToolingVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.plugin.variantImplementationFactoryProvider
import org.jetbrains.kotlin.gradle.testing.prettyPrinted
import org.jetbrains.kotlin.gradle.util.applyMultiplatformPlugin
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.enableDependencyVerification
import org.jetbrains.kotlin.gradle.utils.LazyResolvedConfigurationComponent
import org.jetbrains.kotlin.gradle.utils.LazyResolvedConfigurationWithArtifacts
import org.jetbrains.kotlin.gradle.utils.createConsumable
import org.jetbrains.kotlin.gradle.utils.createResolvable
import org.jetbrains.kotlin.gradle.utils.groupByNotNullToSet
import org.jetbrains.kotlin.gradle.utils.resolvedDependenciesByKmpModuleId
import org.jetbrains.kotlin.gradle.utils.resolvedDependenciesByRequested
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
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
            repositories.kotlinBuildDeps()
            repositories.mavenCentralCacheRedirector()
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
        val resolvableConfiguration = project.configurations.createResolvable("forTest_resolvable")
        val consumableConfiguration = project.configurations.createConsumable("forTest_consumable")

        // add dependency from the resolvable configuration to the consumable configuration
        project.dependencies.add(
            resolvableConfiguration.name,
            project.dependencies.project(":", configuration = consumableConfiguration.name)
        )

        // add dependency from the consumable co to the resolvable configuration
        resolvableConfiguration.extendsFrom(consumableConfiguration)

        // add artifact onto the consumable configuration
        project.artifacts.add(consumableConfiguration.name, project.file("artifact.tmp"))

        val lazyConfiguration = LazyResolvedConfigurationWithArtifacts(resolvableConfiguration)

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

    @Test
    fun `test - cachingGroupByToNonNullSet`() {
        val project = buildProject {
            enableDependencyVerification(false)
            repositories.mavenCentralCacheRedirector()
            applyMultiplatformPlugin()
        }

        val kotlin = project.multiplatformExtension
        kotlin.jvm()
        kotlin.linuxX64()

        kotlin.sourceSets.getByName("commonMain").dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
        }

        project.evaluate()

        val commonMainCompileDependencies = kotlin.metadata().compilations.getByName("commonMain")
            .internal.configurations.compileDependencyConfiguration

        val lazyCommonMainCompileDependencies = LazyResolvedConfigurationComponent(commonMainCompileDependencies)
        val coordinatesMapper = { dependency: DependencyResult ->
            if (dependency is ResolvedDependencyResult) {
                val id = dependency.selected.id as ModuleComponentIdentifier
                "${id.group}:${id.module}"
            } else null
        }
        val variantNameMapper = { dependency: DependencyResult ->
            if (dependency is ResolvedDependencyResult) {
                dependency.resolvedVariant.displayName
            } else null
        }

        val cache = project.kotlinGradleTaskExecutionCache.get()

        val group = cache.getOrCompute("group") {
            lazyCommonMainCompileDependencies.groupByNotNullToSet(coordinatesMapper, variantNameMapper)
        }

        // getting group with the same name but different selectors should return the previously computed group
        val groupV2 = cache.getOrCompute("group") {
            lazyCommonMainCompileDependencies.groupByNotNullToSet({ "" }, { "" })
        }
        assertSame(
            group,
            groupV2
        )

        assertEquals(
            mapOf(
                "org.jetbrains.kotlin:kotlin-stdlib-common" to setOf(
                    "stdlibCommonElements",
                ),
                "org.jetbrains.kotlinx:atomicfu" to setOf(
                    "metadataApiElements",
                ),
                "org.jetbrains.kotlinx:kotlinx-coroutines-core" to setOf(
                    "metadataApiElements",
                ),
            ).prettyPrinted,
            group.prettyPrinted
        )

        val reversedGroup = cache.getOrCompute("reversedGroup") {
            lazyCommonMainCompileDependencies.groupByNotNullToSet(
                keySelector = variantNameMapper,
                valueTransform = coordinatesMapper
            )
        }

        assertEquals(
            mapOf(
                "metadataApiElements" to setOf(
                    "org.jetbrains.kotlinx:atomicfu",
                    "org.jetbrains.kotlinx:kotlinx-coroutines-core",
                ),
                "stdlibCommonElements" to setOf(
                    "org.jetbrains.kotlin:kotlin-stdlib-common",
                ),
            ).prettyPrinted,
            reversedGroup.prettyPrinted
        )
    }

    @Test
    fun `test - resolvedDependenciesByKmpModuleId`() {
        val project = projectWithCoroutinesInCommonMain()

        val resolvedDependencies = project.commonMainCompileDependencies().resolvedDependenciesByKmpModuleId(
            cache = project.kotlinGradleTaskExecutionCache.get(),
            projectId = project.path,
            buildIdentifierAccessor = project.variantImplementationFactoryProvider<BuildIdentifierAccessor.Factory>(),
        )

        assertEquals(
            mapOf(
                "KmpModuleIdentifier(moduleVersion=org.jetbrains.kotlin:kotlin-stdlib-common, module org.jetbrains.kotlin:kotlin-stdlib-common)" to setOf(
                    "org.jetbrains.kotlin:kotlin-stdlib-common:1.9.21",
                ),
                "KmpModuleIdentifier(moduleVersion=org.jetbrains.kotlinx:atomicfu, module org.jetbrains.kotlinx:atomicfu)" to setOf(
                    "org.jetbrains.kotlinx:atomicfu:0.23.1",
                ),
                "KmpModuleIdentifier(moduleVersion=org.jetbrains.kotlinx:kotlinx-coroutines-core, module org.jetbrains.kotlinx:kotlinx-coroutines-core)" to setOf(
                    "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0",
                ),
            ).prettyPrinted,
            resolvedDependencies.entries.associate { (kmpModuleId, dependencies) ->
                kmpModuleId.toString() to dependencies.map { it.toString() }.toSet()
            }.prettyPrinted
        )
    }

    @Test
    fun `test - resolvedDependenciesByRequested`() {
        val project = projectWithCoroutinesInCommonMain()

        val resolvedDependencies = project.commonMainCompileDependencies().resolvedDependenciesByRequested(
            cache = project.kotlinGradleTaskExecutionCache.get(),
            projectId = project.path,
        )

        assertEquals(
            mapOf(
                "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0" to setOf(
                    "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0",
                ),
                "org.jetbrains.kotlinx:atomicfu:0.23.1" to setOf(
                    "org.jetbrains.kotlinx:atomicfu:0.23.1",
                ),
                "org.jetbrains.kotlin:kotlin-stdlib-common:1.9.21" to setOf(
                    "org.jetbrains.kotlin:kotlin-stdlib-common:1.9.21",
                ),
            ).prettyPrinted,
            resolvedDependencies.entries.associate { (requested, dependencies) ->
                requested.toString() to dependencies.map { it.toString() }.toSet()
            }.prettyPrinted
        )
    }

    private fun projectWithCoroutinesInCommonMain(): ProjectInternal {
        val project = buildProject {
            enableDependencyVerification(false)
            repositories.mavenCentralCacheRedirector()
            applyMultiplatformPlugin()
        }

        val kotlin = project.multiplatformExtension
        kotlin.jvm()
        kotlin.linuxX64()

        kotlin.sourceSets.getByName("commonMain").dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
        }

        project.evaluate()
        return project
    }

    private fun Project.commonMainCompileDependencies() = LazyResolvedConfigurationComponent(
        multiplatformExtension
            .metadata()
            .compilations
            .getByName("commonMain")
            .internal.configurations.compileDependencyConfiguration
    )
}
