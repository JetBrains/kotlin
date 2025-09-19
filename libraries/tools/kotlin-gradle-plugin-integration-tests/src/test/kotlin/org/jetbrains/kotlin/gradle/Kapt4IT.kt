/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.GradleTest
import org.jetbrains.kotlin.gradle.testbase.assertFileExists
import org.jetbrains.kotlin.gradle.testbase.build
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.appendText

@DisplayName("Kapt 4 base checks")
class Kapt4IT : Kapt3IT() {
    override val defaultBuildOptions = super.defaultBuildOptions.copyEnsuringK2()

    @DisplayName("KT18799: generate annotation value for constant values in documented types")
    @GradleTest
    override fun testKt18799(gradleVersion: GradleVersion) {
        super.testKt18799(gradleVersion)
    }

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

    @Disabled("KT-71786: K2KAPT task does not fail")
    @GradleTest
    override fun testFailOnTopLevelSyntaxError(gradleVersion: GradleVersion) {}
}
