/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.GradleTest
import org.jetbrains.kotlin.gradle.testbase.TestProject
import org.jetbrains.kotlin.gradle.testbase.assertFileExists
import org.jetbrains.kotlin.gradle.testbase.build
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.appendText
import kotlin.io.path.name
import kotlin.io.path.walk

@DisplayName("Kapt 4 base checks")
class Kapt4IT : Kapt3IT() {
    override val defaultBuildOptions = super.defaultBuildOptions.copyEnsuringK2()

    override fun TestProject.customizeProject() {
        forceK2Kapt()
    }

    @Disabled("KT-71786: K2KAPT task does not fail")
    @GradleTest
    override fun testFailOnTopLevelSyntaxError(gradleVersion: GradleVersion) {}

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

    @DisplayName("K2 kapt cannot be enabled in K1")
    @GradleTest
    override fun testK2KaptCannotBeEnabledInK1(gradleVersion: GradleVersion) {}
}

@DisplayName("Kapt 4 with classloaders cache")
class Kapt4ClassLoadersCacheIT : Kapt3ClassLoadersCacheIT() {
    override val defaultBuildOptions = super.defaultBuildOptions.copyEnsuringK2()

    override fun TestProject.customizeProject() {
        forceK2Kapt()
    }

    @Disabled("KT-71786: K2KAPT task does not fail")
    @GradleTest
    override fun testFailOnTopLevelSyntaxError(gradleVersion: GradleVersion) {}
}

fun TestProject.forceK1Kapt() {
    forceK2Kapt(false)
}

fun TestProject.forceK2Kapt() {
    forceK2Kapt(true)
}

private fun TestProject.forceK2Kapt(value: Boolean) {
    projectPath.walk().forEach {
        when (it.fileName.name) {
            "gradle.properties" -> it.appendText("\nkapt.use.k2=$value\n")
        }
    }
}
