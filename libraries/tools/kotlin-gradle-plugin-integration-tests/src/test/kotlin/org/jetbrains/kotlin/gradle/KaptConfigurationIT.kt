/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.process.CommandLineArgumentProvider
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.tasks.BaseKapt
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.appendText

@DisplayName("Generic Kapt configuration")
@OtherGradlePluginTests
class KaptConfigurationIT : KGPBaseTest() {

    @DisplayName("KT-58009: both deprecated and the new API may populate kapt options")
    @GradleTest
    @TestMetadata("kapt2/simple")
    fun kaptOptionsAreCombined(gradleVersion: GradleVersion) {
        project("kapt2/simple", gradleVersion) {
            gradleProperties.appendText("\nkapt.verbose=true")
            buildScriptInjection {
                project.tasks.withType(BaseKapt::class.java).configureEach {
                    @Suppress("DEPRECATION")
                    it.annotationProcessorOptionProviders.add(
                        listOf(CommandLineArgumentProvider { listOf("-Aoption1=kt58009", "-Aoption2=kt58009") })
                    )
                    it.annotationProcessorOptionsProviders.add(
                        CommandLineArgumentProvider { listOf("-Aoption3=kt58009", "-Aoption4=kt58009") }
                    )
                }
            }
            build(":kaptKotlin") {
                assertOutputContains("option1=kt58009=, option2=kt58009=, option3=kt58009=, option4=kt58009=")
            }
        }
    }
}