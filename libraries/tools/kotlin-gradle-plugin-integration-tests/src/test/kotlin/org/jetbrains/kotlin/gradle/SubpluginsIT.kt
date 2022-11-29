/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.checkBytecodeContains
import org.jetbrains.kotlin.test.util.JUnit4Assertions.assertTrue
import org.junit.jupiter.api.DisplayName

@DisplayName("Other plugins tests")
@OtherGradlePluginTests
class SubpuginsIT : KGPBaseTest() {

    @DisplayName("Subplugin example works as expected")
    @GradleTest
    fun testGradleSubplugin(gradleVersion: GradleVersion) {
        project("kotlinGradleSubplugin", gradleVersion) {
            build("compileKotlin", "build") {
                assertTasksExecuted(":compileKotlin")
                assertOutputContains("ExampleSubplugin loaded")
                assertOutputContains("Project component registration: exampleValue")
            }

            build("compileKotlin", "build") {
                assertTasksUpToDate(":compileKotlin")
                assertOutputContains("ExampleSubplugin loaded")
                assertOutputDoesNotContain("Project component registration: exampleValue")
            }
        }
    }

    @DisplayName("Allopen plugin opens classes and methods")
    @GradleTest
    fun testAllOpenPlugin(gradleVersion: GradleVersion) {
        project("allOpenSimple", gradleVersion) {
            build("assemble") {
                val classesDir = kotlinClassesDir()
                val openClass = classesDir.resolve("test/OpenClass.class")
                val closedClass = classesDir.resolve("test/ClosedClass.class")
                assertFileExists(openClass)
                assertFileExists(closedClass)

                checkBytecodeContains(
                    openClass.toFile(),
                    "public class test/OpenClass {",
                    "public method()V"
                )

                checkBytecodeContains(
                    closedClass.toFile(),
                    "public final class test/ClosedClass {",
                    "public final method()V"
                )
            }
        }
    }

    @DisplayName("Kotlin Spring plugin opens classes and methods")
    @GradleTest
    fun testKotlinSpringPlugin(gradleVersion: GradleVersion) {
        project("allOpenSpring", gradleVersion) {
            build("assemble") {

                val classesDir = kotlinClassesDir()
                val openClass = classesDir.resolve("test/OpenClass.class")
                val closedClass = classesDir.resolve("test/ClosedClass.class")

                assertFileExists(openClass)
                assertFileExists(closedClass)

                checkBytecodeContains(
                    openClass.toFile(),
                    "public class test/OpenClass {",
                    "public method()V"
                )

                checkBytecodeContains(
                    closedClass.toFile(),
                    "public final class test/ClosedClass {",
                    "public final method()V"
                )
            }
        }
    }

    @DisplayName("Jpa plugin generates no-arg constructor")
    @GradleTest
    fun testKotlinJpaPlugin(gradleVersion: GradleVersion) {
        project("noArgJpa", gradleVersion) {
            build("assemble") {
                val classesDir = kotlinClassesDir()

                fun checkClass(name: String) {
                    val testClass = classesDir.resolve("test/$name.class")
                    assertFileExists(testClass)
                    checkBytecodeContains(testClass.toFile(), "public <init>()V")
                }

                checkClass("Test")
                checkClass("Test2")
            }
        }
    }

    @DisplayName("NoArg: Don't invoke initializers by default")
    @GradleTest
    fun testNoArgKt18668(gradleVersion: GradleVersion) {
        project("noArgKt18668", gradleVersion) {
            build("assemble")
        }
    }

    @DisplayName("sam-with-receiver works")
    @GradleTest
    fun testSamWithReceiverSimple(gradleVersion: GradleVersion) {
        project("samWithReceiverSimple", gradleVersion) {
            build("assemble")
        }
    }

    @DisplayName("assignment works")
    @GradleTest
    fun testAssignmentSimple(gradleVersion: GradleVersion) {
        project("assignmentSimple", gradleVersion) {
            build("assemble")
        }
    }

