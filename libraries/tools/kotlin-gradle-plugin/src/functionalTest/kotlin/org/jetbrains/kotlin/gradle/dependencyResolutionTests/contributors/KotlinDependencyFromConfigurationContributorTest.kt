/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.dependencyResolutionTests.contributors

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dependencies.KotlinDependencyFromConfigurationContributor
import org.jetbrains.kotlin.gradle.dependencyResolutionTests.SourceSetDependenciesResolution
import org.jetbrains.kotlin.gradle.util.applyMultiplatformPlugin
import org.jetbrains.kotlin.gradle.util.configureDefaults
import org.jetbrains.kotlin.gradle.util.kotlin
import org.jetbrains.kotlin.gradle.util.setMultiplatformAndroidSourceSetLayoutVersion
import org.jetbrains.kotlin.gradle.utils.androidExtension
import kotlin.test.Test

@OptIn(ExperimentalWasmDsl::class)
internal class KotlinDependencyFromConfigurationContributorTest : SourceSetDependenciesResolution() {
    private val contributor = KotlinDependencyFromConfigurationContributor

    private fun Project.defaultTargets() {
        setMultiplatformAndroidSourceSetLayoutVersion(2)
        applyMultiplatformPlugin()
        plugins.apply("com.android.library")
        androidExtension.configureDefaults()

        kotlin {
            jvm()
            linuxX64()
            js()
            wasmJs()
            wasmWasi()
            androidTarget {
                publishLibraryVariants("release")
            }

            applyDefaultHierarchyTemplate {
                common {
                    group("jvmAndAndroid") {
                        withAndroidTarget()
                        withJvm()
                    }
                }
            }
        }
    }

    @Test
    fun `resolves dependencies for jvm target`() {
        val expected = mutableMapOf(
            "x" to "x"
        )
        testKotlinSourceSetDependenciesContributor(contributor, expected) { project ->
            project.defaultTargets()

//            api("commonMain", "test:lib-commonMain:1.0")
//            api("commonTest", "test:lib-commonTest:1.0")
//            api("jvmAndAndroidMain", "test:lib-jvmAndAndroidMain:1.0")
//            api("jvmAndAndroidTest", "test:lib-jvmAndAndroidTest:1.0")
            api("jvmMain", "test:lib:1.0")
//            api("jvmTest", "test:lib-test:1.0")
        }
    }
}