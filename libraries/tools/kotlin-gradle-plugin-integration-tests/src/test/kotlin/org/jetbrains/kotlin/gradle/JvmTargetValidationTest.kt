/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.JavaVersion
import org.gradle.api.logging.LogLevel
import org.gradle.internal.jvm.JavaInfo
import org.gradle.internal.jvm.Jvm
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import java.io.File
import kotlin.io.path.appendText
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteExisting
import kotlin.io.path.writeText

@DisplayName("JVM tasks target validation")
class JvmTargetValidationTest : KGPBaseTest() {

    @JvmGradlePluginTests
    @DisplayName("Should produce error if java and kotlin jvm targets are different")
    @GradleTest
    internal fun shouldFailIfJavaAndKotlinJvmTargetsAreDifferent(gradleVersion: GradleVersion) {
        project(
            projectName = "kotlinJavaProject".fullProjectName,
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.WARN)
        ) {
            setJavaCompilationCompatibility(JavaVersion.VERSION_1_8)
            useToolchainToCompile(11)

            gradleProperties.append(
                """
                kotlin.jvm.target.validation.mode = error
                """.trimIndent()
            )

            buildAndFail("assemble") {
                assertHasDiagnostic(KotlinToolingDiagnostics.InconsistentTargetCompatibilityForKotlinAndJavaTasks)
            }
        }
    }

    @JvmGradlePluginTests
    @DisplayName("Should allow to override validation mode for specific task")
    @GradleTest
    internal fun overrideModeForTask(gradleVersion: GradleVersion) {
        project(
            projectName = "kotlinJavaProject".fullProjectName,
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.WARN)
        ) {
            setJavaCompilationCompatibility(JavaVersion.VERSION_1_8)
            useToolchainToCompile(11)

            gradleProperties.append(
                """
                kotlin.jvm.target.validation.mode = error
                """.trimIndent()
            )

            buildGradle.appendText(
                //language=groovy
                """
                |
                |tasks.named("compileKotlin", org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile.class) {
                |    jvmTargetValidationMode.set(org.jetbrains.kotlin.gradle.dsl.jvm.JvmTargetValidationMode.IGNORE)
                |}
                """.trimMargin()
            )

            build("assemble") {
                assertNoDiagnostic(KotlinToolingDiagnostics.InconsistentTargetCompatibilityForKotlinAndJavaTasks)
            }
        }
    }

    @JvmGradlePluginTests
    @DisplayName("Should warn in the build log if verification mode is 'warning' and kotlin and java targets are different")
    @GradleTest
    internal fun shouldWarnBuildIfJavaAndKotlinJvmTargetsAreDifferent(gradleVersion: GradleVersion) {
        project(
            projectName = "kotlinJavaProject".fullProjectName,
            gradleVersion = gradleVersion
        ) {
            setJavaCompilationCompatibility(JavaVersion.VERSION_1_8)
            useToolchainToCompile(11)
            //language=properties
            gradleProperties.append(
                """
                # suppress inspection "UnusedProperty"
                kotlin.jvm.target.validation.mode = warning
                """.trimIndent()
            )

            build("assemble") {
                assertHasDiagnostic(KotlinToolingDiagnostics.InconsistentTargetCompatibilityForKotlinAndJavaTasks)
            }
        }
    }

    @JvmGradlePluginTests
    @DisplayName("Should ignore if verification mode is 'ignore' and kotlin and java targets are different")
    @GradleTest
    internal fun shouldNotPrintAnythingIfJavaAndKotlinJvmTargetsAreDifferent(
        gradleVersion: GradleVersion
    ) {
        project(
            projectName = "kotlinJavaProject".fullProjectName,
            gradleVersion = gradleVersion
        ) {
            setJavaCompilationCompatibility(JavaVersion.VERSION_1_8)
            useToolchainToCompile(11)
            //language=properties
            gradleProperties.append(
                """
                # suppress inspection "UnusedProperty"
                kotlin.jvm.target.validation.mode = ignore
                """.trimIndent()
            )

            build("assemble") {
                assertNoDiagnostic(KotlinToolingDiagnostics.InconsistentTargetCompatibilityForKotlinAndJavaTasks)
            }
        }
    }

    @JvmGradlePluginTests
    @DisplayName("Should not produce warning when java and kotlin jvm targets are the same")
    @GradleTest
    internal fun shouldNotWarnOnJavaAndKotlinSameJvmTargets(gradleVersion: GradleVersion) {
        project(
            projectName = "kotlinJavaProject".fullProjectName,
            gradleVersion = gradleVersion
        ) {
            useToolchainToCompile(11)

            gradleProperties.append(
                """
                kotlin.jvm.target.validation.mode = error
                """.trimIndent()
            )

            build("build") {
                assertNoDiagnostic(KotlinToolingDiagnostics.InconsistentTargetCompatibilityForKotlinAndJavaTasks)
            }
        }
    }

    @JvmGradlePluginTests
    @DisplayName("Should produce Java-Kotlin jvm target incompatibility warning only for related tasks")
    @GradleTest
    internal fun shouldProduceJavaKotlinJvmTargetDifferenceWarningOnlyForRelatedTasks(
        gradleVersion: GradleVersion
    ) {
        project(
            projectName = "kotlinJavaProject".fullProjectName,
            gradleVersion = gradleVersion
        ) {
            useToolchainToCompile(11)

            //language=properties
            gradleProperties.append(
                """
                # suppress inspection "UnusedProperty"
                kotlin.jvm.target.validation.mode = warning
                """.trimIndent()
            )

            //language=Groovy
            buildGradle.append(
                """
                
                tasks
                .matching {
                    it instanceof JavaCompile && it.name == "compileTestJava"
                }
                .configureEach {
                    sourceCompatibility = JavaVersion.VERSION_1_8
                    targetCompatibility = JavaVersion.VERSION_1_8
                }
                
                """.trimIndent()
            )

            build("build") {
                assertHasDiagnostic(
                    KotlinToolingDiagnostics.InconsistentTargetCompatibilityForKotlinAndJavaTasks,
                    withSubstring = "compileTestKotlin"
                )
                assertNoDiagnostic(
                    KotlinToolingDiagnostics.InconsistentTargetCompatibilityForKotlinAndJavaTasks,
                    withSubstring = "compileKotlin"
                )
            }
        }
    }

    @JvmGradlePluginTests
    @DisplayName("Should correctly validate JVM targets in mixed Kotlin/Java projects that are using <JDK1.8")
    @GradleTest
    internal fun oldJdkMixedJavaKotlinTargetVerification(gradleVersion: GradleVersion) {
        project(
            projectName = "kotlinJavaProject".fullProjectName,
            gradleVersion = gradleVersion
        ) {
            //language=groovy
            buildGradle.append(
                """
                
                java {
                    toolchain {
                        languageVersion.set(JavaLanguageVersion.of(8))
                    }
                }
                
                """.trimIndent()
            )

            gradleProperties.append(
                """
                kotlin.jvm.target.validation.mode = error
                """.trimIndent()
            )

            build("build")
        }
    }

    @JvmGradlePluginTests
    @DisplayName("Should skip JVM target validation if no Kotlin sources are available")
    @GradleTest
    internal fun shouldSkipJvmTargetValidationNoKotlinSources(gradleVersion: GradleVersion) {
        project(
            projectName = "kotlinJavaProject".fullProjectName,
            gradleVersion = gradleVersion
        ) {
            setJavaCompilationCompatibility(JavaVersion.VERSION_1_8)
            useToolchainToCompile(11)

            gradleProperties.append(
                """
                kotlin.jvm.target.validation.mode = error
                """.trimIndent()
            )

            kotlinSourcesDir().toFile().deleteRecursively()
            javaSourcesDir().resolve("demo/HelloWorld.java").deleteExisting()

            build("assemble") {
                assertNoDiagnostic(KotlinToolingDiagnostics.InconsistentTargetCompatibilityForKotlinAndJavaTasks)
            }

            javaSourcesDir().resolve("demo/Greeter.java").modify {
                it.replace("myGreeting = greeting;", """myGreeting = greeting + "!";""")
            }

            build("assemble") {
                assertNoDiagnostic(KotlinToolingDiagnostics.InconsistentTargetCompatibilityForKotlinAndJavaTasks)
            }
        }
    }

    @JvmGradlePluginTests
    @DisplayName("Should still do JVM target validation if no java sources are available")
    @GradleTest
    internal fun shouldDoJvmTargetValidationOnNoJavaSources(gradleVersion: GradleVersion) {
        project(
            projectName = "simple".fullProjectName,
            gradleVersion = gradleVersion,
        ) {
            //language=properties
            gradleProperties.append(
                """
                # suppress inspection "UnusedProperty"
                kotlin.jvm.target.validation.mode = warning
                """.trimIndent()
            )

            buildGradle.appendText(
                """
                |
                |kotlin.jvmToolchain(11)
                |kotlin.compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
                """.trimMargin()
            )

            build(":compileKotlin") {
                assertHasDiagnostic(KotlinToolingDiagnostics.InconsistentTargetCompatibilityForKotlinAndJavaTasks)
            }
        }
    }

    @JvmGradlePluginTests
    @DisplayName("Should do JVM target validation if java sources are added and configuration cache is reused")
    @GradleTest
    internal fun shouldDoJvmTargetValidationOnNewJavaSourcesAndConfigurationCacheReuse(gradleVersion: GradleVersion) {
        project(
            projectName = "simple".fullProjectName,
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.withConfigurationCache
        ) {
            // Validation mode should be 'warning' because of https://github.com/gradle/gradle/issues/9339
            // which is fixed in Gradle 7.2
            //language=properties
            gradleProperties.append(
                """
                # suppress inspection "UnusedProperty"
                kotlin.jvm.target.validation.mode = warning
                """.trimIndent()
            )

            buildGradle.appendText(
                """
                |
                |kotlin.jvmToolchain(11)
                |kotlin.compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
                """.trimMargin()
            )

            build(":compileKotlin") {
                assertHasDiagnostic(KotlinToolingDiagnostics.InconsistentTargetCompatibilityForKotlinAndJavaTasks)
            }

            javaSourcesDir().resolve("demo").run {
                createDirectories()
                resolve("HelloWorld.java").writeText(
                    //language=Java
                    """
                    package demo;

                    public class HelloWorld {
                        public static void main(String[] args) {
                            System.out.println("Hello world!");
                        }
                    }
                    """.trimIndent()
                )
            }

            build(":compileKotlin") {
                assertHasDiagnostic(KotlinToolingDiagnostics.InconsistentTargetCompatibilityForKotlinAndJavaTasks)
            }
        }
    }

    @OtherGradlePluginTests
    @DisplayName("Validation should work correctly for KaptGenerateStubs task")
    @GradleTest
    internal fun kaptGenerateStubsValidateCorrect(gradleVersion: GradleVersion) {
        project(
            projectName = "kapt2/simple",
            gradleVersion = gradleVersion,
        ) {
            val toolchainJavaVersion = if (gradleVersion < GradleVersion.version("6.9")) 11 else 17

            gradleProperties.append(
                """
                kotlin.jvm.target.validation.mode = error
                org.gradle.java.installations.paths=${getJdk17().javaHome},${getJdk11().javaHome}
                """.trimIndent()
            )

            buildGradle.append(
                """
                
                kotlin {
                    jvmToolchain {
                        languageVersion.set(JavaLanguageVersion.of($toolchainJavaVersion))
                    }
                }
                """.trimIndent()
            )

            build(":kaptGenerateStubsKotlin")
        }
    }

    @JvmGradlePluginTests
    @DisplayName("Default value becomes 'error' with Gradle 8+")
    @GradleTestVersions(maxVersion = TestVersions.Gradle.G_8_0)
    @GradleTest
    internal fun errorByDefaultWithGradle8(gradleVersion: GradleVersion) {
        project("simple".fullProjectName, gradleVersion) {
            //language=Groovy
            @Suppress("UnnecessaryQualifiedReference")
            buildGradle.appendText(
                """
                |
                |tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile.class).configureEach {
                |    compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
                |}
                """.trimMargin()
            )

            if (gradleVersion.baseVersion >= GradleVersion.version("8.0")) {
                buildAndFail("assemble") {
                    assertHasDiagnostic(KotlinToolingDiagnostics.InconsistentTargetCompatibilityForKotlinAndJavaTasks)
                }
            } else {
                build("assemble") {
                    assertHasDiagnostic(KotlinToolingDiagnostics.InconsistentTargetCompatibilityForKotlinAndJavaTasks)
                }
            }
        }
    }

    @MppGradlePluginTests
    @DisplayName("Validation should show error for MPP JVM target withJava")
    @GradleTest
    internal fun mppWithJavaFailValidation(gradleVersion: GradleVersion) {
        project("kt-31468-multiple-jvm-targets-with-java", gradleVersion) {
            gradleProperties.append(
                """
                kotlin.jvm.target.validation.mode = error
                """.trimIndent()
            )

            subProject("lib").buildGradleKts.appendText(
                """
                |
                |java {
                |    targetCompatibility = JavaVersion.VERSION_17
                |    sourceCompatibility = JavaVersion.VERSION_17
                |}
                """.trimMargin()
            )

            buildAndFail(":lib:compileKotlinJvmWithJava") {
                assertHasDiagnostic(KotlinToolingDiagnostics.InconsistentTargetCompatibilityForKotlinAndJavaTasks)
            }
        }
    }

    @MppGradlePluginTests
    @DisplayName("Validation should not run MPP JVM target without withJava")
    @GradleTest
    internal fun mppJvmNotFailValidation(gradleVersion: GradleVersion) {
        project("kt-31468-multiple-jvm-targets-with-java", gradleVersion) {
            gradleProperties.append(
                """
                kotlin.jvm.target.validation.mode = error
                """.trimIndent()
            )

            subProject("lib").buildGradleKts.appendText(
                """
                |
                |java {
                |    targetCompatibility = JavaVersion.VERSION_17
                |    sourceCompatibility = JavaVersion.VERSION_17
                |}
                """.trimMargin()
            )

            build(":lib:compileKotlinPlainJvm")
        }
    }

    private fun TestProject.setJavaCompilationCompatibility(
        target: JavaVersion
    ) {
        //language=Groovy
        buildGradle.append(
            """

            tasks.withType(JavaCompile.class).configureEach {
                sourceCompatibility = JavaVersion.${target.name}
                targetCompatibility = JavaVersion.${target.name}
            }
            
            """.trimIndent()
        )
    }

    private fun TestProject.useToolchainToCompile(
        jdkVersion: Int
    ) {
        //language=Groovy
        buildGradle.append(
            """
            import org.jetbrains.kotlin.gradle.tasks.UsesKotlinJavaToolchain
            
            def toolchain = project.extensions.getByType(JavaPluginExtension.class).toolchain
            toolchain.languageVersion.set(JavaLanguageVersion.of($jdkVersion))
            def service = project.extensions.getByType(JavaToolchainService.class)
            def defaultLauncher = service.launcherFor(toolchain)
                    
            project.tasks
                 .withType(UsesKotlinJavaToolchain.class)
                 .configureEach {
                      it.kotlinJavaToolchain.toolchain.use(
                          defaultLauncher
                      )
                 }

            """.trimIndent()
        )
    }

    private fun getJdk11(): JavaInfo = Jvm.forHome(File(System.getProperty("jdk11Home")))

    private fun getJdk17(): JavaInfo = Jvm.forHome(File(System.getProperty("jdk17Home")))

    private val String.fullProjectName get() = "kotlin-java-toolchain/$this"
}
