/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.api.Task
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinNativeSettingsDsl
import org.jetbrains.kotlin.gradle.ecosystem.KotlinEcosystemExtension
import org.jetbrains.kotlin.gradle.targets.native.internal.CInteropCommonizerTask
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink
import org.jetbrains.kotlin.gradle.testbase.GradleTest
import org.jetbrains.kotlin.gradle.testbase.KGPBaseTest
import org.jetbrains.kotlin.gradle.testbase.NativeGradlePluginTests
import org.jetbrains.kotlin.gradle.testbase.TestProject
import org.jetbrains.kotlin.gradle.testbase.addEcosystemPluginToBuildScriptCompilationClasspath
import org.jetbrains.kotlin.gradle.testbase.assertOutputContains
import org.jetbrains.kotlin.gradle.testbase.assertOutputDoesNotContain
import org.jetbrains.kotlin.gradle.testbase.buildScriptReturn
import org.jetbrains.kotlin.gradle.testbase.project
import org.jetbrains.kotlin.gradle.testbase.settingsBuildScriptInjection
import org.jetbrains.kotlin.internal.compilerRunner.native.parallelismLimitServiceName
import org.junit.jupiter.api.DisplayName
import kotlin.test.assertEquals

@DisplayName("Kotlin/Native max workers configured in the settings plugin")
@NativeGradlePluginTests
@OptIn(ExperimentalKotlinGradlePluginApi::class)
class KotlinNativeSettingsDslIT : KGPBaseTest() {

    @DisplayName("Max workers come from the settings DSL, then the Gradle property, and stay unset when neither is set")
    @GradleTest
    fun testMaxWorkersResolveFromSettingsDslThenGradleProperty(gradleVersion: GradleVersion) {
        project("empty", gradleVersion) {
            applySettingsDsl {
                compileMaxWorkers.set(4)
            }

            assertEquals(
                listOf(4, 3, null),
                registeredMaxParallelUsages("-Pkotlin.native.compileMaxWorkers=2", "-Pkotlin.native.linkMaxWorkers=3"),
            )
        }
    }

    private fun TestProject.applySettingsDsl(configure: KotlinNativeSettingsDsl.() -> Unit) {
        addEcosystemPluginToBuildScriptCompilationClasspath()
        settingsBuildScriptInjection {
            settings.plugins.apply("org.jetbrains.kotlin.ecosystem")
            settings.extensions.getByType(KotlinEcosystemExtension::class.java).native(configure)
        }
    }

    private fun TestProject.registeredMaxParallelUsages(vararg gradleProperties: String): List<Int?> =
        buildScriptReturn {
            fun limit(taskType: Class<out Task>): Int? = project.gradle.sharedServices.registrations
                .findByName(parallelismLimitServiceName(taskType))
                ?.maxParallelUsages?.orNull
            listOf(
                limit(KotlinNativeCompile::class.java),
                limit(KotlinNativeLink::class.java),
                limit(CInteropCommonizerTask::class.java),
            )
        }.buildAndReturn("tasks", *gradleProperties) {
            assertOutputContains("Kotlin/Native settings DSL value 4 overrides Gradle property kotlin.native.compileMaxWorkers=2")
            assertOutputDoesNotContain("overrides Gradle property kotlin.native.linkMaxWorkers")
        }
}
