/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.JavaVersion
import org.gradle.api.logging.LogLevel
import org.gradle.internal.jvm.JavaInfo
import org.gradle.internal.jvm.Jvm
import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import java.io.File

@JvmGradlePluginTests
@DisplayName("Kotlin Java Toolchain support")
class KotlinJavaToolchainTest : KGPBaseTest() {

    @GradleTestVersions
    @GradleTest
    @DisplayName("Should use by default same jvm as Gradle daemon for jdkHome")
    internal fun byDefaultShouldUseGradleJDK(gradleVersion: GradleVersion) {
        project(
            projectName = "simple".fullProjectName,
            gradleVersion = gradleVersion
        ) {
            build("assemble") {
                assertJdkHomeIsUsingJdk(getUserJdk().javaHomeRealPath)
            }
        }
    }

    @GradleTest
    @DisplayName("Should use provided jdk location to compile Kotlin sources")
    internal fun customJdkHomeLocation(gradleVersion: GradleVersion) {
        project(
            projectName = "simple".fullProjectName,
            gradleVersion = gradleVersion,
        ) {
            setJavaCompilationCompatibility(JavaVersion.VERSION_1_9)

            useJdkToCompile(
                getJdk9Path(),
                JavaVersion.VERSION_1_9
            )

            build("assemble") {
                assertJdkHomeIsUsingJdk(getJdk9().javaHomeRealPath)
            }
        }
    }

    @GradleTest
    @DisplayName("KotlinCompile task should use build cache when using provided JDK")
    internal fun customJdkBuildCache(gradleVersion: GradleVersion) {
        val buildCache = workingDir.resolve("custom-jdk-build-cache")
        project(
            projectName = "simple".fullProjectName,
            gradleVersion = gradleVersion,
            projectPathAdditionalSuffix = "1/cache-test",
            buildOptions = defaultBuildOptions.copy(buildCacheEnabled = true)
        ) {
            enableLocalBuildCache(buildCache)
            if (shouldUseToolchain(gradleVersion)) {
                useToolchainExtension(11)
            } else {
                setJavaCompilationCompatibility(JavaVersion.VERSION_1_9)
                useJdkToCompile(
                    getJdk9Path(),
                    JavaVersion.VERSION_1_9
                )
            }

            build("assemble")
        }

        project(
            projectName = "simple".fullProjectName,
            gradleVersion = gradleVersion,
            projectPathAdditionalSuffix = "2/cache-test",
            buildOptions = defaultBuildOptions.copy(buildCacheEnabled = true)
        ) {
            enableLocalBuildCache(buildCache)
            if (shouldUseToolchain(gradleVersion)) {
                useToolchainExtension(11)
            } else {
                setJavaCompilationCompatibility(JavaVersion.VERSION_1_9)
                useJdkToCompile(
                    getJdk9Path(),
                    JavaVersion.VERSION_1_9
                )
            }

            build("assemble") {
                assertTasksFromCache(":compileKotlin")
            }
        }
    }

    @DisplayName("Kotlin compile task should reuse build cache when toolchain is set and build is happening on different JDKs")
    @GradleTestVersions
    @GradleTest
    internal fun differentBuildJDKBuildCacheHit(gradleVersion: GradleVersion) {
        val buildCache = workingDir.resolve("custom-jdk-build-cache")
        project(
            projectName = "simple".fullProjectName,
            gradleVersion = gradleVersion,
            projectPathAdditionalSuffix = "1/cache-test",
            buildOptions = defaultBuildOptions.copy(buildCacheEnabled = true),
            buildJdk = getJdk9().javaHome!!
        ) {
            enableLocalBuildCache(buildCache)
            useToolchainExtension(11)

            build("assemble")
        }

        project(
            projectName = "simple".fullProjectName,
            gradleVersion = gradleVersion,
            projectPathAdditionalSuffix = "2/cache-test",
            buildOptions = defaultBuildOptions.copy(buildCacheEnabled = true),
            buildJdk = getJdk11().javaHome!!
        ) {
            enableLocalBuildCache(buildCache)
            useToolchainExtension(11)

            build("assemble") {
                assertTasksFromCache(":compileKotlin")
            }
        }
    }

