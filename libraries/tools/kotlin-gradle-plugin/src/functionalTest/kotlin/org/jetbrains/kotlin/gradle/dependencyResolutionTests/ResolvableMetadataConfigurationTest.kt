/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.dependencyResolutionTests

import org.gradle.api.Project
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinBinaryDependency
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.assertMatches
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.binaryCoordinates
import org.jetbrains.kotlin.gradle.plugin.ide.kotlinIdeMultiplatformImport
import org.jetbrains.kotlin.gradle.plugin.kotlinToolingVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.resolvableMetadataConfiguration
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.util.applyMultiplatformPlugin
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.enableDefaultStdlibDependency
import org.jetbrains.kotlin.gradle.util.kotlin
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class ResolvableMetadataConfigurationTest : SourceSetDependenciesResolution() {

    @Test
    fun `test - resolves consistent in project`() {
        val project = buildProject {
            configureRepositoriesForTests()
            applyMultiplatformPlugin()
        }

        val kotlin = project.multiplatformExtension

        /* Define simple targets */
        kotlin.jvm()
        kotlin.linuxArm64()
        kotlin.linuxX64()

        kotlin.applyDefaultHierarchyTemplate()

        val commonMain = kotlin.sourceSets.getByName("commonMain")
        val nativeMain = kotlin.sourceSets.getByName("nativeMain")

        commonMain.dependencies {
            implementation("com.squareup.okio:okio:3.2.0")
        }

        /* nativeMain explicitly using 3.3.0 (higher than 3.2.0 in commonMain) */
        nativeMain.dependencies {
            implementation("com.squareup.okio:okio:3.3.0")
            implementation("com.arkivanov.mvikotlin:mvikotlin:3.0.2")
        }

        project.evaluate()

        /* Check by resolving the 'resolvableMetadataConfigurations' directly */
        kotlin.sourceSets.forEach { sourceSet ->
            sourceSet.internal.resolvableMetadataConfiguration.incoming.resolutionResult.allDependencies
                .mapNotNull { result -> if (result is ResolvedDependencyResult) result.selected.id else null }
                .filterIsInstance<ModuleComponentIdentifier>()
                .filter { id -> id.group == "com.squareup.okio" }
                .ifEmpty { fail("Expected at least one okio dependency resolved") }
                .forEach { resolvedId ->
                    assertEquals(
                        "3.3.0", resolvedId.version,
                        "SourceSet: ${sourceSet.name} resolved $resolvedId, but expected consistent version 3.3.0"
                    )
                }
        }

        /* Check IDE resolution for commonMain */
        project.kotlinIdeMultiplatformImport.resolveDependencies("commonMain")
            .assertMatches(
                binaryCoordinates(Regex("com.squareup.okio:okio(-.*)?:.*:3.3.0")),
                binaryCoordinates("org.jetbrains.kotlin:kotlin-stdlib:commonMain:${project.kotlinToolingVersion}")
            )

        /* Check IDE resolution for nativeMain */
        project.kotlinIdeMultiplatformImport.resolveDependencies("nativeMain")
            .filterIsInstance<IdeaKotlinBinaryDependency>()
            .filter { it.coordinates?.group.orEmpty() in setOf("com.squareup.okio", "com.arkivanov.mvikotlin") }
            .assertMatches(
                binaryCoordinates(Regex("com.squareup.okio:okio(-.*)?:.*:3.3.0")),
                binaryCoordinates(Regex("com.arkivanov.mvikotlin:mvikotlin(-*)?:.*:3.0.2")),
            )
    }

    @Test
    fun commonMainWithHigherVersion() {
        assertSourceSetDependenciesResolution("commonMainWithHigherVersion.txt") { project ->
            project.defaultTargets()
            project.kotlin { linuxArm64() }

            // commonMain depends on `lib:2.0`, so during dependency resolution,
            // this version should win over jvmMain and linuxMain that depend on 1.0
            api("jvmMain", "lib", "1.0")
            api("linuxMain", "lib", "1.0")
            api("commonMain", "lib", "2.0")
            // for test source sets, it should work the same due to transitivity.
            // because test code depends on the main with all its transitive dependencies.
            // this is the general logic of `associatedWith` compilations.
            api("commonTest", "lib", "1.0")
        }
    }

    @Test
    @Ignore("TODO: KT-66375")
    fun jvmMainWithHigherVersion() {
        assertSourceSetDependenciesResolution("leafSourceSetWithHigherVersion.txt") { project ->
            project.defaultTargets()

            // jvmMain depends on 3.0.
            // commonMain code is included in jvmMain compilation, so it should see the same version as jvmMain (3.0 wins here).
            // commonMain code is included in jsMain compilation, so it should see the same version as commonMain (3.0 wins here).
            // linuxX64Main should receive 3.0 version from commonMain transitively
            api("jvmMain", "lib", "3.0")
            api("jsMain", "lib", "2.0")
            api("commonMain", "lib", "1.0")
        }
    }

    @Test
    @Ignore("TODO: KT-66375")
    fun nativeMainWithHigherVersion() {
        assertSourceSetDependenciesResolution("leafSourceSetWithHigherVersion.txt") { project ->
            project.defaultTargets()

            /** Same as for [jvmMainWithHigherVersion] but for linuxX64 */
            api("linuxX64Main", "lib", "3.0")
            api("jsMain", "lib", "2.0")
            api("commonMain", "lib", "1.0")
        }
    }

    @Test
    @Ignore("TODO: KT-66375")
    fun jsMainWithHigherVersion() {
        assertSourceSetDependenciesResolution("leafSourceSetWithHigherVersion.txt") { project ->
            project.defaultTargets()

            /** Same as for [jvmMainWithHigherVersion] but for js */
            api("jsMain", "lib", "3.0")
            api("nativeMain", "lib", "2.0")
            api("commonMain", "lib", "1.0")
        }
    }

    @Test
    fun commonTestShouldNotAffectMainSourceSets() {
        assertSourceSetDependenciesResolution("commonTestShouldNotAffectMainSourceSets.txt") { project ->
            project.defaultTargets()

            /** Test source sets are not compiled together with the main code, but just depend on it.
            Thus, by default, there is no need for the main code to receive the same dependency versions as tests.
            However, the other way around works in the opposite. See, for example, test [commonMainWithHigherVersion] */
            api("commonMain", "lib", "1.0")
            api("commonTest", "lib", "2.0")
        }
    }

    @Test
    @Ignore("TODO: KT-66375")
    fun leafSourceSetsDependsOnDifferentVersionsAndCommonCodeDoesNot() {
        assertSourceSetDependenciesResolution("leafSourceSetsDependsOnDifferentVersionsAndCommonCodeDoesNot.txt") { project ->
            project.defaultTargets()

            /** Even though commonMain has no dependency on lib, thus there is no connection between
             * jvmMain and linuxX64Main their dependencies should be still resolved consistently.
             * This is by design!
             * It complies with the desire of having global dependencies for the whole project */
            api("jvmMain", "lib", "1.0")
            api("linuxX64Main", "lib", "2.0")
        }
    }

    /**
     * after KT-66375 is fixed it is expected that all source sets will have foo:2.0 dependency
     * unless other is decided
     */
    @Test
    fun KT66375JvmDependenciesShouldNotDowngrade() {
        val appProject = buildProject(projectBuilder = { withName("app") })
        val libProject = buildProject(projectBuilder = { withName("lib").withParent(appProject) })

        appProject.enableDefaultStdlibDependency(false)
        libProject.enableDefaultStdlibDependency(false)

        assertSourceSetDependenciesResolution("KT66375JvmDependenciesShouldNotDowngrade.txt", withProject = appProject) {
            appProject.applyMultiplatformPlugin().apply {
                jvm(); linuxX64()
                // common main depends on 1.0
                sourceSets.getByName("commonMain").dependencies { this.api(mockedDependency("foo", "1.0")) }
                // jvm main depends on lib that transitively depends on 2.0
                sourceSets.jvmMain.dependencies { api(project(":lib")) }
            }

            libProject.applyMultiplatformPlugin().apply {
                jvm(); linuxX64()
                sourceSets.jvmMain.dependencies { api(mockedDependency("foo", "2.0")) }
            }
        }
    }

    /**
     * This test checks that `iosArm64` will successfully resolve `iosArm64`, even on
     * non-Mac hosts. If the test fails on non-Mac host, please DO NOT add @OsCondition
     * and investiagate
     */
    @Test
    fun leafHostSpecificSourceSetsDependencies() {
        val appProject = buildProject(projectBuilder = { withName("app") })
        val libProject = buildProject(projectBuilder = { withName("lib").withParent(appProject) })

        appProject.enableDefaultStdlibDependency(false)
        libProject.enableDefaultStdlibDependency(false)

        assertSourceSetDependenciesResolution("leafHostSpecificSourceSetsDependencies.txt", withProject = appProject) {
            appProject.applyMultiplatformPlugin().apply {
                jvm()
                iosArm64()
                sourceSets.getByName("commonMain").dependencies { api(project(":lib")) }
            }

            libProject.applyMultiplatformPlugin().apply {
                jvm()
                iosArm64()
            }
        }
    }

    private fun Project.defaultTargets() {
        kotlin { jvm(); linuxX64(); js(); applyDefaultHierarchyTemplate() }
    }
}
