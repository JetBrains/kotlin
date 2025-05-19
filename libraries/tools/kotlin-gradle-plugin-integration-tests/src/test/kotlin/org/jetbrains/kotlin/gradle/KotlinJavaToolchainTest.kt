/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.JavaVersion
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.UsesKotlinJavaToolchain
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.replaceText
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.appendText

@DisplayName("Kotlin Java Toolchain support")
class KotlinJavaToolchainTest : KGPBaseTest() {

    @JvmGradlePluginTests
    @GradleTest
    @DisplayName("Should use by default same jvm as Gradle daemon for jdkHome")
    internal fun byDefaultShouldUseGradleJDK(gradleVersion: GradleVersion) {
        project(
            projectName = "simple".fullProjectName,
            gradleVersion = gradleVersion
        ) {
            build("assemble") {
                assertJdkHomeIsUsingJdk(getUserJdk().jdkRealPath)
            }
        }
    }

    @JvmGradlePluginTests
    @GradleTest
    @DisplayName("Should use provided jdk location to compile Kotlin sources")
    internal fun customJdkHomeLocation(gradleVersion: GradleVersion) {
        project(
            projectName = "simple".fullProjectName,
            gradleVersion = gradleVersion,
        ) {
            setJavaCompilationCompatibility(JavaVersion.VERSION_11)

            useJdkToCompile(
                jdk11Info.jdkPath,
                JavaVersion.VERSION_11
            )

            build("assemble") {
                assertJdkHomeIsUsingJdk(jdk11Info.jdkRealPath)
            }
        }
    }

    @JvmGradlePluginTests
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
            useToolchainExtension(11)

