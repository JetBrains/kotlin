/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName

@DisplayName("Build FUS statistics")
class BuildFusStatisticsIT : KGPDaemonsBaseTest() {
    @DisplayName("works for project with buildSrc and kotlinDsl plugin")
    @GradleTest
    fun testCompatibilityBuildSrcWithKotlinDsl(gradleVersion: GradleVersion) {
        project(
            "buildSrcUsingKotlinCompilationAndKotlinPlugin",
            gradleVersion,
            buildOptions = defaultBuildOptions
                .copy(logLevel = LogLevel.DEBUG)
        ) {
            build("assemble", buildOptions = buildOptions.copy(pathToFusReportDirectory = { projectPath })) {
                //register build service for buildSrc.
                when {
                    // Since Gradle 8.11 Kotlin version 2.0.20 is used which contains only one service
                    gradleVersion < GradleVersion.version(TestVersions.Gradle.G_9_0) -> {
                        assertOutputContainsExactlyTimes(
                            "class org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatsBeanService_v2 is already instantiated in another classpath",
                            1
                        )
                        assertOutputContainsExactlyTimes(
                            "class org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatsBeanService is already instantiated in another classpath",
                            1
                        )

                        // from buildSrc project
                        assertOutputContainsExactlyTimes(
                            "[KOTLIN] Initialize BuildFusService${'$'}Inject",
                            1
                        )

                        //from the main project
                        assertOutputContainsExactlyTimes(
                            "[KOTLIN] Initialize build service FlowActionBuildFusService${'$'}Inject",
                            1
                        )
                    }
                    // Since Gradle 9.0
                    gradleVersion < GradleVersion.version(TestVersions.Gradle.G_9_4) -> {
                        assertOutputContainsExactlyTimes(
                            "class org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatsBeanService_v2 is already instantiated in another classpath",
                            1
                        )
                        assertOutputContainsExactlyTimes(
                            "class org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatsBeanService is already instantiated in another classpath",
                            1
                        )

                        // Old service is not registered neither by main or buildSrc builds
                        assertOutputContainsExactlyTimes(
                            "[KOTLIN] Initialize BuildFusService${'$'}Inject",
                            0
                        )

                        //from buildSrc project
                        assertOutputContainsExactlyTimes(
                            "[KOTLIN] Initialize FlowActionBuildFusService${'$'}Inject",
                            1
                        )

                        //from main project
                        assertOutputContainsExactlyTimes(
                            "[KOTLIN] Initialize build service FlowActionBuildFusService${'$'}Inject",
                            1
                        )
                    }
                    // Since Gradle 9.4
                    else -> {
                        assertOutputContainsExactlyTimes(
                            "class org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatsBeanService_v2 is already instantiated in another classpath",
                            1
                        )
                        assertOutputContainsExactlyTimes(
                            "class org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatsBeanService is already instantiated in another classpath",
                            1
                        )

                        // Old service is not registered neither by main or buildSrc builds
                        assertOutputContainsExactlyTimes(
                            "[KOTLIN] Initialize BuildFusService${'$'}Inject",
                            0
                        )

                        //from buildSrc project
                        assertOutputContainsExactlyTimes(
                            "[KOTLIN] Initialize FlowActionBuildFusService${'$'}Inject",
                            0
                        )

                        //from main project
                        assertOutputContainsExactlyTimes(
                            "[KOTLIN] Initialize build service FlowActionBuildFusService${'$'}Inject",
                            2
                        )
                    }
                }

                assertOutputDoesNotContain("[org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatHandler] Could not execute")
            }
        }
    }
}