    @GradleTest
    @DisplayName("Kotlin compile task should not use build cache on using different JDK versions")
    internal fun differentJdkBuildCacheMiss(gradleVersion: GradleVersion) {
        val buildCache = workingDir.resolve("custom-jdk-build-cache")
        project(
            projectName = "simple".fullProjectName,
            gradleVersion = gradleVersion,
            projectPathAdditionalSuffix = "1/cache-test",
            buildOptions = defaultBuildOptions.copy(buildCacheEnabled = true)
        ) {
            enableLocalBuildCache(buildCache)
            if (shouldUseToolchain(gradleVersion)) {
                useToolchainExtension(11)
            } else {
                setJavaCompilationCompatibility(JavaVersion.VERSION_1_9)
                useJdkToCompile(
                    getJdk9Path(),
                    JavaVersion.VERSION_1_9
                )
            }
            build("assemble")
        }

        project(
            projectName = "simple".fullProjectName,
            gradleVersion = gradleVersion,
            projectPathAdditionalSuffix = "2/cache-test",
            buildOptions = defaultBuildOptions.copy(buildCacheEnabled = true)
        ) {
            enableLocalBuildCache(buildCache)
            build("assemble") {
                assertTasksExecuted(":compileKotlin")
            }
        }
    }

    @DisplayName("Kapt task should use only process worker isolation when kotlin java toolchain is set")
    @GradleTest
    internal fun kaptTasksShouldUseProcessWorkersIsolation(gradleVersion: GradleVersion) {
        project(
            projectName = "simpleWithKapt".fullProjectName,
            gradleVersion = gradleVersion,
        ) {
            if (shouldUseToolchain(gradleVersion)) {
                useToolchainExtension(11)
            } else {
                useJdkToCompile(
                    getJdk9Path(),
                    JavaVersion.VERSION_1_9
                )
            }
            gradleProperties.append(
                "kapt.workers.isolation = none"
            )

            build("assemble") {
                assertJdkHomeIsUsingJdk(
                    if (shouldUseToolchain(gradleVersion)) {
                        getToolchainExecPathFromLogs()
                    } else {
                        getJdk9().javaHomeRealPath
                    }
                )

                assertOutputContains("Using workers PROCESS isolation mode to run kapt")
                assertOutputContains("Using non-default Kotlin java toolchain - 'kapt.workers.isolation == none' property is ignored!")
            }
        }
    }

    @DisplayName("Kapt task should use worker no-isolation mode when build is using Gradle JDK")
    @GradleTest
    internal fun kaptTasksShouldUseNoIsolationModeOnDefaultJvm(gradleVersion: GradleVersion) {
        project(
            projectName = "simpleWithKapt".fullProjectName,
            gradleVersion = gradleVersion,
        ) {
            build("assemble") {
                assertOutputContains("Using workers NONE isolation mode to run kapt")
                assertOutputDoesNotContain("Using non-default Kotlin java toolchain - 'kapt.workers.isolation == none' property is ignored!")
            }
        }
    }

    @DisplayName("Kapt tasks with custom JDK should be cacheable")
    @GradleTest
    internal fun kaptTasksWithCustomJdkCacheable(gradleVersion: GradleVersion) {
        val buildCache = workingDir.resolve("custom-jdk-build-cache")
        project(
            projectName = "simpleWithKapt".fullProjectName,
            gradleVersion = gradleVersion,
            projectPathAdditionalSuffix = "1/cache-test",
            buildOptions = defaultBuildOptions.copy(buildCacheEnabled = true)
        ) {
            enableLocalBuildCache(buildCache)
            if (shouldUseToolchain(gradleVersion)) {
                useToolchainExtension(11)
            } else {
                useJdkToCompile(
                    getJdk9Path(),
                    JavaVersion.VERSION_1_9
                )
            }

            build("assemble")
        }

        project(
            projectName = "simpleWithKapt".fullProjectName,
            gradleVersion = gradleVersion,
            projectPathAdditionalSuffix = "2/cache-test",
            buildOptions = defaultBuildOptions.copy(buildCacheEnabled = true),
        ) {
            enableLocalBuildCache(buildCache)
            if (shouldUseToolchain(gradleVersion)) {
                useToolchainExtension(11)
            } else {
                useJdkToCompile(
                    getJdk9Path(),
                    JavaVersion.VERSION_1_9
                )
            }

            build("assemble") {
                assertTasksFromCache(
                    ":kaptGenerateStubsKotlin",
                    ":kaptKotlin",
                    ":compileKotlin"
                )
            }
        }
    }