            build("assemble")
        }

        project(
            projectName = "simple".fullProjectName,
            gradleVersion = gradleVersion,
            projectPathAdditionalSuffix = "2/cache-test",
            buildOptions = defaultBuildOptions.copy(buildCacheEnabled = true)
        ) {
            enableLocalBuildCache(buildCache)
            useToolchainExtension(11)

            build("assemble") {
                assertTasksFromCache(":compileKotlin")
            }
        }
    }

    @JvmGradlePluginTests
    @DisplayName("Kotlin compile task should reuse build cache when toolchain is set and build is happening on different JDKs")
    @GradleTest
    internal fun differentBuildJDKBuildCacheHit(gradleVersion: GradleVersion) {
        val buildCache = workingDir.resolve("custom-jdk-build-cache")
        project(
            projectName = "simple".fullProjectName,
            gradleVersion = gradleVersion,
            projectPathAdditionalSuffix = "1/cache-test",
            buildOptions = defaultBuildOptions.copy(buildCacheEnabled = true),
            buildJdk = jdk17Info.javaHome
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
            buildJdk = jdk17Info.javaHome
        ) {
            enableLocalBuildCache(buildCache)
            useToolchainExtension(11)

            build("assemble") {
                assertTasksFromCache(":compileKotlin")
            }
        }
    }

    @JvmGradlePluginTests
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
            useToolchainExtension(11)

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

    @JvmGradlePluginTests
    @DisplayName("Kapt task should use only process worker isolation when kotlin java toolchain is set")
    @GradleTest
    internal fun kaptTasksShouldUseProcessWorkersIsolation(gradleVersion: GradleVersion) {
        project(
            projectName = "simpleWithKapt".fullProjectName,
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(languageVersion = "1.9"),
        ) {
            useToolchainExtension(11)

            gradleProperties.append(
                "kapt.workers.isolation = none"
            )

            build("assemble") {
                assertJdkHomeIsUsingJdk(getToolchainExecPathFromLogs())

                assertOutputContains("Using workers PROCESS isolation mode to run kapt")
                assertOutputContains("Using non-default Kotlin java toolchain - 'kapt.workers.isolation == none' property is ignored!")
            }
        }
    }

    @JvmGradlePluginTests
    @DisplayName("Kapt task should use worker no-isolation mode when build is using Gradle JDK")
    @GradleTest
    internal fun kaptTasksShouldUseNoIsolationModeOnDefaultJvm(gradleVersion: GradleVersion) {
        project(
            projectName = "simpleWithKapt".fullProjectName,
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(languageVersion = "1.9"),
        ) {
            build("assemble") {
                assertOutputContains("Using workers NONE isolation mode to run kapt")
                assertOutputDoesNotContain("Using non-default Kotlin java toolchain - 'kapt.workers.isolation == none' property is ignored!")
            }
        }
    }

    @JvmGradlePluginTests
    @DisplayName("Kapt tasks with custom JDK should be cacheable")
    @GradleTest
    internal fun kaptTasksWithCustomJdkCacheable(gradleVersion: GradleVersion) {
        val buildCache = workingDir.resolve("custom-jdk-build-cache")
        project(
            projectName = "simpleWithKapt".fullProjectName,
            gradleVersion = gradleVersion,
            projectPathAdditionalSuffix = "1/cache-test",
            buildOptions = defaultBuildOptions.copy(buildCacheEnabled = true, languageVersion = "1.9")
        ) {
            enableLocalBuildCache(buildCache)
            useToolchainExtension(11)

            build("assemble")
        }

        project(
            projectName = "simpleWithKapt".fullProjectName,
            gradleVersion = gradleVersion,
            projectPathAdditionalSuffix = "2/cache-test",
            buildOptions = defaultBuildOptions.copy(buildCacheEnabled = true, languageVersion = "1.9"),
        ) {
            enableLocalBuildCache(buildCache)
            useToolchainExtension(11)

            build("assemble") {
                assertTasksFromCache(
                    ":kaptGenerateStubsKotlin",
                    ":kaptKotlin",
                    ":compileKotlin"
                )
            }
        }
    }

    @JvmGradlePluginTests
    @DisplayName("Kapt tasks with default JDK and different isolation modes should be cacheable")
    @GradleTest
    internal fun kaptCacheableOnSwitchingIsolationModeAndDefaultJDK(gradleVersion: GradleVersion) {
        val buildCache = workingDir.resolve("custom-jdk-build-cache")
        project(
            projectName = "simpleWithKapt".fullProjectName,
            gradleVersion = gradleVersion,
            projectPathAdditionalSuffix = "1/cache-test",
            buildOptions = defaultBuildOptions.copy(buildCacheEnabled = true, languageVersion = "1.9")
        ) {
            enableLocalBuildCache(buildCache)

            build("assemble")
        }

        project(
            projectName = "simpleWithKapt".fullProjectName,
            gradleVersion = gradleVersion,
            projectPathAdditionalSuffix = "2/cache-test",
            buildOptions = defaultBuildOptions.copy(buildCacheEnabled = true, languageVersion = "1.9"),
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

    @JvmGradlePluginTests
    @DisplayName("Should allow to set JDK version for tasks via Java toolchain")
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

    @JvmGradlePluginTests
    @DisplayName("Should allow to set Java toolchain via extension")
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

    @JvmGradlePluginTests
    @DisplayName("Toolchain should be correctly supported in multiplatform plugin jvm with 'withJava()' targets")
    @GradleTest
    internal fun toolchainCorrectlySupportedInMPPluginWithJava(gradleVersion: GradleVersion) {
        project(
            projectName = "mppJvmWithJava".fullProjectName,
            gradleVersion = gradleVersion
        ) {
            if (!isWithJavaSupported) buildGradle.replaceText("withJava()", "")
            useToolchainToCompile(11)

            build("assemble") {
                assertJdkHomeIsUsingJdk(getToolchainExecPathFromLogs())
            }
        }
    }

    @JvmGradlePluginTests
    @DisplayName("Toolchain should be correctly supported in multiplatform plugin jvm with 'withJava()' targets")
    @GradleTest
    internal fun toolchainCorrectlySupportedInMPPlugin(gradleVersion: GradleVersion) {
        project(
            projectName = "mppJvmWithJava".fullProjectName,
            gradleVersion = gradleVersion
        ) {
            if (!isWithJavaSupported) buildGradle.replaceText("withJava()", "")
            useToolchainToCompile(11)

            build("assemble") {
                assertJdkHomeIsUsingJdk(getToolchainExecPathFromLogs())
            }
        }
    }

    @JvmGradlePluginTests
    @DisplayName("Should set 'jvmTarget' option if user does not specify it explicitly via jdk setter")
    @GradleTest
    internal fun shouldSetJvmTargetNonSpecifiedByUserViaSetJdk(gradleVersion: GradleVersion) {
        project(
            projectName = "simple".fullProjectName,
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)
        ) {
            useJdkToCompile(
                jdk11Info.jdkPath,
                JavaVersion.VERSION_11
            )

            //language=properties
            gradleProperties.append(
                """
                # suppress inspection "UnusedProperty"
                kotlin.jvm.target.validation.mode = warning
                """.trimIndent()
            )

            build("build") {
                assertOutputContains("-jvm-target 11")
                assertOutputDoesNotContain("-jvm-target 17")
            }
        }
    }

    @JvmGradlePluginTests
    @DisplayName("Should not override user specified 'jvmTarget' option via jdk setter")
    @GradleTest
    internal fun shouldNotOverrideUserJvmTargetViaSetJDK(gradleVersion: GradleVersion) {
        project(
            projectName = "simple".fullProjectName,
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)
        ) {
            setJvmTarget("17")

            useJdkToCompile(
                jdk11Info.jdkPath,
                JavaVersion.VERSION_11
            )

            build("build") {
                assertOutputContains("-jvm-target 17")
                assertOutputDoesNotContain("-jvm-target 11")
            }
        }
    }

    @JvmGradlePluginTests
    @DisplayName("Should set 'jvmTarget' option if user does not specify it explicitly via toolchain setter")
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
                assertOutputDoesNotContain("-jvm-target 17")
            }
        }
    }

    @JvmGradlePluginTests
    @DisplayName("Should not override user specified 'jvmTarget' option via toolchain setter")
    @GradleTest
    internal fun shouldNotOverrideUserSpecifiedJvmTargetViaToolchain(gradleVersion: GradleVersion) {
        project(
            projectName = "simple".fullProjectName,
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)
        ) {
            setJvmTarget("17")
            useToolchainToCompile(11)

            //language=properties
            gradleProperties.append(
                """
                # suppress inspection "UnusedProperty"
                kotlin.jvm.target.validation.mode = warning
                """.trimIndent()
            )

            build("build") {
                assertOutputContains("-jvm-target 17")
                assertOutputDoesNotContain("-jvm-target 11")
            }
        }
    }

    @JvmGradlePluginTests
    @DisplayName("Setting toolchain via java extension should also affect Kotlin compilations")
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

            build("build") {
                assertOutputContains("[KOTLIN] Kotlin compilation 'jdkHome' argument: ${jdk11Info.jdkPath.replace("\\\\", "\\")}")
            }
        }
    }

    @JvmGradlePluginTests
    @DisplayName("Setting toolchain via java extension should update jvm-target argument on eager task creation")
    @GradleTest
    internal fun settingToolchainViaJavaUpdateJvmTarget(gradleVersion: GradleVersion) {
        project(
            projectName = "kotlinJavaProject".fullProjectName,
            gradleVersion = gradleVersion
        ) {
            //language=groovy
            buildGradle.append(
                """
                tasks.named("compileKotlin").get() // Trigger task eager creation
                
                java {
                    toolchain {
                        languageVersion.set(JavaLanguageVersion.of(11))
                    }
                }
                
                """.trimIndent()
            )

            build("build", buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)) {
                val compilerArgs = output.lineSequence()
                    .filter { it.contains(":compileKotlin Kotlin compiler args:") }
                    .first()
                assert(compilerArgs.contains("-jvm-target 11")) {
                    "Kotlin compilation jvm-target argument is ${output.substringAfter("-jvm-target ").substringBefore(" ")}"
                }
            }
        }
    }

    @JvmGradlePluginTests
    @DisplayName("Build should not produce warnings when '-no-jdk' option is present")
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

    @AndroidGradlePluginTests
    @DisplayName("Toolchain should take into account kotlin options that are set via android extension")
    @GradleAndroidTest
    internal fun kotlinOptionsAndroidAndToolchain(
        gradleVersion: GradleVersion,
        agpVersion: String,
        providedJdk: JdkVersions.ProvidedJdk
    ) {
        project(
            "android".fullProjectName,
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion).suppressWarningFromAgpWithGradle813(gradleVersion),
            buildJdk = providedJdk.location
        ) {
            useToolchainExtension(11)

            build("assembleDebug")
        }
    }

    @JvmGradlePluginTests
    @DisplayName("Toolchain should not make an exception when build is running on JDK 17, but toolchain is set to JDK 1.8")
    @GradleTest
    internal fun shouldNotRaiseErrorOnJDK11withJDK1_8Toolchain(gradleVersion: GradleVersion) {
        project(
            projectName = "simple".fullProjectName,
            gradleVersion = gradleVersion,
            buildJdk = jdk17Info.javaHome
        ) {
            useToolchainExtension(8)

            build("assemble")
        }
    }

    @JvmGradlePluginTests
    @DisplayName("JVM target shouldn't be changed when toolchain is not configured")
    // Starting Gradle 8.0 toolchain is always configured by default
    @GradleTestVersions(maxVersion = TestVersions.Gradle.G_7_6)
    @GradleTest
    internal fun shouldNotChangeJvmTargetWithNoToolchain(gradleVersion: GradleVersion) {
        project(
            projectName = "simple".fullProjectName,
            gradleVersion = gradleVersion,
            buildJdk = jdk11Info.javaHome
        ) {
            //language=properties
            gradleProperties.append(
                """
                # suppress inspection "UnusedProperty"
                kotlin.jvm.target.validation.mode = warning
                """.trimIndent()
            )

            val defaultJvmTargetName = JvmTarget.DEFAULT.let {
                "${it.declaringJavaClass.canonicalName}.${it.name}"
            }

            //language=Groovy
            buildGradle.append(
                """
                tasks.named("compileKotlin") {
                    doLast {
                        def actualJvmTarget = compilerOptions.jvmTarget.orNull
                        if (actualJvmTarget != $defaultJvmTargetName) {
                            //noinspection GroovyAssignabilityCheck
                            throw new GradleException("Expected `jvmTarget` value is 'JvmTarget.DEFAULT' but the actual value was ${'$'}actualJvmTarget")
                        }
                    }
                }
                """.trimIndent()
            )
            build("assemble")
        }
    }

    @JvmGradlePluginTests
    @DisplayName("Kotlin toolchain should support configuration cache")
    @GradleTest
    internal fun testConfigurationCache(gradleVersion: GradleVersion) {
        project(
            projectName = "simple".fullProjectName,
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.withConfigurationCache
        ) {
            useToolchainExtension(11)

            build("assemble")
            build("assemble") {
                assertTasksUpToDate(":compileKotlin")
            }
        }
    }

    @JvmGradlePluginTests
    @DisplayName("Should work with configuration cache when toolchain is not configured")
    @GradleTest
    internal fun testConfigurationCacheNoToolchain(gradleVersion: GradleVersion) {
        project(
            projectName = "simple".fullProjectName,
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.withConfigurationCache
        ) {
            build("assemble")
            build("assemble")
        }
    }

    @JvmGradlePluginTests
    @DisplayName("Toolchain simplified method with JDK version in extension is working")
    @GradleTest
    internal fun toolchainSimplifiedConfiguration(gradleVersion: GradleVersion) {
        project(
            projectName = "simple".fullProjectName,
            gradleVersion = gradleVersion,
            buildJdk = jdk17Info.javaHome
        ) {
            //language=Groovy
            buildGradle.append(
                """
                 import org.gradle.api.plugins.JavaPluginExtension
                 import org.gradle.jvm.toolchain.JavaToolchainService
                 
                 kotlin {
                     jvmToolchain(8)
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

            build("assemble") {
                assertJdkHomeIsUsingJdk(getToolchainExecPathFromLogs())
            }
        }
    }

    @JvmGradlePluginTests
    @DisplayName("KT-55004: Should use non-default toolchain for parent project")
    @GradleTest
    internal fun toolchainFromParentProject(gradleVersion: GradleVersion) {
        project(
            projectName = "multiproject".fullProjectName,
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)
        ) {
            subProject("lib").buildScriptInjection {
                project.tasks.named("compileJava", JavaCompile::class.java).configure { task ->
                    task.targetCompatibility = JavaVersion.VERSION_11.toString()
                    task.sourceCompatibility = JavaVersion.VERSION_11.toString()
                }
                project.tasks.withType(UsesKotlinJavaToolchain::class.java).configureEach { task ->
                    task.kotlinJavaToolchain.jdk.use(
                        jdk11Info.jdkPath,
                        JavaVersion.VERSION_11
                    )
                }
            }

            build(":lib:compileKotlin") {
                assertOutputDoesNotContain("-jvm-target 17")
                assertOutputContains("-jvm-target 11")
            }
        }
    }

    @JvmGradlePluginTests
    @DisplayName("Toolchain should not override Jvm target configured in project level DSL")
    @GradleTest
    fun toolchainNotOverrideProjectJvmTarget(gradleVersion: GradleVersion) {
        project(
            projectName = "simple".fullProjectName,
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)
        ) {
            //language=properties
            gradleProperties.append(
                """
                # suppress inspection "UnusedProperty"
                kotlin.jvm.target.validation.mode = warning
                """.trimIndent()
            )

            buildGradle.appendText(
                //language=groovy
                """
                |
                |kotlin.compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
                |
                """.trimMargin()
            )

            build(":compileKotlin") {
                assertTasksExecuted(":compileKotlin")
                assertCompilerArgument(":compileKotlin", "-jvm-target 11")
            }
        }
    }

    @AndroidGradlePluginTests
    @DisplayName("Toolchain should not override Jvm target configured via kotlinOptions in android project")
    @GradleAndroidTest
    internal fun kotlinOptionsAndroidAndToolchainNotOverrideJvmTarget(
        gradleVersion: GradleVersion,
        agpVersion: String,
        providedJdk: JdkVersions.ProvidedJdk
    ) {
        project(
            "android".fullProjectName,
            gradleVersion,
            buildOptions = defaultBuildOptions
                .copy(androidVersion = agpVersion, logLevel = LogLevel.DEBUG)
                .suppressWarningFromAgpWithGradle813(gradleVersion),
            buildJdk = providedJdk.location
        ) {
            buildGradle.appendText(
                //language=groovy
                """
                |
                |android.kotlinOptions.jvmTarget = "11"
                """.trimMargin()
            )

            // Otherwise jvm target validation on Gradle 8+ will fail
            //language=properties
            gradleProperties.append(
                """
                # suppress inspection "UnusedProperty"
                kotlin.jvm.target.validation.mode = warning
                """.trimIndent()
            )

            build(":compileDebugKotlin") {
                assertTasksExecuted(":compileDebugKotlin")
                assertCompilerArgument(":compileDebugKotlin", "-jvm-target 11")
            }
        }
    }

    private fun BuildResult.assertJdkHomeIsUsingJdk(
        javaexecPath: String
    ) = assertOutputContains("[KOTLIN] Kotlin compilation 'jdkHome' argument: $javaexecPath")

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

    private fun BuildResult.getToolchainExecPathFromLogs() = output
        .lineSequence()
        .first { it.startsWith("Toolchain jdk path:") }
        .substringAfter("Toolchain jdk path: ")
}
