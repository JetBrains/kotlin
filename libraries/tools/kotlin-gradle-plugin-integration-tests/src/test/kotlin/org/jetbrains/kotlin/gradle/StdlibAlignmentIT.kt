/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.JavaVersion
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.replaceText
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.appendText

@DisplayName("Stdlib 1.8+ version alignment")
class StdlibAlignmentIT : KGPBaseTest() {

    val constrainedAlignmentVersion = "1.8.0"

    @AndroidGradlePluginTests
    @DisplayName("stdlib-jdk7, stdlib-jdk8 versions aligned with stdlib:1.8+")
    @GradleAndroidTest
    fun testStdlibAlignmentAndroid(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk
    ) {
        project(
            "AndroidSimpleApp",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                androidVersion = agpVersion
            ),
            buildJdk = jdkVersion.location,
        ) {
            // Adding dependency that pulls transitively older versions
            // of stdlib-jdk8
            buildGradle.appendText(
                """
                |
                |dependencies {
                |    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.5.2"
                |}
                """.trimMargin()
            )

            build("checkDebugDuplicateClasses")
        }
    }

    @AndroidGradlePluginTests
    @DisplayName("alignment is possible to disable")
    @GradleAndroidTest
    fun testDisableStdlibAlignmentAndroid(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk
    ) {
        project(
            "AndroidSimpleApp",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                androidVersion = agpVersion
            ),
            buildJdk = jdkVersion.location,
        ) {
            // Adding dependency that pulls transitively older versions
            // of stdlib-jdk8
            buildGradle.appendText(
                """
                |
                |dependencies {
                |    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.5.2"
                |}
                """.trimMargin()
            )
            buildGradle.replaceText("kotlin-stdlib:\$kotlin_version", "kotlin-stdlib:1.9.0")

            gradleProperties.appendText(
                """
                |
                |kotlin.stdlib.jdk.variants.version.alignment=false
                """.trimMargin()
            )

            buildAndFail("checkDebugDuplicateClasses") {
                assertOutputContains("Duplicate class kotlin.internal.jdk8.JDK8PlatformImplementations")
            }
        }
    }

    @JvmGradlePluginTests
    @DisplayName("alignment is working when stdlib-jdk7:1.8+ is added as dependency")
    @GradleTest
    fun stdlibJdk7Alignment(gradleVersion: GradleVersion) {
        project("sourceSetsKotlinDsl", gradleVersion) {
            buildGradleKts.appendText(
                """
                |
                |dependencies {
                |   implementation(kotlin("stdlib-jdk7"))
                |   implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")
                |}
                """.trimMargin()
            )

            build("dependencies", "--configuration", "compileClasspath") {
                assertOutputContains(
                    """
                    |\--- org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2
                    |     \--- org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.5.2
                    |          +--- org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.5.30 -> ${constrainedAlignmentVersion}
                    """.trimMargin().normalizeLineEndings()
                )
            }
        }
    }

    @JvmGradlePluginTests
    @DisplayName("alignment in Kotlin DSL")
    @GradleTest
    fun stdlibJdkAlignmentKotlinDsl(gradleVersion: GradleVersion) {
        for ((stdlibVersion, alignedVersion) in listOf(
            defaultBuildOptions.kotlinVersion to constrainedAlignmentVersion,
            "1.9.0" to "1.9.0"
        )) {
            project("sourceSetsKotlinDsl", gradleVersion) {
                buildGradleKts.appendText(
                    """
                    |
                    |dependencies {
                    |   implementation(kotlin("stdlib", version = "$stdlibVersion"))
                    |   implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")
                    |}
                    """.trimMargin()
                )

                build("dependencies", "--configuration", "compileClasspath") {
                    assertOutputContains(
                        """
                        |\--- org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2
                        |     \--- org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.5.2
                        |          +--- org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.5.30 -> ${alignedVersion}
                        """.trimMargin().normalizeLineEndings()
                    )
                }
            }
        }
    }

    @JvmGradlePluginTests
    @DisplayName("KT-54653: alignment should not work for unrelated configuration")
    @GradleTest
    fun stdlibJdkAlignmentUnrelatedConfigurations(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            buildGradle.modify {
                """
                |$it
                |
                |// Stdlib-jdk8 version should be <1.8.0
                |configurations.create("specificDeps")
                |
                |dependencies {
                |    specificDeps "org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.20"
                |    implementation "org.jetbrains.kotlin:kotlin-stdlib:${buildOptions.kotlinVersion}"
                |}
                """.trimMargin()
            }

            build("dependencies") {
                assertOutputDoesNotContain(
                    """
                    |
                    |specificDeps
                    |\--- org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.20 -> org.jetbrains.kotlin:kotlin-stdlib:1.7.20
                    |     +--- org.jetbrains.kotlin:kotlin-stdlib-common:1.7.20
                    |     \--- org.jetbrains:annotations:13.0
                    """.trimMargin().normalizeLineEndings()
                )
            }
        }
    }

    @JvmGradlePluginTests
    @DisplayName("KT-54653: alignment works for complex resolvable configurations hierarchy")
    @GradleTest
    fun stdlibJdkAlignmentComplexResolvableConfiguration(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            buildGradle.modify {
                """
                |$it
                |
                |// Stdlib-jdk8 version should be <1.8.0
                |def specificDepsConf = configurations.create("specificDeps") {
                |    setCanBeResolved(true)
                |}
                |
                |configurations.create("specificDepsChild") {
                |    setCanBeResolved(true)
                |    extendsFrom specificDepsConf
                |}
                |
                |dependencies {
                |    specificDeps "org.jetbrains.kotlin:kotlin-stdlib:${buildOptions.kotlinVersion}"
                |    // brings transitively older stdlib-jdk7,8 dependencies
                |    specificDepsChild "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2"
                |}
                """.trimMargin()
            }

            build("dependencies") {
                assertOutputDoesNotContain(
                    """
                    |
                    |specificDepsChild
                    |+--- org.jetbrains.kotlin:kotlin-stdlib:${buildOptions.kotlinVersion}
                    ||    +--- org.jetbrains.kotlin:kotlin-stdlib-common:${buildOptions.kotlinVersion}
                    ||    \--- org.jetbrains:annotations:13.0
                    |\--- org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2
                    |     \--- org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.5.2
                    |          +--- org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.5.30
                    |          |    +--- org.jetbrains.kotlin:kotlin-stdlib:1.5.30 -> ${buildOptions.kotlinVersion} (*)
                    |          |    \--- org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.5.30
                    |          |         \--- org.jetbrains.kotlin:kotlin-stdlib:1.5.30 -> ${buildOptions.kotlinVersion} (*)
                    |          \--- org.jetbrains.kotlin:kotlin-stdlib-common:1.5.30 -> ${buildOptions.kotlinVersion}
                    """.trimMargin()
                )
            }
        }
    }

    @JvmGradlePluginTests
    @DisplayName("KT-54703: JPMS projects with dependency on kotlin.stdlib.jdk8 work as expected")
    @JdkVersions(versions = [JavaVersion.VERSION_11])
    @GradleTestVersions(minVersion = TestVersions.Gradle.G_7_0)
    @GradleWithJdkTest
    fun alignmentWorksCorrectlyForJPMS(
        gradleVersion: GradleVersion,
        providedJdk: JdkVersions.ProvidedJdk
    ) {
        project(
            "jpmsProject",
            gradleVersion,
            buildJdk = providedJdk.location
        ) {
            subProject("list").buildGradleKts.appendText(
                """
                |
                |dependencies {
                |    api("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.20")
                |}
                """.trimMargin()
            )
            subProject("list").javaSourcesDir()
                .resolve("module-info.java")
                .modify {
                    val lines = it.lines()
                    """
                    |${lines.first()}
                    |    requires transitive kotlin.stdlib.jdk8;
                    |${lines.drop(1).joinToString(separator = System.lineSeparator())}
                    """.trimMargin()
                }

            subProject("application").buildGradleKts.appendText(
                """
                |
                |dependencies {
                |    implementation("org.jetbrains.kotlin:kotlin-stdlib")
                |}
                """.trimMargin()
            )

            build(":application:assemble")
        }
    }
}