/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.regressionTests

import org.jetbrains.kotlin.gradle.dependencyResolutionTests.mavenCentralCacheRedirector
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformSourceSetConventionsImpl.commonMain
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformSourceSetConventionsImpl.dependencies
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.configurationResult
import org.jetbrains.kotlin.gradle.targets.native.internal.createCommonizedCInteropDependencyConfigurationView
import org.jetbrains.kotlin.gradle.util.*
import kotlin.test.Test
import kotlin.test.fail

class KT61376CInteropCommonizerConfigurationsTest {
    @Test
    fun `test - cinterop commonizer configurations - do not match any sqldelight variants`() {
        val project = buildProject {
            enableDependencyVerification(false)
            repositories.mavenCentralCacheRedirector()
            applyMultiplatformPlugin()
        }

        project.runLifecycleAwareTest {
            val kotlin = project.multiplatformExtension
            kotlin.linuxX64()
            kotlin.linuxArm64()

            kotlin.sourceSets.commonMain.dependencies {
                implementation("com.squareup.sqldelight:coroutines-extensions:1.5.5")
            }

            configurationResult.await()

            val resolvedFiles = project.createCommonizedCInteropDependencyConfigurationView(kotlin.sourceSets.commonMain.get()).files
            if (resolvedFiles.isNotEmpty()) {
                fail("Expected no files resolved. Found $resolvedFiles")
            }
        }
    }
}