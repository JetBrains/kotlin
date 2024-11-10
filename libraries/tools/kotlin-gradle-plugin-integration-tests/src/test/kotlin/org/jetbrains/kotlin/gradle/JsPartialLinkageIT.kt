/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName

@JsGradlePluginTests
class JsPartialLinkageIT : KGPBaseTest() {
    @GradleTest
    @TestMetadata(value = "kt-72965-no-pl-warnings-on-SubclassOptInRequired-annotation-site")
    @DisplayName("KT-72965: No Partial Linkage warnings on SubclassOptInRequired annotation site")
    fun testNoPartialLinkageWarningsOnSubclassOptInRequiredAnnotationSite(gradleVersion: GradleVersion) {
        project(
            projectName = "kt-72965-no-pl-warnings-on-SubclassOptInRequired-annotation-site/lib",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(kotlinVersion = "2.0.20")
        ) {
            build("publish") {
                assertTasksExecuted(":publish")
            }
        }

        project(
            projectName = "kt-72965-no-pl-warnings-on-SubclassOptInRequired-annotation-site/app",
            gradleVersion = gradleVersion,
        ) {
            build("compileDevelopmentExecutableKotlinJs") {
                assertTasksExecuted(":compileDevelopmentExecutableKotlinJs")
                // TODO KT-72965: These warnings should go away.
                assertOutputContains(SUBCLASS_OPT_IN_REQUIRED_CONSTRUCTOR_CANT_BE_CALLED)
                assertOutputContains(SUBCLASS_OPT_IN_REQUIRED_NO_CONSTRUCTOR_FOUND)
            }
        }
    }

    @GradleTest
    @TestMetadata(value = "kt-72965-pl-warnings-on-SubclassOptInRequired-constructor-call")
    @DisplayName("KT-72965: Ignore Partial Linkage warnings on SubclassOptInRequired annotation site")
    fun testPartialLinkageWarningsOnSubclassOptInRequiredConstructorCall(gradleVersion: GradleVersion) {
        project(
            projectName = "kt-72965-pl-warnings-on-SubclassOptInRequired-constructor-call/lib",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(kotlinVersion = "2.0.20")
        ) {
            build("publish") {
                assertTasksExecuted(":publish")
            }
        }

        project(
            projectName = "kt-72965-pl-warnings-on-SubclassOptInRequired-constructor-call/app",
            gradleVersion = gradleVersion,
        ) {
            build("compileDevelopmentExecutableKotlinJs") {
                assertTasksExecuted(":compileDevelopmentExecutableKotlinJs")
                assertOutputContains(SUBCLASS_OPT_IN_REQUIRED_CONSTRUCTOR_CANT_BE_CALLED)
                assertOutputContains(SUBCLASS_OPT_IN_REQUIRED_NO_CONSTRUCTOR_FOUND)
            }
        }
    }

    companion object {
        private val SUBCLASS_OPT_IN_REQUIRED_CONSTRUCTOR_CANT_BE_CALLED = Regex(
            "w: .+: Constructor 'SubclassOptInRequired\\.<init>' can not be called: No constructor found for symbol 'kotlin/SubclassOptInRequired\\.<init>|<init>\\(kotlin\\.reflect\\.KClass<out|kotlin\\.Annotation>\\)\\{}\\[0]'"
        )

        private const val SUBCLASS_OPT_IN_REQUIRED_NO_CONSTRUCTOR_FOUND =
            "w: <missing declarations>: No constructor found for symbol 'kotlin/SubclassOptInRequired.<init>|<init>(kotlin.reflect.KClass<out|kotlin.Annotation>){}[0]'"
    }
}