    @DisplayName("Kapt tasks with default JDK and different isolation modes should be cacheable")
    @GradleTest
    internal fun kaptCacheableOnSwitchingIsolationModeAndDefaultJDK(gradleVersion: GradleVersion) {
        val buildCache = workingDir.resolve("custom-jdk-build-cache")
        project(
            projectName = "simpleWithKapt".fullProjectName,
            gradleVersion = gradleVersion,
            projectPathAdditionalSuffix = "1/cache-test",
            buildOptions = defaultBuildOptions.copy(buildCacheEnabled = true)
        ) {
            enableLocalBuildCache(buildCache)

            build("assemble")
        }

        project(
            projectName = "simpleWithKapt".fullProjectName,
            gradleVersion = gradleVersion,
            projectPathAdditionalSuffix = "2/cache-test",
            buildOptions = defaultBuildOptions.copy(buildCacheEnabled = true),
        ) {
            enableLocalBuildCache(buildCache)
            gradleProperties.append(
                "kapt.workers.isolation = process"
            )

            build("assemble") {
                assertTasksFromCache(
                    ":kaptGenerateStubsKotlin",
                    ":kaptKotlin",
                    ":compileKotlin"
                )
            }
        }
    }

    @DisplayName("Should allow to set JDK version for tasks via Java toolchain")
    @GradleTestVersions
    @GradleTest
    internal fun setJdkUsingJavaToolchain(gradleVersion: GradleVersion) {
        project(
            projectName = "simple".fullProjectName,
            gradleVersion = gradleVersion
        ) {
            useToolchainToCompile(11)
            build("assemble") {
                assertJdkHomeIsUsingJdk(getToolchainExecPathFromLogs())
            }
        }
    }

    @DisplayName("Should allow to set Java toolchain via extension")
    @GradleTestVersions
    @GradleTest
    internal fun setJdkUsingJavaToolchainViaExtension(gradleVersion: GradleVersion) {
        project(
            projectName = "simple".fullProjectName,
            gradleVersion = gradleVersion
        ) {
            useToolchainExtension(11)
            build("assemble") {
                assertJdkHomeIsUsingJdk(getToolchainExecPathFromLogs())
            }
        }
    }

    @DisplayName("Toolchain should be correctly supported in multiplatform plugin jvm targets")
    @GradleTestVersions
    @GradleTest
    internal fun toolchainCorrectlySupportedInMPPlugin(gradleVersion: GradleVersion) {
        project(
            projectName = "mppJvmWithJava".fullProjectName,
            gradleVersion = gradleVersion
        ) {
            useToolchainToCompile(11)

            build("assemble") {
                assertJdkHomeIsUsingJdk(getToolchainExecPathFromLogs())
            }
        }
    }

