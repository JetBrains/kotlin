/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.checkBytecodeContains
import org.jetbrains.kotlin.testFederation.MustRunOnChangesInCompilerPlugins
import org.junit.jupiter.api.DisplayName

@DisplayName("Other plugins tests")
@MustRunOnChangesInCompilerPlugins
class SubpluginsIT : KGPBaseTest() {

    @OtherGradlePluginTests
    @DisplayName("Subplugin example works as expected")
    @GradleTest
    fun testGradleSubplugin(gradleVersion: GradleVersion) {
        project(
            "kotlinGradleSubplugin",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                configurationCache = BuildOptions.ConfigurationCacheValue.DISABLED,
                isolatedProjects = BuildOptions.IsolatedProjectsMode.DISABLED,
            ),
        ) {
            build("compileKotlin", "build") {
                assertTasksExecuted(":compileKotlin")
                assertOutputContains("ExampleSubplugin loaded")
            }

            build("compileKotlin", "build") {
                assertTasksUpToDate(":compileKotlin")
                assertOutputContains("ExampleSubplugin loaded")
            }
        }
    }

    @OtherGradlePluginTests
    @DisplayName("Subplugin example works as expected with configuration cache")
    @GradleTest
    fun testGradleSubpluginWithCC(gradleVersion: GradleVersion) {
        project(
            "kotlinGradleSubplugin",
            gradleVersion,
        ) {
            build("compileKotlin", "build") {
                assertTasksExecuted(":compileKotlin")
                assertOutputContains("ExampleSubplugin loaded")
            }

            build("compileKotlin", "build") {
                assertTasksUpToDate(":compileKotlin")
                assertConfigurationCacheReused()
                assertOutputDoesNotContain("ExampleSubplugin loaded")
            }
        }
    }

    @OtherGradlePluginTests
    @DisplayName("Allopen plugin opens classes and methods")
    @GradleWithCompilerVersionTest
    fun testAllOpenPlugin(gradleVersion: GradleVersion, compilerVersion: String) {
        project("allOpenSimple", gradleVersion, compilerVersion = compilerVersion) {
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

    @OtherGradlePluginTests
    @DisplayName("Kotlin Spring plugin opens classes and methods")
    @GradleWithCompilerVersionTest
    fun testKotlinSpringPlugin(gradleVersion: GradleVersion, compilerVersion: String) {
        project("allOpenSpring", gradleVersion, compilerVersion = compilerVersion) {
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

    @OtherGradlePluginTests
    @DisplayName("Jpa plugin generates no-arg constructor with open class")
    @GradleTest
    fun testKotlinJpaPlugin(gradleVersion: GradleVersion) {
        project("noArgJpa", gradleVersion) {
            build("assemble") {
                val classesDir = kotlinClassesDir()

                fun checkClass(name: String) {
                    val testClass = classesDir.resolve("test/$name.class")
                    assertFileExists(testClass)
                    checkBytecodeContains(testClass.toFile(), "public <init>()V")
                    checkBytecodeContains(testClass.toFile(), "public class test/$name {")
                }

                checkClass("Test")
                checkClass("Test2")
            }
        }
    }

    @OtherGradlePluginTests
    @DisplayName("NoArg: Don't invoke initializers by default")
    @GradleWithCompilerVersionTest
    fun testNoArgKt18668(gradleVersion: GradleVersion, compilerVersion: String) {
        project("noArgKt18668", gradleVersion, compilerVersion = compilerVersion) {
            build("assemble")
        }
    }

    @OtherGradlePluginTests
    @DisplayName("sam-with-receiver works")
    @GradleWithCompilerVersionTest
    fun testSamWithReceiverSimple(gradleVersion: GradleVersion, compilerVersion: String) {
        project("samWithReceiverSimple", gradleVersion, compilerVersion = compilerVersion) {
            build("assemble")
        }
    }

    @OtherGradlePluginTests
    @DisplayName("assignment works")
    @GradleWithCompilerVersionTest
    fun testAssignmentSimple(gradleVersion: GradleVersion, compilerVersion: String) {
        project("assignmentSimple", gradleVersion, compilerVersion = compilerVersion) {
            build("assemble")
        }
    }

    @OtherGradlePluginTests
    @DisplayName("Allopen plugin works when classpath dependency is not declared in current or root project ")
    @GradleWithCompilerVersionTest
    fun testAllOpenFromNestedBuildscript(gradleVersion: GradleVersion, compilerVersion: String) {
        project("allOpenFromNestedBuildscript", gradleVersion, compilerVersion = compilerVersion) {
            build("testClasses") {
                val nestedSubproject = subProject("a/b")
                assertFileExists(nestedSubproject.kotlinClassesDir().resolve("MyClass.class"))
                assertFileExists(nestedSubproject.kotlinClassesDir("test").resolve("MyTestClass.class"))
            }
        }
    }

    @OtherGradlePluginTests
    @DisplayName("Allopen applied from script works")
    @GradleWithCompilerVersionTest
    fun testAllopenFromScript(gradleVersion: GradleVersion, compilerVersion: String) {
        project("allOpenFromScript", gradleVersion, compilerVersion = compilerVersion) {
            build("testClasses") {
                assertFileExists(kotlinClassesDir().resolve("MyClass.class"))
                assertFileExists(kotlinClassesDir(sourceSet = "test").resolve("MyTestClass.class"))
            }
        }
    }

    @AndroidGradlePluginTests
    @DisplayName("KT-39809: kapt subplugin legacy loading does not fail the build")
    @GradleAndroidTest
    @AndroidTestVersions(maxVersion = TestVersions.AGP.AGP_813)
    fun testKotlinVersionDowngradeInSupbrojectKt39809(
        gradleVersion: GradleVersion,
        agpVersion: String,
        providedJdk: JdkVersions.ProvidedJdk
    ) {
        project(
            "kapt/android-dagger",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion).suppressAgpWarningIsProperty(gradleVersion),
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

    @OtherGradlePluginTests
    @DisplayName("Lombok plugin is working")
    @GradleWithCompilerVersionTest
    fun testLombokPlugin(gradleVersion: GradleVersion, compilerVersion: String) {
        project("lombokProject", gradleVersion, compilerVersion = compilerVersion) {
            listOf(
                subProject("yeskapt").buildGradle,
                subProject("nokapt").buildGradle,
                subProject("withconfig").buildGradle
            ).forEach { buildGradle ->
                buildGradle.modify {
                    val freefairLombokVersion = "8.4"
                    it.replace("<freefair_lombok_version>", freefairLombokVersion)
                }
            }
            if (compilerVersion < "2.5") {
                listOf(subProject("yeskapt").buildGradle, subProject("withconfig").buildGradle).forEach {
                    it.append(
                        """
                        kapt {
                            detectMemoryLeaks = "default"
                        }
                        """.trimIndent()
                    )
                }
            }
            build("build")
        }
    }

    @OtherGradlePluginTests
    @DisplayName("KT-51378: Using 'kotlin-dsl' with latest plugin version in buildSrc module")
    @GradleWithCompilerVersionTest
    fun testBuildSrcKotlinDSL(gradleVersion: GradleVersion, compilerVersion: String) {
        val languageVersionForBuildSrc = KotlinVersion.firstNonDeprecated.name
        project("buildSrcUsingKotlinCompilationAndKotlinPlugin", gradleVersion, compilerVersion = compilerVersion) {
            subProject("buildSrc").buildGradleKts.modify {
                //language=kts
                """
                ${it.substringBefore("}")}
                }
                
                buildscript {
                    val kotlin_version = extra["kotlin_version"]
                    repositories {
                        mavenLocal()
                    }
                    
                    dependencies {
                        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${'$'}kotlin_version")
                    }
                }
                
                afterEvaluate {
                    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
                        // aligned with embedded Kotlin compiler: https://docs.gradle.org/current/userguide/compatibility.html#kotlin
                        // the hardcoded values are fine as this block (and the test) are checking some old Gradle functionality
                        compilerOptions.apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.$languageVersionForBuildSrc)
                        compilerOptions.languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.$languageVersionForBuildSrc)
                    }
                }
                
                ${it.substringAfter("}")}
                """.trimIndent()
            }

            build("assemble")
        }
    }
}
