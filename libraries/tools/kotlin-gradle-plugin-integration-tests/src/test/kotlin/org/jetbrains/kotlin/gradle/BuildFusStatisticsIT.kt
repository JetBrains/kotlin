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
    @GradleTestVersions(
        additionalVersions = [
            TestVersions.Gradle.G_8_0,
            TestVersions.Gradle.G_8_2,
            TestVersions.Gradle.G_8_3,
            TestVersions.Gradle.G_8_11
        ],
    )
    fun testCompatibilityBuildSrcWithKotlinDsl(gradleVersion: GradleVersion) {
        project(
            "buildSrcUsingKotlinCompilationAndKotlinPlugin",
            gradleVersion,
            buildOptions = defaultBuildOptions
                .copy(logLevel = LogLevel.DEBUG)
                // on Gradle 7 with CC enabled `KotlinBuildStatsBeanService` is being instantiated in another classpath
                .disableConfigurationCacheForGradle7(gradleVersion),
        ) {
            build("assemble", "-Pkotlin.session.logger.root.path=$projectPath") {
                //register build service for buildSrc.
                when {
                    // until 8.0, Gradle was embedding the Kotlin version that used a slightly different approach to detect build finish,
                    // so the service was unregistered after the finish of the buildSrc build
                    // and then registered again in the root build
                    gradleVersion < GradleVersion.version(TestVersions.Gradle.G_8_0) -> {
                        assertOutputContainsExactlyTimes(
                            "Instantiated class org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatsService: new instance", // the  service for buildSrc
                            1
                        )
                        assertOutputContainsExactlyTimes(
                            "Instantiated class org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatsBeanService: new instance", // the legacy service for compatibility
                            1
                        )
                        assertOutputContainsExactlyTimes(
                            "Instantiated class org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatsBeanService_v2: new instance", // the current default version of the service
                            1
                        )
                    }
                    gradleVersion < GradleVersion.version(TestVersions.Gradle.G_8_3) -> {
                        assertOutputContainsExactlyTimes(
                            "Instantiated class org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatsService: new instance", // the legacy service for compatibility
                            1
                        )
                        assertOutputContainsExactlyTimes(
                            "Instantiated class org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatsBeanService_v2: new instance", // the current default version of the service
                            1
                        )
                    }
                    //for gradle 8.3 kotlin 1.9.0 is used, log message is changed
                    gradleVersion < GradleVersion.version(TestVersions.Gradle.G_8_5) -> {
                        assertOutputContainsExactlyTimes(
                            "Register JMX service for backward compatibility", // the legacy service for compatibility
                            1
                        )
                        assertOutputContainsExactlyTimes(
                            "Instantiated class org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatsService_v2: new instance", // the current default version of the service
                            1
                        )
                    }
                    //for gradle 8.5+ kotlin 1.9.20+ versions KGP from buildSrc registered both services
                    gradleVersion < GradleVersion.version(TestVersions.Gradle.G_8_11) -> {
                        assertOutputContainsExactlyTimes(
                            "Instantiated class org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatsService: new instance", // the legacy service for compatibility
                            1
                        )
                        assertOutputContainsExactlyTimes(
                            "Instantiated class org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatsService_v2: new instance", // the current default version of the service
                            1
                        )

                    }
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

                        //from main project
                        assertOutputContainsExactlyTimes(
                            "[KOTLIN] Initialize FlowActionBuildFusService${'$'}Inject",
                            1
                        )
                    }
                    // Since Gradle 9.0 TBA
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

                        //from buildSrc and main project
                        assertOutputContainsExactlyTimes(
                            "[KOTLIN] Initialize FlowActionBuildFusService${'$'}Inject",
                            2
                        )
                    }
                }

                assertOutputDoesNotContain("[org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatHandler] Could not execute")
            }
        }
    }
}