    @DisplayName("Should set 'jvmTarget' option if user does not specify it explicitly via jdk setter")
    @GradleTest
    internal fun shouldSetJvmTargetNonSpecifiedByUserViaSetJdk(gradleVersion: GradleVersion) {
        project(
            projectName = "simple".fullProjectName,
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)
        ) {
            useJdkToCompile(
                getJdk11Path(),
                JavaVersion.VERSION_11
            )

            build("build") {
                assertOutputContains("-jvm-target 11")
                assertOutputDoesNotContain("-jvm-target 1.8")
            }
        }
    }

    @DisplayName("Should not override user specified 'jvmTarget' option via jdk setter")
    @GradleTest
    internal fun shouldNotOverrideUserJvmTargetViaSetJDK(gradleVersion: GradleVersion) {
        project(
            projectName = "simple".fullProjectName,
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)
        ) {
            setJvmTarget("1.8")

            useJdkToCompile(
                getJdk11Path(),
                JavaVersion.VERSION_11
            )

            build("build") {
                assertOutputContains("-jvm-target 1.8")
                assertOutputDoesNotContain("-jvm-target 11")
            }
        }
    }

    @DisplayName("Should set 'jvmTarget' option if user does not specify it explicitly via toolchain setter")
    @GradleTestVersions
    @GradleTest
    internal fun shouldSetJvmTargetNonSpecifiedByUserViaToolchain(gradleVersion: GradleVersion) {
        project(
            projectName = "simple".fullProjectName,
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)
        ) {
            useToolchainToCompile(11)

            build("build") {
                assertOutputContains("-jvm-target 11")
                assertOutputDoesNotContain("-jvm-target 1.8")
            }
        }
    }

    @DisplayName("Should not override user specified 'jvmTarget' option via toolchain setter")
    @GradleTestVersions
    @GradleTest
    internal fun shouldNotOverrideUserSpecifiedJvmTargetViaToolchain(gradleVersion: GradleVersion) {
        project(
            projectName = "simple".fullProjectName,
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)
        ) {
            setJvmTarget("1.8")
            useToolchainToCompile(11)

            build("build") {
                assertOutputContains("-jvm-target 1.8")
                assertOutputDoesNotContain("-jvm-target 11")
            }
        }
    }

    @DisplayName("Setting toolchain via java extension should also affect Kotlin compilations")
    @GradleTestVersions
    @GradleTest
    internal fun settingToolchainViaJavaShouldAlsoWork(gradleVersion: GradleVersion) {
        project(
            projectName = "kotlinJavaProject".fullProjectName,
            gradleVersion = gradleVersion
        ) {
            //language=groovy
            buildGradle.append(
                """
                
                java {
                    toolchain {
                        languageVersion.set(JavaLanguageVersion.of(11))
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

    @DisplayName("Build should not produce warnings when '-no-jdk' option is present")
    @GradleTestVersions
    @GradleTest
    internal fun noWarningOnNoJdkOptionPresent(gradleVersion: GradleVersion) {
        project(
            projectName = "simple".fullProjectName,
            gradleVersion = gradleVersion
        ) {
            useToolchainToCompile(11)

            //language=groovy
            buildGradle.append(
                """
                
                import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
                
                tasks
                    .withType(KotlinCompile.class)
                    .configureEach {
                        kotlinOptions {
                            noJdk = true
                        }                
                    }
                """.trimIndent()
            )

            build("build") {
                assertOutputDoesNotContain("w: The '-jdk-home' option is ignored because '-no-jdk' is specified")
            }
        }
    }

    @DisplayName("Toolchain should take into account kotlin options that are set via android extension")
    @GradleTestVersions
    @GradleTest
    internal fun kotlinOptionsAndroidAndToolchain(gradleVersion: GradleVersion) {
        project("android".fullProjectName, gradleVersion) {
            useToolchainExtension(11)

            //language=properties
            gradleProperties.append(
                """
                # suppress inspection "UnusedProperty"
                kotlin.jvm.target.validation.mode = error
                """.trimIndent()
            )

            build(
                "assembleDebug",
                buildOptions = defaultBuildOptions.copy(
                    androidVersion = TestVersions.AGP.AGP_42
                )
            )
        }
    }

    @DisplayName("Toolchain should not make an exception when build is running on JDK 11, but toolchain is set to JDK 1.8")
    @GradleTestVersions
    @GradleTest
    internal fun shouldNotRaiseErrorOnJDK11withJDK1_8Toolchain(gradleVersion: GradleVersion) {
        project(
            projectName = "simple".fullProjectName,
            gradleVersion = gradleVersion,
            buildJdk = getJdk11().javaHome
        ) {
            useToolchainExtension(8)

            build("assemble")
        }
    }

    @DisplayName("JVM target shouldn't be changed when toolchain is not configured")
    @GradleTestVersions
    @GradleTest
    internal fun shouldNotChangeJvmTargetWithNoToolchain(gradleVersion: GradleVersion) {
        project(
            projectName = "simple".fullProjectName,
            gradleVersion = gradleVersion,
            buildJdk = getJdk11().javaHome
        ) {
            //language=Groovy
            buildGradle.append(
                """
                tasks.named("compileKotlin") {
                    doLast {
                        def actualJvmTarget = filteredArgumentsMap['jvmTarget']
                        if (actualJvmTarget != "null") {
                            //noinspection GroovyAssignabilityCheck
                            throw new GradleException("Expected `jvmTarget` value is 'null' but the actual value was ${'$'}actualJvmTarget")
                        }
                    }
                }
                """.trimIndent()
            )
            build("assemble")
        }
    }

    @DisplayName("Kotlin toolchain should support configuration cache")
    @GradleTestVersions
    @GradleTest
    internal fun testConfigurationCache(gradleVersion: GradleVersion) {
        project(
            projectName = "simple".fullProjectName,
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.withConfigurationCache
        ) {
            useToolchainExtension(15)

            //language=properties
            gradleProperties.append(
                """
                # suppress inspection "UnusedProperty"
                kotlin.jvm.target.validation.mode = error
                """.trimIndent()
            )

            build("assemble")
            build("assemble")
        }
    }

    @DisplayName("Should work with configuration cache when toolchain is not configured")
    @GradleTest
    internal fun testConfigurationCacheNoToolchain(gradleVersion: GradleVersion) {
        project(
            projectName = "simple".fullProjectName,
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.withConfigurationCache
        ) {
            //language=properties
            gradleProperties.append(
                """
                # suppress inspection "UnusedProperty"
                kotlin.jvm.target.validation.mode = error
                """.trimIndent()
            )

            build("assemble")
            build("assemble")
        }
    }

    private fun BuildResult.assertJdkHomeIsUsingJdk(
        javaexecPath: String
    ) = assertOutputContains("[KOTLIN] Kotlin compilation 'jdkHome' argument: $javaexecPath")

    private fun getUserJdk(): JavaInfo = Jvm.forHome(File(System.getProperty("java.home")))
    private fun getJdk9(): JavaInfo = Jvm.forHome(File(System.getProperty("jdk9Home")))
    private fun getJdk11(): JavaInfo = Jvm.forHome(File(System.getProperty("jdk11Home")))
    // replace required for windows paths so Groovy will not complain about unexpected char '\'
    private fun getJdk9Path(): String = getJdk9().javaHome.absolutePath.replace("\\", "\\\\")
    private fun getJdk11Path(): String = getJdk11().javaHome.absolutePath.replace("\\", "\\\\")
    private val JavaInfo.javaHomeRealPath
        get() = javaHome
            .toPath()
            .toRealPath()
            .toAbsolutePath()
            .toString()

    private val String.fullProjectName get() = "kotlin-java-toolchain/$this"

    private fun TestProject.setJvmTarget(
        jvmTarget: String
    ) {
        //language=Groovy
        buildGradle.append(
            """
            import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
            
            tasks.withType(KotlinCompile).configureEach {
                 kotlinOptions {
                      jvmTarget = "$jvmTarget"
                 }            
            }
            """.trimIndent()
        )
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

    private fun TestProject.useJdkToCompile(
        jdkPath: String,
        jdkVersion: JavaVersion
    ) {
        //language=Groovy
        buildGradle.append(
            """
            import org.gradle.api.JavaVersion
            import org.jetbrains.kotlin.gradle.tasks.UsesKotlinJavaToolchain
            
            project.tasks
                 .withType(UsesKotlinJavaToolchain.class)
                 .configureEach {
                      it.kotlinJavaToolchain.jdk.use(
                           "$jdkPath",
                           JavaVersion.${jdkVersion.name}
                      )
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
            
            afterEvaluate {
                logger.info("Toolchain jdk path: ${'$'}{defaultLauncher.get().metadata.installationPath.asFile.absolutePath}")
            }
            """.trimIndent()
        )
    }

    private fun TestProject.useToolchainExtension(
        jdkVersion: Int
    ) {
        //language=Groovy
        buildGradle.append(
            """
            import org.gradle.api.plugins.JavaPluginExtension
            import org.gradle.jvm.toolchain.JavaLanguageVersion
            import org.gradle.jvm.toolchain.JavaToolchainService
            
            kotlin {
                jvmToolchain {
                    languageVersion.set(JavaLanguageVersion.of($jdkVersion))
                }
            }
            
            afterEvaluate {
                def toolchain = project.extensions.getByType(JavaPluginExtension.class).toolchain
                def service = project.extensions.getByType(JavaToolchainService.class)
                //noinspection GroovyUnusedAssignment
                def defaultLauncher = service.launcherFor(toolchain)
                logger.info("Toolchain jdk path: ${'$'}{defaultLauncher.get().metadata.installationPath.asFile.absolutePath}")
            }
            """.trimIndent()
        )
    }

    private fun shouldUseToolchain(gradleVersion: GradleVersion) = gradleVersion >= GradleVersion.version("6.7")

    private fun BuildResult.getToolchainExecPathFromLogs() = output
        .lineSequence()
        .first { it.startsWith("Toolchain jdk path:") }
        .substringAfter("Toolchain jdk path: ")
}