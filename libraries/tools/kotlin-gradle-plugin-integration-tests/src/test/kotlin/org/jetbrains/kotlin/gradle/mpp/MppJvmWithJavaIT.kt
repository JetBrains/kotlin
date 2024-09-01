/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.gradle.mpp

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics.DeprecatedJvmWithJavaPresetDiagnostic
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.replaceText
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import java.nio.file.Path
import java.util.zip.ZipFile
import kotlin.io.path.*
import kotlin.test.assertContains

@MppGradlePluginTests
class MppJvmWithJavaIT : KGPBaseTest() {

    @GradleTest
    @TestMetadata(value = "new-mpp-lib-and-app")
    @Disabled("KT-60745")
    fun testJvmWithJavaEquivalence(
        gradleVersion: GradleVersion,
    ): Unit = doTestJvmWithJava(gradleVersion, testJavaSupportInJvmTargets = false)

    @GradleTest
    @GradleTestVersions(minVersion = "8.0") // Shadow requires Gradle 8.0+
    @TestMetadata(value = "new-mpp-lib-and-app")
    fun testJavaSupportInJvmTargets(
        gradleVersion: GradleVersion,
    ): Unit = doTestJvmWithJava(gradleVersion, testJavaSupportInJvmTargets = true)

    private fun doTestJvmWithJava(
        gradleVersion: GradleVersion,
        testJavaSupportInJvmTargets: Boolean,
    ) {
        project(
            projectName = "new-mpp-lib-and-app/sample-lib",
            gradleVersion = gradleVersion,
        ) {
            includeOtherProjectAsSubmodule(
                otherProjectName = "sample-lib-gradle-kotlin-dsl",
                pathPrefix = "new-mpp-lib-and-app",
            )
            val subproject = subProject("sample-lib-gradle-kotlin-dsl")

            buildGradle.replaceText(
                "shouldBeJs = true",
                "shouldBeJs = false",
            )

            subproject.buildGradleKts.replaceText(
                "shouldBeJs = true",
                "shouldBeJs = false",
            )

            val classesWithoutJava: Set<String> = buildSet {
                build("assemble") {
                    addAll(projectPath.resolve("build/classes").relativePathsOfDirectoryContents())
                }
            }

            buildGradle.replaceText(
                """//id("com.gradleup.shadow")""",
                """id("com.gradleup.shadow") version "${TestVersions.ThirdPartyDependencies.SHADOW_PLUGIN_VERSION}"""",
            )
            buildGradle.replaceText(
                """//id("application")""",
                """id("application")""",
            )
            // Check that Kapt works, generates and compiles sources
            buildGradle.replaceText(
                """//id("org.jetbrains.kotlin.kapt")""",
                """id("org.jetbrains.kotlin.kapt")""",
            )

            buildGradle.modify { script ->
                buildString {
                    if (testJavaSupportInJvmTargets) {
                        appendLine(script)
                        appendLine(
                            """
                            |kotlin.jvm("jvm6") {
                            |  withJava()
                            |  withJava() // also check that the function is idempotent
                            |}
                            """.trimMargin()
                        )
                    } else {
                        appendLine(
                            script
                                .replace("presets.jvm", "presets.jvmWithJava")
                                .replace("jvm(", "targetFromPreset(presets.jvmWithJava, ")
                        )
                    }

                    appendLine(
                        """
                        |application {
                        |    mainClass = 'com.example.lib.CommonKt'
                        |}
                        |
                        |dependencies {
                        |    jvm6MainImplementation("com.google.dagger:dagger:${TestVersions.ThirdPartyDependencies.GOOGLE_DAGGER}")
                        |    kapt("com.google.dagger:dagger-compiler:2.24")
                        |    kapt(project(":sample-lib-gradle-kotlin-dsl"))
                        |    
                        |    // also check incremental Kapt class structure configurations, KT-33105
                        |    jvm6MainImplementation(project(":sample-lib-gradle-kotlin-dsl")) 
                        |}
                        """.trimMargin()
                    )
                }
            }

            // also check incremental Kapt class structure configurations, KT-33105
            gradleProperties.append("kapt.incremental.apt=true")

            // Check Kapt:
            projectPath.resolve("src/jvm6Main/kotlin/Main.kt")
                .append(
                    """
                    |interface Iface
                    |
                    |@dagger.Module
                    |object Module {
                    |    @JvmStatic @dagger.Provides
                    |    fun provideHeater(): Iface = object : Iface { }
                    |}
                    """.trimMargin()
                )

            val javaMainSrcDir = if (testJavaSupportInJvmTargets) "src/jvm6Main/java" else "src/main/java"
            val javaTestSrcDir = if (testJavaSupportInJvmTargets) "src/jvm6Test/java" else "src/test/java"

            projectPath.resolve(javaMainSrcDir).apply {
                createDirectories()

                // Check that Java can access the dependencies (kotlin-stdlib):
                resolve("JavaClassInJava.java").writeText(
                    """
                    |package com.example.lib;
                    |import kotlin.sequences.Sequence;
                    |class JavaClassInJava {
                    |    Sequence<String> makeSequence() { throw new UnsupportedOperationException(); }
                    |}
                    """.trimMargin()
                )

                // Add a Kotlin source file in the Java source root and check that it is compiled:
                resolve("KotlinClassInJava.kt").writeText(
                    """
                    |package com.example.lib
                    |class KotlinClassInJava
                    """.trimMargin()
                )
            }

            projectPath.resolve(javaTestSrcDir).apply {
                createDirectories()

                resolve("JavaTest.java").writeText(
                    """
                    |package com.example.lib;
                    |import org.junit.*;
                    |public class JavaTest {
                    |    @Test
                    |    public void testAccessKotlin() {
                    |        MainKt.expectedFun();
                    |        MainKt.x();
                    |        new KotlinClassInJava();
                    |        new JavaClassInJava();
                    |    }
                    |}
                    """.trimMargin()
                )
            }

            build(
                "clean", "build", "run", "shadowJar",
                buildOptions = defaultBuildOptions
                    .suppressDeprecationWarningsOn("KT-66542: withJava() produces deprecation warning") {
                        gradleVersion >= GradleVersion.version(TestVersions.Gradle.G_8_7)
                    }
            ) {
                val expectedMainClasses =
                    classesWithoutJava + setOf(
                        // classes for Kapt test:
                        "java/main/com/example/lib/Module_ProvideHeaterFactory.class",
                        "kotlin/jvm6/main/com/example/lib/Module\$provideHeater\$1.class",
                        "kotlin/jvm6/main/com/example/lib/Iface.class",
                        "kotlin/jvm6/main/com/example/lib/Module.class",
                        // other added classes:
                        "kotlin/jvm6/main/com/example/lib/KotlinClassInJava.class",
                        "java/main/com/example/lib/JavaClassInJava.class",
                        "java/test/com/example/lib/JavaTest.class"
                    )

                val actualClasses = projectPath.resolve("build/classes").relativePathsOfDirectoryContents()
                assertEquals(expectedMainClasses.sorted().joinToString("\n"), actualClasses.sorted().joinToString("\n"))

                val jvmTestTaskName = if (testJavaSupportInJvmTargets) "jvm6Test" else "test"
                assertTasksExecuted(":$jvmTestTaskName")

                if (testJavaSupportInJvmTargets) {
                    assertFileInProjectExists("build/reports/tests/allTests/classes/com.example.lib.JavaTest.html")
                }

                if (testJavaSupportInJvmTargets) {
                    assertNoDiagnostic(DeprecatedJvmWithJavaPresetDiagnostic)
                } else {
                    assertHasDiagnostic(DeprecatedJvmWithJavaPresetDiagnostic)
                }

                assertTasksExecuted(":run")
                assertOutputContains(">>> Common.kt >>> main()")

                assertTasksExecuted(":shadowJar")

                val entries = ZipFile(projectPath.resolve("build/libs/sample-lib-1.0-all.jar").toFile()).use { zip ->
                    zip.entries().asSequence().map { it.name }.toSet()
                }
                assertContains(entries, "kotlin/Pair.class")
                assertContains(entries, "com/example/lib/CommonKt.class")
                assertContains(entries, "com/example/lib/MainKt.class")
                assertContains(entries, "com/example/lib/JavaClassInJava.class")
                assertContains(entries, "com/example/lib/KotlinClassInJava.class")
            }
        }
    }

    companion object {
        /** Get the relative paths of all files within this directory. */
        private fun Path.relativePathsOfDirectoryContents(): Set<String> {
            return walk()
                .filter { it.isRegularFile() }
                .map { it.relativeTo(this@relativePathsOfDirectoryContents).invariantSeparatorsPathString }
                .toSet()
        }
    }
}
