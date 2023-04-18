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
import kotlin.io.path.appendText
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteExisting
import kotlin.io.path.writeText

@DisplayName("JVM tasks target validation")
@JvmGradlePluginTests
class JvmTargetValidationTest : KGPBaseTest() {

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
                assertOutputContains(
                    "'compileJava' task (current target is 1.8) and 'compileKotlin' task (current target is 11) jvm target compatibility " +
                            "should be set to the same Java version.\n" +
                            "By default will become an error since Gradle 8.0+! " +
                            "Read more: https://kotl.in/gradle/jvm/target-validation\n" +
                            "Consider using JVM toolchain: https://kotl.in/gradle/jvm/toolchain"
                )
            }
        }
    }

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
                assertOutputDoesNotContain(
                    "'compileJava' task (current target is 1.8) and 'compileKotlin' task (current target is 11) jvm target compatibility " +
                            "should be set to the same Java version.\n" +
                            "By default will become an error since Gradle 8.0+! " +
                            "Read more: https://kotl.in/gradle/jvm/target-validation\n" +
                            "Consider using JVM toolchain: https://kotl.in/gradle/jvm/toolchain"
                )
            }
        }
    }

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
                assertOutputContains(
                    "'compileJava' task (current target is 1.8) and 'compileKotlin' task (current target is 11) jvm target compatibility " +
                            "should be set to the same Java version.\n"
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

            gradleProperties.append(
                """
                kotlin.jvm.target.validation.mode = error
                """.trimIndent()
            )

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

            gradleProperties.append(
                """
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

            gradleProperties.append(
                """
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

    @DisplayName("Should still do JVM target validation if no java sources are available")
    @GradleTest
    internal fun shouldDoJvmTargetValidationOnNoJavaSources(gradleVersion: GradleVersion) {
        project(
            projectName = "simple".fullProjectName,
            gradleVersion = gradleVersion,
            buildJdk = getJdk11().javaHome // should differ from default Kotlin jvm target value
        ) {
            //language=properties
            gradleProperties.append(
                """
                # suppress inspection "UnusedProperty"
                kotlin.jvm.target.validation.mode = warning
                """.trimIndent()
            )

            build("assemble") {
                assertOutputContains(
                    "'compileJava' task (current target is 11) and 'compileKotlin' task (current target is 1.8) jvm target compatibility " +
                            "should be set to the same Java version.\n"
                )
            }
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
                assertOutputContains(
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
    @GradleTest
    internal fun kaptGenerateStubsValidateCorrect(gradleVersion: GradleVersion) {
        project(
            projectName = "kapt2/simple",
            gradleVersion = gradleVersion,
            buildJdk = getJdk11().javaHome
        ) {
            val toolchainJavaVersion = if (gradleVersion < GradleVersion.version("6.9")) {
                15
            } else {
                16
            }

            gradleProperties.append(
                """
                kotlin.jvm.target.validation.mode = error
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

            build("assemble", forceOutput = true)
        }
    }

    @DisplayName("Default value becomes 'error' with Gradle 8+")
    @GradleTestVersions(maxVersion = TestVersions.Gradle.G_8_0)
    @GradleTest
    internal fun errorByDefaultWithGradle8(gradleVersion: GradleVersion) {
        project("simple".fullProjectName, gradleVersion) {
            //language=Groovy
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
                    assertOutputContains(
                        "'compileJava' task (current target is 1.8) and 'compileKotlin' task (current target is 11) jvm target compatibility should be set to the same Java version."
                    )
                }
            } else {
                build("assemble") {
                    assertOutputContains(
                        "'compileJava' task (current target is 1.8) and 'compileKotlin' task (current target is 11) jvm target compatibility should be set to the same Java version."
                    )
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
                assertOutputContains("'compileJava' task (current target is 17) and 'compileKotlinJvmWithJava' task" +
                            " (current target is 1.8) jvm target compatibility should be set to the same Java version.")
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

    private val String.fullProjectName get() = "kotlin-java-toolchain/$this"
}