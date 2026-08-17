/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.JavaVersion
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName

@DisplayName("Kapt caching inside Gradle daemon")
@DaemonsGradlePluginTests
class KaptAndGradleDaemon : KGPDaemonsBaseTest() {

    override val defaultBuildOptions: BuildOptions = super.defaultBuildOptions
        .copy(
            kaptOptions = BuildOptions.KaptOptions(
                verbose = true,
                includeCompileClasspath = false
            )
        )

    @DisplayName("Javac should be loaded only once")
    // "com.sun.tools.javac.util.Context" is only available in JDK 1.8
    @GradleTestVersions(maxVersion = TestVersions.Gradle.G_8_9)
    @JdkVersions(versions = [JavaVersion.VERSION_1_8])
    @GradleWithJdkTest
    fun testJavacIsLoadedOnce(
        gradleVersion: GradleVersion,
        providedJdk: JdkVersions.ProvidedJdk,
    ) {
        project(
            "javacIsLoadedOnce".withPrefix,
            gradleVersion,
            buildJdk = providedJdk.location,
        ) {
            build("assemble") {
                val loadsCount = "Loaded com.sun.tools.javac.util.Context from"
                    .toRegex(RegexOption.LITERAL)
                    .findAll(output)
                    .count()

                assert(loadsCount == 1) {
                    """
                    |${printBuildOutput()}
                    |
                    | 'javac' is loaded not only once: $loadsCount times.
                    """.trimMargin()
                }
            }
        }
    }

    private val String.withPrefix get() = "kapt/$this"
}