    @DisplayName("Allopen plugin works when classpath dependency is not declared in current or root project ")
    @GradleTest
    fun testAllOpenFromNestedBuildscript(gradleVersion: GradleVersion) {
        project("allOpenFromNestedBuildscript", gradleVersion) {
            build("build") {
                val nestedSubproject = subProject("a/b")
                assertFileExists(nestedSubproject.kotlinClassesDir().resolve("MyClass.class"))
                assertFileExists(nestedSubproject.kotlinClassesDir("test").resolve("MyTestClass.class"))
            }
        }
    }

    @DisplayName("Allopen applied from script works")
    @GradleTest
    fun testAllopenFromScript(gradleVersion: GradleVersion) {
        project("allOpenFromScript", gradleVersion) {
            build("build") {
                assertFileExists(kotlinClassesDir().resolve("MyClass.class"))
                assertFileExists(kotlinClassesDir(sourceSet = "test").resolve("MyTestClass.class"))
            }
        }
    }

    @AndroidGradlePluginTests
    @DisplayName("KT-39809: kapt subplugin legacy loading does not fail the build")
    @AndroidTestVersions(minVersion = TestVersions.AGP.AGP_42)
    @GradleAndroidTest
    fun testKotlinVersionDowngradeInSupbrojectKt39809(
        gradleVersion: GradleVersion,
        agpVersion: String,
        providedJdk: JdkVersions.ProvidedJdk
    ) {
        project(
            "kapt2/android-dagger",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = providedJdk.location
        ) {
            subProject("app").buildGradle.modify {
                """
                buildscript {
                	repositories {
                		mavenCentral()
                	}
                	dependencies {
                		classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${TestVersions.Kotlin.STABLE_RELEASE}")
                	}
                }

                $it
                """.trimIndent()
            }

            build(":app:compileDebugKotlin")
        }
    }

    @DisplayName("Lombok plugin is working")
    @GradleTest
    fun testLombokPlugin(gradleVersion: GradleVersion) {
        project("lombokProject", gradleVersion) {
            build("build")
        }
    }

    @DisplayName("KT-47921: serialization plugin passed first to the compiler")
    @GradleTest
    fun testSerializationPluginOrderedFirst(gradleVersion: GradleVersion) {
        project("allOpenSimple", gradleVersion) {
            // Ensure that there are also allopen, noarg, and serialization plugins applied:
            buildGradle.modify {
                """
                |plugins {
                |    id "org.jetbrains.kotlin.plugin.noarg"
                |    id "org.jetbrains.kotlin.plugin.serialization"
                |${it.substringAfter("plugins {")}
                """.trimMargin()
            }

            build(
                "compileKotlin",
                buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)
            ) {
                val xPlugin = output
                    .split(" ")
                    .single { it.startsWith("-Xplugin") }
                    .substringAfter("-Xplugin")
                    .split(",")
                assertTrue(xPlugin.first().contains("serialization")) {
                    "Expected serialization plugin to go first; actual order: $xPlugin"
                }
            }
        }
    }

    @DisplayName("KT-51378: Using 'kotlin-dsl' with latest plugin version in buildSrc module")
    @GradleTestVersions(
        minVersion = TestVersions.Gradle.G_6_8 // Gradle usage of sam-with-receivers subplugin was added in this version
    )
    @GradleTest
    fun testBuildSrcKotlinDSL(gradleVersion: GradleVersion) {
        project("buildSrcUsingKotlinCompilationAndKotlinPlugin", gradleVersion) {
            subProject("buildSrc").buildGradleKts.modify {
                //language=kts
                """
                ${it.substringBefore("}")}
                }
                
                buildscript {
                    val kotlin_version: String by extra
                    repositories {
                        mavenLocal()
                    }
                    
                    dependencies {
                        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${'$'}kotlin_version")
                    }
                }
                
                ${it.substringAfter("}")}
                """.trimIndent()
            }

            build("assemble")
        }
    }
}
