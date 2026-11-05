/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinNativeSettingsDsl
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_NATIVE_COMPILE_MAX_WORKERS
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_NATIVE_LINK_MAX_WORKERS
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.isParallelismLimitServiceRegistered
import org.jetbrains.kotlin.gradle.util.maxParallelUsagesOf
import org.jetbrains.kotlin.gradle.util.propertiesExtension
import org.jetbrains.kotlin.internal.compilerRunner.native.KotlinNativeSettingsDslImpl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalKotlinGradlePluginApi::class, InternalKotlinGradlePluginApi::class)
class KotlinNativeSettingsDslTest {

    @Test
    fun `settings DSL value overrides the Gradle property`() {
        val project = nativeProject(gradleProperties = mapOf(KOTLIN_NATIVE_LINK_MAX_WORKERS to "2")) { linkMaxWorkers.set(4) }

        assertEquals(4, project.maxParallelUsagesOf<KotlinNativeLink>())
    }

    @Test
    fun `Gradle property applies to a value the settings DSL does not set`() {
        val project = nativeProject(
            gradleProperties = mapOf(KOTLIN_NATIVE_COMPILE_MAX_WORKERS to "1", KOTLIN_NATIVE_LINK_MAX_WORKERS to "2"),
        ) {
            linkMaxWorkers.set(4)
        }

        assertEquals(1, project.maxParallelUsagesOf<KotlinNativeCompile>())
        assertEquals(4, project.maxParallelUsagesOf<KotlinNativeLink>())
    }

    @Test
    fun `settings DSL value from an environment variable provider falls back to orElse when the variable is not set`() {
        val project = nativeProject { project ->
            linkMaxWorkers.set(project.providers.environmentVariable(UNSET_ENVIRONMENT_VARIABLE).map { 2 }.orElse(4))
        }

        assertEquals(4, project.maxParallelUsagesOf<KotlinNativeLink>())
    }

    @Test
    fun `settings DSL registers no limit service when neither the DSL nor the Gradle property is set`() {
        val project = buildProjectWithMPP()
        KotlinNativeSettingsDslImpl(project.objects, project.providers, project.gradle).registerParallelismLimitServices()

        assertFalse(project.isParallelismLimitServiceRegistered<KotlinNativeLink>())
    }

    private fun nativeProject(
        gradleProperties: Map<String, String> = emptyMap(),
        configureDsl: KotlinNativeSettingsDsl.(Project) -> Unit = {},
    ): Project {
        // Settings plugin registers services before project setup actions run.
        val project = buildProjectWithMPP(
            preApplyCode = {
                gradleProperties.forEach { (name, value) -> propertiesExtension.set(name, value) }
                val dsl = KotlinNativeSettingsDslImpl(objects, providers, gradle)
                dsl.configureDsl(this)
                dsl.registerParallelismLimitServices()
            }
        )
        project.evaluate()
        return project
    }

    private companion object {
        const val UNSET_ENVIRONMENT_VARIABLE = "KOTLIN_NATIVE_SETTINGS_DSL_TEST_UNSET"
    }
}
