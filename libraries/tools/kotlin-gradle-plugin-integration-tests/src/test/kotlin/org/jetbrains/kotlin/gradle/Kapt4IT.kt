/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.TestProject
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.appendText
import kotlin.io.path.name
import kotlin.io.path.walk

@DisplayName("Kapt 4 base checks")
class Kapt4IT : Kapt3IT() {
    override val defaultBuildOptions = super.defaultBuildOptions.copyEnsuringK2()

    override fun TestProject.customizeProject() {
        forceKapt4()
    }


    @Disabled("Enable when KT-61845 is fixed")
    override fun testKt18799(gradleVersion: GradleVersion) {}

    @Disabled("Doesn't make sense in Kapt 4")
    override fun useGeneratedKotlinSourceK2(gradleVersion: GradleVersion) {}

    @Disabled("Doesn't make sense in Kapt 4")
    override fun fallBackModeWithUseK2(gradleVersion: GradleVersion) {}

    @Disabled("Doesn't make sense in Kapt 4")
    override fun fallBackModeWithLanguageVersion2_0(gradleVersion: GradleVersion) {}

    @Disabled("Doesn't make sense in Kapt 4")
    override fun useK2KaptProperty(gradleVersion: GradleVersion) {}

    @Disabled("Doesn't make sense in Kapt 4")
    override fun testRepeatableAnnotationsWithOldJvmBackend(gradleVersion: GradleVersion) {}
}

@DisplayName("Kapt 4 with classloaders cache")
class Kapt4ClassLoadersCacheIT : Kapt3ClassLoadersCacheIT() {
    override val defaultBuildOptions = super.defaultBuildOptions.copyEnsuringK2()

    override fun TestProject.customizeProject() {
        forceKapt4()
    }

    @Disabled("Enable when KT-61845 is fixed")
    override fun testKt18799(gradleVersion: GradleVersion) {}

    @Disabled("Doesn't make sense in Kapt 4")
    override fun useGeneratedKotlinSourceK2(gradleVersion: GradleVersion) {}

    @Disabled("Doesn't make sense in Kapt 4")
    override fun fallBackModeWithUseK2(gradleVersion: GradleVersion) {}

    @Disabled("Doesn't make sense in Kapt 4")
    override fun fallBackModeWithLanguageVersion2_0(gradleVersion: GradleVersion) {}

    @Disabled("Doesn't make sense in Kapt 4")
    override fun testRepeatableAnnotationsWithOldJvmBackend(gradleVersion: GradleVersion) {}

    @Disabled("Doesn't work in 2.0. Neither with Kapt 3 nor with Kapt 4")
    override fun testMPPKaptPresence(gradleVersion: GradleVersion) {}
}

fun TestProject.forceKapt4() {
    projectPath.walk().forEach {
        when (it.fileName.name) {
            "build.gradle" -> it.appendText(
                """
                
                pluginManager.withPlugin('kotlin') {
                    tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile).configureEach {
                       compilerOptions.freeCompilerArgs.addAll(['-Xuse-kapt4', '-Xsuppress-version-warnings'])
                    }
                }
                
                """.trimIndent()
            )
            "build.gradle.kts" -> it.appendText(
                """
                
                pluginManager.withPlugin("kotlin") {
                    tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile::class.java).configureEach {
                       compilerOptions.freeCompilerArgs.addAll(listOf("-Xuse-kapt4", "-Xsuppress-version-warnings"))
                    }
                }
                
                """.trimIndent()
            )
        }
    }
}
