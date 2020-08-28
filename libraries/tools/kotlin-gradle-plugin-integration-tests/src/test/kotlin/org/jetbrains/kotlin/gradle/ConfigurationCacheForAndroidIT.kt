/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.util.AGPVersion
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.Test

class ConfigurationCacheForAndroidIT : AbstractConfigurationCacheIT() {
    private val androidGradlePluginVersion: AGPVersion
        get() = AGPVersion.v4_2_0

    override fun defaultBuildOptions() =
        super.defaultBuildOptions().copy(
            androidHome = KotlinTestUtils.findAndroidSdk(),
            androidGradlePluginVersion = androidGradlePluginVersion,
            configurationCache = true,
            /* AGP causes a configuration cache problem:
                 - plugin 'com.android.internal.application': registration of listener on 'TaskExecutionGraph.addTaskExecutionListener' is unsupported

               which causes tests to fail when configuration-cache-problems=fail is used.
               However, everything works fine with WARN level reporting.
               TODO: switch to FAIL when AGP no longer causes the cache problem
             */
            configurationCacheProblems = ConfigurationCacheProblems.WARN
        )

    @Test
    fun testAndroidKaptProject() = with(Project("android-dagger", directoryPrefix = "kapt2")) {
        applyAndroid40Alpha4KotlinVersionWorkaround()
        projectDir.resolve("gradle.properties").appendText("\nkapt.incremental.apt=false")
        testConfigurationCacheOf(":app:compileDebugKotlin", ":app:kaptDebugKotlin", ":app:kaptGenerateStubsDebugKotlin")
    }

    @Test
    fun testKotlinAndroidProject() = with(Project("AndroidProject")) {
        applyAndroid40Alpha4KotlinVersionWorkaround()
        testConfigurationCacheOf(":Lib:compileFlavor1DebugKotlin", ":Android:compileFlavor1DebugKotlin")
    }

    /**
     * Android Gradle plugin 4.0-alpha4 depends on the EAP versions of some o.j.k modules.
     * Force the current Kotlin version, so the EAP versions are not queried from the
     * test project's repositories, where there's no 'kotlin-eap' repo.
     * TODO remove this workaround once an Android Gradle plugin version is used that depends on the stable Kotlin version
     */
    private fun Project.applyAndroid40Alpha4KotlinVersionWorkaround() {
        setupWorkingDir()

        val resolutionStrategyHack = """
            configurations.all { 
                resolutionStrategy.dependencySubstitution.all { dependency ->
                    def requested = dependency.requested
                    if (requested instanceof ModuleComponentSelector && requested.group == 'org.jetbrains.kotlin') {
                        dependency.useTarget requested.group + ':' + requested.module + ':' + '${defaultBuildOptions().kotlinVersion}'
                    }
                }
            }
        """.trimIndent()

        gradleBuildScript().appendText(
            "\n" + """
            buildscript {
                $resolutionStrategyHack
            }
            $resolutionStrategyHack
        """.trimIndent()
        )
    }
}
