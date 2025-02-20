/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.uklibs

import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.jetbrains.kotlin.gradle.artifacts.publishedMetadataCompilations
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.awaitMetadataTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.plugin.mpp.resolvableMetadataConfiguration
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.diagnostics.UklibFromKGPSourceSetsDependenciesChecker
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.publication.uklibPublishedPlatformCompilations
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.androidLibrary
import org.jetbrains.kotlin.gradle.util.kotlin
import org.jetbrains.kotlin.gradle.util.runLifecycleAwareTest
import kotlin.test.Test
import kotlin.test.assertEquals

class UklibDependencyDeclarationViolations {

    @Test
    fun `dependency specification validation - is not triggered when there are no metadata compilations`() {
        runTest(
            { emptySet() }
        ) {
            iosArm64()

            sourceSets.commonMain.dependencies {
                implementation("a:b:1.0")
            }
            sourceSets.iosArm64Main.dependencies {
                implementation("c:d:1.0")
            }
        }
    }

    @Test
    fun `dependency specification validation - with shared and differing dependencies across shared and platform source sets`() {
        runTest(
            {
                setOf(
                    violation(
                        iosArm64().compilations.getByName("main").internal.configurations.compileDependencyConfiguration,
                        setOf(
                            project.dependencies.create("a:different_version:2.0"),
                            project.dependencies.create("a:platform:1.0"),
                        ),
                    )
                )
            }
        ) {
            iosArm64()
            iosX64()

            sourceSets.commonMain.dependencies {
                // This dependency is propagated to all compilations
                implementation("a:common:1.0")
                // This dependency is shared across all compilations
                implementation("a:shared:1.0")
                // This dependency is specified again with a differing version
                implementation("a:different_version:1.0")
            }
            sourceSets.iosArm64Main.dependencies {
                implementation("a:shared:1.0")
                implementation("a:different_version:2.0")
                // This dependency is only specified in the platform compilation
                implementation("a:platform:1.0")
            }
        }
    }

    @Test
    fun `dependency specification validation - warns about missing dependency due to potentially filtered shared sources sets`() {
        runTest(
            {
                setOf(
                    violation(
                        iosArm64().compilations.getByName("main").internal.configurations.compileDependencyConfiguration,
                        setOf(
                            project.dependencies.create("a:ios:1.0"),
                        ),
                    ),
                    violation(
                        iosX64().compilations.getByName("main").internal.configurations.compileDependencyConfiguration,
                        setOf(
                            project.dependencies.create("a:ios:1.0"),
                        ),
                    ),
                    violation(
                        sourceSets.appleMain.get().internal.resolvableMetadataConfiguration,
                        setOf(
                            project.dependencies.create("a:ios:1.0"),
                        ),
                    ),
                    violation(
                        sourceSets.iosMain.get().internal.resolvableMetadataConfiguration,
                        setOf(
                            project.dependencies.create("a:ios:1.0"),
                        ),
                    ),
                )
            }
        ) {
            iosArm64()
            iosX64()

            // The dependency is missing in potentially published fragments nativeMain and commonMain
            sourceSets.appleMain.dependencies {
                implementation("a:ios:1.0")
            }
        }
    }

    @Test
    fun `dependency specification validation - when platform hierarchy is missing metadata compilations`() {
        runTest(
            {
                setOf(
                    violation(
                        iosArm64().compilations.getByName("main").internal.configurations.compileDependencyConfiguration,
                        setOf(
                            project.dependencies.create("a:apple:1.0"),
                        ),
                    ),
                    violation(
                        linuxArm64().compilations.getByName("main").internal.configurations.compileDependencyConfiguration,
                        setOf(
                            project.dependencies.create("a:linux:1.0"),
                        ),
                    )
                )
            }
        ) {
            iosArm64()
            linuxArm64()

            sourceSets.commonMain.dependencies {
                implementation("a:common:1.0")
            }
            sourceSets.appleMain.dependencies {
                implementation("a:apple:1.0")
            }
            sourceSets.linuxMain.dependencies {
                implementation("a:linux:1.0")
            }
        }
    }

    @Test
    fun `dependency specification validation - don't emit for dependencies added by default`() {
        runTest(
            {
                emptySet()
            }
        ) {
            project.androidLibrary { compileSdk = 31 }

            jvm()
            iosArm64()
            iosX64()
            js()
            wasmJs()
            wasmWasi()
            mingwX64()
            androidTarget()

            sourceSets.commonMain.dependencies {
                implementation("a:common:1.0")
            }
        }
    }

    @Test
    fun `dependency specification validation - different compilation scopes - don't trigger violation`() {
        runTest(
            { emptySet() }
        ) {
            jvm()
            js()

            // Scopes don't matter because we only care about that all compilations see the same set of dependencies
            sourceSets.commonMain.dependencies {
                implementation("a:common:1.0")
            }
            sourceSets.jvmMain.dependencies {
                compileOnly("a:common:1.0")
            }
            sourceSets.jsMain.dependencies {
                api("a:common:1.0")
            }
        }
    }

    @Test
    fun `dependency specification validation - runtime scope - isn't validated`() {
        runTest(
            { emptySet() }
        ) {
            jvm()
            js()

            // We probably don't care about runtime dependencies, so those can be whatever
            sourceSets.commonMain.dependencies {
                implementation("a:common:1.0")
            }
            sourceSets.jvmMain.dependencies {
                runtimeOnly("a:runtime_only:1.0")
            }
        }
    }

    @Test
    fun `dependency specification validation - test compilation dependencies - are not validated`() {
        runTest(
            { emptySet() }
        ) {
            jvm()
            js()

            // We don't validate test compilations at all
            sourceSets.commonMain.dependencies {
                implementation("a:common:1.0")
            }
            sourceSets.commonTest.dependencies {
                implementation("a:common_test:1.0")
            }
            sourceSets.jvmTest.dependencies {
                implementation("a:jvm_test:1.0")
            }
        }
    }

    @Test
    fun `dependency specification validation - npm dependencies`() {
        runTest(
            { emptySet() }
        ) {
            jvm()
            js()

            sourceSets.jsMain.dependencies {
                implementation(npm("mocha", "*"))
            }
        }
    }

    @Test
    fun `dependency specification validation - stdlib and dom dependencies - are not validated`() {
        runTest(
            { emptySet() }
        ) {
            jvm()
            js()

            sourceSets.jsMain.dependencies {
                implementation("")
            }
        }
    }


    private fun violation(
        configuration: Configuration,
        uniqueDependencies: Set<Dependency>,
    ): UklibFromKGPSourceSetsDependenciesChecker.DependencyDeclarationViolation = UklibFromKGPSourceSetsDependenciesChecker.DependencyDeclarationViolation(
        configuration,
        uniqueDependencies
    )

    private fun runTest(
        expectedViolations: KotlinMultiplatformExtension.() -> Set<UklibFromKGPSourceSetsDependenciesChecker.DependencyDeclarationViolation>,
        configure: KotlinMultiplatformExtension.() -> Unit,
    ) {
        buildProjectWithMPP {
            kotlin {
                configure()

                runLifecycleAwareTest {
                    assertEquals(
                        expectedViolations(),
                        UklibFromKGPSourceSetsDependenciesChecker.findInconsistentDependencyDeclarations(
                            uklibPublishedPlatformCompilations(),
                            awaitMetadataTarget().publishedMetadataCompilations(),
                        )
                    )
                }
            }
        }
    }
}