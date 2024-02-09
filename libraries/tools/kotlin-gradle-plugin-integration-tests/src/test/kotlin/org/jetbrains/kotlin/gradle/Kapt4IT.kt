/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
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

    @Disabled("Doesn't make sense in Kapt 4")
    override fun useGeneratedKotlinSourceK2(gradleVersion: GradleVersion) {}

    @Disabled("Doesn't make sense in Kapt 4")
    override fun fallBackModeWithUseK2(gradleVersion: GradleVersion) {}

    @Disabled("Doesn't make sense in Kapt 4")
    override fun fallBackModeWithLanguageVersion2_0(gradleVersion: GradleVersion) {}

    @Disabled("Doesn't make sense in Kapt 4")
    override fun useK2KaptProperty(gradleVersion: GradleVersion) {}

    @DisplayName("KT-61879: K2 KAPT works with proguarded compiler jars and enum class")
    @GradleTest
    fun testEnumClass(gradleVersion: GradleVersion) {
        project("simple".withPrefix, gradleVersion) {
            javaSourcesDir().resolve("test.kt").appendText("\nenum class TestEnum")
            build("build") {
                assertKaptSuccessful()
                assertFileExists(kotlinClassesDir().resolve("example/TestEnum.class"))
            }
        }
    }
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
}

fun TestProject.forceKapt4() {
    projectPath.walk().forEach {
        when (it.fileName.name) {
            "build.gradle" -> it.appendText(
                """
                
                try {
                    Class.forName('org.jetbrains.kotlin.gradle.tasks.KotlinCompile')
                    tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile).configureEach {
                       compilerOptions.freeCompilerArgs.addAll(['-Xuse-kapt4', '-Xsuppress-version-warnings'])
                    }
                } catch(ClassNotFoundException ignore) {
                }
                
                """.trimIndent()
            )
            "build.gradle.kts" -> it.appendText(
                """
                
                try {
                    Class.forName("org.jetbrains.kotlin.gradle.tasks.KotlinCompile")
                    tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile::class.java).configureEach {
                       compilerOptions.freeCompilerArgs.addAll(listOf("-Xuse-kapt4", "-Xsuppress-version-warnings"))
                    }
                } catch(ignore: ClassNotFoundException) {
                }
                
                """.trimIndent()
            )
        }
    }
}
