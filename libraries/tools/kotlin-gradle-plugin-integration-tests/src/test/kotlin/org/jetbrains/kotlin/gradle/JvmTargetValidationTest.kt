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
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import java.io.File
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteExisting
import kotlin.io.path.writeText

@DisplayName("JVM tasks target validation")
@JvmGradlePluginTests
class JvmTargetValidationTest : KGPBaseTest() {

    @DisplayName("Should produce warning if java and kotlin jvm targets are different")
    @GradleTest
    internal fun shouldWarnIfJavaAndKotlinJvmTargetsAreDifferent(gradleVersion: GradleVersion) {
        project(
            projectName = "kotlinJavaProject".fullProjectName,
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.WARN)
        ) {
            setJavaCompilationCompatibility(JavaVersion.VERSION_1_8)
            useToolchainToCompile(11)

            build("assemble") {
                assertOutputContains(
                    "'compileJava' task (current target is 1.8) and 'compileKotlin' task (current target is 11) jvm target compatibility " +
                            "should be set to the same Java version."
                )
            }
        }
    }

    @DisplayName("Should fail the build if verification mode is 'error' and kotlin and java targets are different")
    @GradleTest
    internal fun shouldFailBuildIfJavaAndKotlinJvmTargetsAreDifferent(gradleVersion: GradleVersion) {
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
                kotlin.jvm.target.validation.mode = error
                """.trimIndent()
            )

            buildAndFail("assemble") {
                assertOutputContains(
                    "'compileJava' task (current target is 1.8) and 'compileKotlin' task (current target is 11) jvm target compatibility " +
                            "should be set to the same Java version."
                )
            }
        }
    }

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
                assertOutputDoesNotContain(
                    "'compileJava' task (current target is 1.8) and 'compileKotlin' task (current target is 11) jvm target compatibility " +
                            "should be set to the same Java version."
                )
            }
        }
    }

    @DisplayName("Should not produce warning when java and kotlin jvm targets are the same")
    @GradleTest
    internal fun shouldNotWarnOnJavaAndKotlinSameJvmTargets(gradleVersion: GradleVersion) {
        project(
            projectName = "kotlinJavaProject".fullProjectName,
            gradleVersion = gradleVersion
        ) {
            useToolchainToCompile(11)

            build("build") {
                assertOutputDoesNotContain(
                    "'compileJava' task (current target is 1.8) and 'compileKotlin' task (current target is 11) jvm target compatibility " +
                            "should be set to the same Java version."
                )
            }
        }
    }

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

            JavaVersion.VERSION_1_8
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
                assertOutputContains(
                    "'compileTestJava' task (current target is 1.8) and 'compileTestKotlin' task (current target is 11) jvm target " +
                            "compatibility should be set to the same Java version."
                )
                assertOutputDoesNotContain(
                    "'compileJava' task (current target is 1.8) and 'compileKotlin' task (current target is 11) jvm target compatibility " +
                            "should be set to the same Java version."
                )
            }
        }
    }

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
            //language=properties
            gradleProperties.append(
                """
                # suppress inspection "UnusedProperty"
                kotlin.jvm.target.validation.mode = error
                """.trimIndent()
            )

            build("build")
        }
    }

    @DisplayName("Should skip JVM target validation if no Kotlin sources are available")
    @GradleTest
    internal fun shouldSkipJvmTargetValidationNoKotlinSources(gradleVersion: GradleVersion) {
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
                kotlin.jvm.target.validation.mode = error
                """.trimIndent()
            )
            kotlinSourcesDir().toFile().deleteRecursively()
            javaSourcesDir().resolve("demo/HelloWorld.java").deleteExisting()

            build("assemble") {
                assertOutputDoesNotContain(
                    "'compileJava' task (current target is 1.8) and 'compileKotlin' task (current target is 11) jvm target compatibility " +
                            "should be set to the same Java version."
                )
            }

            javaSourcesDir().resolve("demo/Greeter.java").modify {
                it.replace("myGreeting = greeting;", """myGreeting = greeting + "!";""")
            }

            build("assemble") {
                assertOutputDoesNotContain(
                    "'compileJava' task (current target is 1.8) and 'compileKotlin' task (current target is 11) jvm target compatibility " +
                            "should be set to the same Java version."
                )
            }
        }
    }

    @DisplayName("Should skip JVM target validation if no java sources are available")
    @GradleTest
    internal fun shouldSkipJvmTargetValidationNoJavaSources(gradleVersion: GradleVersion) {
        project(
            projectName = "simple".fullProjectName,
            gradleVersion = gradleVersion,
            buildJdk = getJdk11().javaHome // should differ from default Kotlin jvm target value
        ) {
            //language=properties
            gradleProperties.append(
                """
                # suppress inspection "UnusedProperty"
                kotlin.jvm.target.validation.mode = error
                """.trimIndent()
            )

            build("assemble")
        }
    }

    @DisplayName("Should do JVM target validation if java sources are added and configuration cache is reused")
    @GradleTest
    internal fun shouldDoJvmTargetValidationOnNewJavaSourcesAndConfigurationCacheReuse(gradleVersion: GradleVersion) {
        project(
            projectName = "simple".fullProjectName,
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.withConfigurationCache,
            buildJdk = getJdk11().javaHome // should differ from default Kotlin jvm target value
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

            build("assemble") {
                assertOutputDoesNotContain(
                    "'compileJava' task (current target is 11) and 'compileKotlin' task (current target is 1.8) jvm target compatibility should be set to the same Java version."
                )
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

            build("assemble") {
                assertOutputContains(
                    "'compileJava' task (current target is 11) and 'compileKotlin' task (current target is 1.8) jvm target compatibility should be set to the same Java version."
                )
            }
        }
    }

    @DisplayName("Validation should work correctly for KaptGenerateStubs task")
    @GradleTestVersions
    @GradleTest
    internal fun kaptGenerateStubsValidateCorrect(gradleVersion: GradleVersion) {
        project(
            projectName = "kapt2/simple",
            gradleVersion = gradleVersion,
            buildJdk = getJdk11().javaHome
        ) {
            //language=properties
            gradleProperties.append(
                """
                # suppress inspection "UnusedProperty"
                kotlin.jvm.target.validation.mode = error
                """.trimIndent()
            )

            val toolchainJavaVersion = if (gradleVersion < GradleVersion.version("6.9")) {
                15
            } else {
                16
            }
            buildGradle.append(
                """
                
                kotlin {
                    jvmToolchain {
                        languageVersion.set(JavaLanguageVersion.of($toolchainJavaVersion))
                    }
                }
                """.trimIndent()
            )

            build("assemble")
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

    private val String.fullProjectName get() = "kotlin-java-toolchain/$this"
}
