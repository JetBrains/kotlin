/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.dependencyResolutionTests

import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.artifacts.Configuration
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformSourceSetConventionsImpl.dependencies
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.util.*
import org.jetbrains.kotlin.gradle.util.assertNoDiagnostics
import kotlin.test.Test

class KotlinTestFrameworkInferenceFailedTest {

    @Test
    fun `KMP project - when jvmTest depends on kotlin-test - emits no diagnostics`() {
        evaluateProjectAndResolveConfigurationsWhen {
            kmpProjectWithJvm { sourceSets.jvmTest.dependOnKotlinTest() }
        }.assertNoDiagnostics()
    }

    @Test
    fun `KMP project - when commonTest depends on kotlin-test - emits no diagnostics`() {
        evaluateProjectAndResolveConfigurationsWhen {
            kmpProjectWithJvm { sourceSets.commonTest.dependOnKotlinTest() }
        }.assertNoDiagnostics()
    }

    @Test
    fun `KMP project - when jvmMain depends on kotlin-test - emits a diagnostic`() {
        evaluateProjectAndResolveConfigurationsWhen {
            kmpProjectWithJvm { sourceSets.jvmMain.dependOnKotlinTest() }
        }.checkDiagnostics("kotlin-test-kmp-jvmMain")
    }

    @Test
    fun `KMP project - when commonMain depends on kotlin-test - emits a diagnostic`() {
        evaluateProjectAndResolveConfigurationsWhen {
            kmpProjectWithJvm { sourceSets.commonMain.dependOnKotlinTest() }
        }.checkDiagnostics("kotlin-test-kmp-commonMain")
    }

    @Test
    fun `KMP project without jvm - when commonMain depends on kotlin-test - emits no diagnostics`() {
        evaluateProjectAndResolveConfigurationsWhen {
            buildProjectWithMPP {
                kotlin {
                    linuxArm64()
                    linuxX64()

                    sourceSets.commonMain.dependOnKotlinTest()
                }
            }
        }.assertNoDiagnostics()
    }

    @Test
    fun `JVM project - when testImplementation depends on kotlin-test - emits no diagnostics`() {
        evaluateProjectAndResolveConfigurationsWhen {
            buildProjectWithJvm {
                dependencies {
                    "testImplementation"("org.jetbrains.kotlin:kotlin-test")
                }
            }
        }.assertNoDiagnostics()
    }

    @Test
    fun `JVM project - when implementation depends on kotlin-test - emits a diagnostic`() {
        evaluateProjectAndResolveConfigurationsWhen {
            buildProjectWithJvm {
                dependencies {
                    "implementation"("org.jetbrains.kotlin:kotlin-test")
                }
            }
        }.checkDiagnostics("kotlin-test-jvm-implementation")
    }

    private fun kmpProjectWithJvm(configure: KotlinMultiplatformExtension.() -> Unit): ProjectInternal {
        return buildProjectWithMPP {
            kotlin {
                jvm()
                linuxArm64()

                configure(this)
            }
        }
    }

    private fun NamedDomainObjectProvider<KotlinSourceSet>.dependOnKotlinTest() {
        dependencies {
            implementation(kotlin("test"))
        }
    }

    private fun evaluateProjectAndResolveConfigurationsWhen(
        projectFactory: () -> ProjectInternal
    ): ProjectInternal {
        val project = projectFactory()
        project.repositories.mavenLocal()
        project.repositories.mavenCentralCacheRedirector()
        project.evaluate()
        project.configurations.filter(Configuration::isCanBeResolved).forEach(Configuration::resolve)
        return project
    }

}