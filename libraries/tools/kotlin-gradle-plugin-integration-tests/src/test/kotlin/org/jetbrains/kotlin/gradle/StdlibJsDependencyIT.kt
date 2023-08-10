/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.replaceText
import org.junit.jupiter.api.DisplayName

@MppGradlePluginTests
@DisplayName("Multiplatform stdlib-js explicit dependency test")
class StdlibJsDependencyIT : KGPBaseTest() {

    @DisplayName("Js and jvm targets with stdlib-js dependency in jsMain assembles")
    @GradleTest
    fun testJsAndJvmTargetWithDependencyInJsMain(gradleVersion: GradleVersion) {
        projectWithJsTargetAndStdlibJsDependency(
            otherTarget = "jvm()",
            sourceSetWithStdlibJsDependency = "jsMain",
            gradleVersion
        ) {
            build("assemble")
        }
    }

    @DisplayName("Only js target with stdlib-js dependency in jsMain assembles")
    @GradleTest
    fun testOnlyJsTargetWithDependencyInJsMain(gradleVersion: GradleVersion) {
        projectWithJsTargetAndStdlibJsDependency(
            otherTarget = "",
            sourceSetWithStdlibJsDependency = "jsMain",
            gradleVersion
        ) {
            build("assemble")
        }
    }

    @DisplayName("Js and jvm target with stdlib-js dependency in commonMain fails with an ambiguity error")
    @GradleTest
    fun testJsAndJvmTargetWithDependencyInCommonMain(gradleVersion: GradleVersion) {
        projectWithJsTargetAndStdlibJsDependency(
            otherTarget = "jvm()",
            sourceSetWithStdlibJsDependency = "commonMain",
            gradleVersion
        ) {
            buildAndFail("assemble") {
                assertOutputContains(
                    "cannot choose between the following variants of org.jetbrains.kotlin:kotlin-stdlib-js"
                )
            }
        }
    }

    @DisplayName("Only js target with stdlib-js dependency in commonMain assembles")
    @GradleTest
    fun testOnlyJsTargetWithDependencyInCommonMain(gradleVersion: GradleVersion) {
        projectWithJsTargetAndStdlibJsDependency(
            otherTarget = "",
            sourceSetWithStdlibJsDependency = "commonMain",
            gradleVersion
        ) {
            build("assemble")
        }
    }

    private fun projectWithJsTargetAndStdlibJsDependency(
        otherTarget: String,
        sourceSetWithStdlibJsDependency: String,
        gradleVersion: GradleVersion,
        perform: TestProject.() -> Unit
    ) {
        project(
            "stdlib-js-dependency",
            gradleVersion,
        ) {
            buildGradleKts.replaceText("<otherTarget>", otherTarget)
            buildGradleKts.replaceText("<sourceSetWithStdlibJsDependency>", sourceSetWithStdlibJsDependency)

            perform()
        }
    }

}