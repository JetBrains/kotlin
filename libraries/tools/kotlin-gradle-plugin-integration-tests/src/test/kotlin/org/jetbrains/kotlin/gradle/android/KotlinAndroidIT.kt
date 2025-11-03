/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.android

import org.gradle.api.logging.LogLevel
import org.gradle.kotlin.dsl.kotlin
import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import java.nio.file.Files
import kotlin.io.path.appendText
import kotlin.io.path.createDirectories
import kotlin.io.path.createDirectory
import kotlin.io.path.readLines
import kotlin.io.path.writeText
import kotlin.test.assertContains

@DisplayName("base kotlin-android tests")
@AndroidGradlePluginTests
class KotlinAndroidIT : KGPBaseTest() {

    @DisplayName("android project compilation smoke test")
    @GradleAndroidTest
    fun testSimpleCompile(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        fun BuildResult.assertKotlinGradleBuildServicesAreInitialized(times: Int = 1) {
            assertOutputContainsExactlyTimes("Initialized KotlinGradleBuildServices", expectedCount = times)
            assertOutputContainsExactlyTimes("Disposed KotlinGradleBuildServices", expectedCount = times)
        }

        project(
            "AndroidProject",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion, logLevel = LogLevel.DEBUG),
            buildJdk = jdkVersion.location
        ) {
            val modules = listOf("Android", "Lib")
            val flavors = listOf("Flavor1", "Flavor2")
            val buildTypes = listOf("Debug")

            val expectedTasks = mutableListOf<String>()
            for (module in modules) {
                for (flavor in flavors) {
                    for (buildType in buildTypes) {
                        expectedTasks.add(":$module:compile$flavor${buildType}Kotlin")
                    }
                }
            }

            build("assembleDebug", ":Android:test") {
                val pattern = ":Android:compile[\\w\\d]+Kotlin".toRegex()
                assertTasksExecuted(expectedTasks + tasks.map { it.path }.filter { it.matches(pattern) })
                assertOutputContains("InternalDummyTest PASSED")
                // In Gradle 8, `KotlinGradleBuildServices` is instantiated twice on the first run:
                // once during the configuration phase, and again during the execution phase
                // when the stored configuration cache entry is deserialized
                // In contrast, Gradle 7 only instantiates it once and does not reuse the configuration cache entry for this process
                val times = if (gradleVersion < GradleVersion.version(TestVersions.Gradle.G_8_0)) 1 else 2
                assertKotlinGradleBuildServicesAreInitialized(times)
            }

            // Run the a build second time, assert everything is up-to-date
            build("assembleDebug") {
                assertTasksUpToDate(expectedTasks)
                // Since the configuration cache is already stored, `KotlinGradleBuildServices` will be instantiated only once
                assertKotlinGradleBuildServicesAreInitialized()
            }
        }
    }

    @DisplayName("KT-16897: the `assembleAndroidTest` task builds fine without the `build` task")
    @GradleAndroidTest
    fun testAssembleAndroidTest(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "AndroidProject",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location
        ) {
            build("assembleAndroidTest")
        }
    }

    @DisplayName("KT-12303: compilation works fine with icepick")
    @GradleAndroidTest
    fun testAndroidIcepickProject(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "AndroidIcepickProject",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location,
            dependencyManagement = DependencyManagement.DefaultDependencyManagement(
                setOf("https://clojars.org/repo/")
            )
        ) {
            build("assembleDebug")
        }
    }

    @DisplayName("compilation works fine with parcelize")
    @GradleAndroidTest
    fun testParcelize(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "AndroidParcelizeProject",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location
        ) {
            build("assembleDebug")
        }
    }

    @DisplayName("KT-46626: kotlin-android produces clear error message when no android plugin is applied")
    @GradleAndroidTest
    fun shouldAllowToApplyPluginWhenAndroidPluginIsMissing(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "simpleProject",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location
        ) {
            fun replaceKotlinJvmByKotlinAndroid(specifyPluginVersion: Boolean): (String) -> CharSequence = { line ->
                if (line.contains("id \"org.jetbrains.kotlin.jvm\"")) {
                    "    id \"org.jetbrains.kotlin.android\"" + if (specifyPluginVersion) " version \"${'$'}kotlin_version\"" else ""
                } else {
                    line
                }
            }

            buildGradle.modify {
                it.lines().joinToString(
                    separator = "\n",
                    transform = replaceKotlinJvmByKotlinAndroid(false)
                )
            }
            settingsGradle.modify {
                it.lines().joinToString(
                    separator = "\n",
                    transform = replaceKotlinJvmByKotlinAndroid(true)
                )
            }

            buildAndFail("tasks") {
                assertOutputContains("'kotlin-android' plugin requires one of the Android Gradle plugins.")
            }
        }
    }

    @DisplayName("KT-49483: lint dependencies are resolved properly")
    @GradleAndroidTest
    fun testLintDependencyResolutionKt49483(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        disabledOnWindowsWhenAgpVersionIsLowerThan(agpVersion, "7.4.0", "Lint leaves opened file descriptors")
        project(
            "AndroidProject",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location
        ) {
            buildGradle.modify {
                it.replace(
                    "plugins {",
                    """
                    plugins {
                       id("com.android.lint")

                    """.trimIndent()
                )
            }

            subProject("Lib").buildGradle.appendText(
                //language=Gradle
                """
                
                android {
                    lintOptions.checkDependencies = true
                }
                dependencies {
                    implementation(project(":java-lib"))
                }
                """.trimIndent()
            )

            settingsGradle.appendText(
                //language=Gradle
                """
                
                include("java-lib")
                """.trimIndent()
            )

            subProject("java-lib").buildGradle.apply {
                Files.createDirectories(parent)
                writeText(
                    //language=Gradle
                    """
                    plugins {
                        id("java-library")
                        id("com.android.lint")
                    }
                    """.trimIndent()
                )
            }

            // Gradle 6 + AGP 4 produce a deprecation warning on parallel executions about resolving a configuration from another project
            build(":Lib:lintFlavor1Debug", buildOptions = buildOptions.copy(parallel = true)) {
                assertOutputDoesNotContain("as an external dependency and not analyze it.")
            }
        }
    }

    @DisplayName("publish with omitted stdlib version sets proper version")
    @GradleAndroidTest
    fun testOmittedStdlibVersion(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "AndroidProject",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location
        ) {
            subProject("Lib").buildGradle.modify {
                it +
                        //language=Gradle
                        """
                        
                        apply plugin: 'maven-publish'
                            
                        dependencies {
                             implementation 'org.jetbrains.kotlin:kotlin-stdlib'
                        }
                        
                        android {
                            defaultPublishConfig 'flavor1Debug'
                        }
                        
                        afterEvaluate {
                            publishing {
                                publications {
                                    flavorDebug(MavenPublication) {
                                        from components.flavor1Debug
                                        
                                        group = 'com.example'
                                        artifactId = 'flavor1Debug'
                                        version = '1.0'
                                    }
                                }
                                repositories {
                                    maven {
                                        url = "${'$'}buildDir/repo"
                                    }
                                }
                            }
                        }
                        """.trimIndent()
            }

            build(":Lib:assembleFlavor1Debug", ":Lib:publish") {
                assertTasksExecuted(":Lib:compileFlavor1DebugKotlin", ":Lib:publishFlavorDebugPublicationToMavenRepository")
                val pomLines =
                    subProject("Lib").projectPath.resolve("build/repo/com/example/flavor1Debug/1.0/flavor1Debug-1.0.pom").readLines()
                val stdlibVersionLineNumber = pomLines.indexOfFirst { "<artifactId>kotlin-stdlib</artifactId>" in it } + 1
                val versionLine = pomLines[stdlibVersionLineNumber]
                assertContains(versionLine, "<version>${buildOptions.kotlinVersion}</version>")
            }
        }
    }

    @DisplayName("KT-77288: android.kotlinOptions should not cause generated accessors compilation error")
    @GradleAndroidTest
    fun testKotlinOptionsDeprecation(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "AndroidSimpleApp",
            gradleVersion,
            buildJdk = jdkVersion.location,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
        ) {
            val buildSrcDir = projectPath.resolve("buildSrc").also { it.createDirectory() }
            buildSrcDir.resolve("build.gradle.kts").writeText(
                """
                |plugins {
                |   `kotlin-dsl`
                |}
                |
                |repositories {
                |    mavenLocal()
                |    google()
                |    mavenCentral()
                |}
                |
                |dependencies {
                |    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${buildOptions.kotlinVersion}")
                |    implementation("com.android.library:com.android.library.gradle.plugin:$agpVersion")
                |}
                """.trimMargin()
            )
            val buildSrcSourcesDir = buildSrcDir.resolve("src/main/kotlin").also { it.createDirectories() }
            buildSrcSourcesDir.resolve("my-utils.gradle.kts").writeText(
                """
                |plugins {
                |    id("org.jetbrains.kotlin.android")
                |    id("com.android.library")
                |}
                |
                |fun test() = println("hello")
                """.trimMargin()
            )

            if (gradleVersion < GradleVersion.version(TestVersions.Gradle.G_8_0)) {
                gradleProperties.appendText(
                    """
                systemProp.org.gradle.kotlin.dsl.precompiled.accessors.strict=true
                """.trimIndent()
                )
            }

            build("help")
        }
    }

    @DisplayName("KT-80785: AGP 9.0 with disabled built-in Kotlin fails with actionable diagnostic because of the new AGP DSL")
    @GradleAndroidTest
    @AndroidTestVersions(minVersion = TestVersions.AGP.AGP_90)
    fun testNewAgpDslDiagnostic(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "empty",
            gradleVersion,
            buildJdk = jdkVersion.location,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
        ) {
            plugins {
                kotlin("android")
                id("com.android.library")
            }
            gradleProperties.appendText(
                //language=properties
                """
                |
                |android.builtInKotlin=false
                """.trimMargin()
            )
            buildAndFail("help") {
                assertHasDiagnostic(KotlinToolingDiagnostics.KotlinAndroidIsIncompatibleWithTheNewAgpDsl)
                assertNoDiagnostic(KotlinToolingDiagnostics.KMPIsIncompatibleWithTheNewAgpDsl)
                assertNoDiagnostic(KotlinToolingDiagnostics.DeprecatedKotlinAndroidPlugin)
            }
        }
    }

    @DisplayName("AGP 9.0 with disabled built-in Kotlin and enabled Variants API produces deprecation warnings for 'org.jetbrains.kotlin.android' plugin")
    @GradleAndroidTest
    @AndroidTestVersions(minVersion = TestVersions.AGP.AGP_90)
    fun testKotlinAndroidDeprecationDiagnostic(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "empty",
            gradleVersion,
            buildJdk = jdkVersion.location,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
        ) {
            plugins {
                kotlin("android")
                id("com.android.library")
            }

            gradleProperties.appendText(
                //language=properties
                """
                |
                |android.builtInKotlin=false
                |android.newDsl=false
                """.trimMargin()
            )

            buildScriptInjection {
                with(androidLibrary) {
                    compileSdk = 36
                    namespace = "com.example"
                }
            }
            build("help") {
                assertNoDiagnostic(KotlinToolingDiagnostics.KotlinAndroidIsIncompatibleWithTheNewAgpDsl)
                assertNoDiagnostic(KotlinToolingDiagnostics.KMPIsIncompatibleWithTheNewAgpDsl)
                assertHasDiagnostic(KotlinToolingDiagnostics.DeprecatedKotlinAndroidPlugin)
            }
        }
    }

    @DisplayName("No 'org.jetbrains.kotlin.android' plugin deprecation with AGP <9.0")
    @GradleAndroidTest
    @AndroidTestVersions(
        minVersion = TestVersions.AGP.AGP_811,
        maxVersion = TestVersions.AGP.AGP_811,
    )
    fun testKotlinAndroidNoDeprecationDiagnostic(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "empty",
            gradleVersion,
            buildJdk = jdkVersion.location,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
        ) {
            plugins {
                kotlin("android")
                id("com.android.library")
            }

            buildScriptInjection {
                with(androidLibrary) {
                    compileSdk = 36
                    namespace = "com.example"
                }
            }
            build("help") {
                assertNoDiagnostic(KotlinToolingDiagnostics.KotlinAndroidIsIncompatibleWithTheNewAgpDsl)
                assertNoDiagnostic(KotlinToolingDiagnostics.DeprecatedKotlinAndroidPlugin)
                assertNoDiagnostic(KotlinToolingDiagnostics.KMPIsIncompatibleWithTheNewAgpDsl)
            }
        }
    }

    @DisplayName("KT-81601: KMP plus AGP 9.0 with disabled built-in Kotlin fails with actionable diagnostic because of the new AGP DSL ")
    @GradleAndroidTest
    @AndroidTestVersions(minVersion = TestVersions.AGP.AGP_90)
    fun testNewAgpDslDiagnosticInKmp(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "empty",
            gradleVersion,
            buildJdk = jdkVersion.location,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
        ) {
            plugins {
                kotlin("multiplatform")
                id("com.android.library")
            }

            buildScriptInjection {
                @Suppress("DEPRECATION")
                kotlinMultiplatform.androidTarget()

                with(androidLibrary) {
                    compileSdk = 36
                    namespace = "com.example"
                }
            }

            gradleProperties.appendText(
                //language=properties
                """
                |
                |android.builtInKotlin=false
                """.trimMargin()
            )
            buildAndFail("help") {
                assertHasDiagnostic(KotlinToolingDiagnostics.KMPIsIncompatibleWithTheNewAgpDsl)
                assertNoDiagnostic(KotlinToolingDiagnostics.KotlinAndroidIsIncompatibleWithTheNewAgpDsl)
                assertNoDiagnostic(KotlinToolingDiagnostics.DeprecatedKotlinAndroidPlugin)
            }
        }
    }

    @DisplayName("KT-80785: usage of new AGP DSL does not hide AgpWithBuiltInKotlinIsAlreadyApplied")
    @GradleAndroidTest
    @AndroidTestVersions(minVersion = TestVersions.AGP.AGP_90)
    fun testNewAgpDslDiagnosticWithBuiltInKotlin(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "empty",
            gradleVersion,
            buildJdk = jdkVersion.location,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
        ) {
            plugins {
                /*
                 * The plugin ordering matters here.
                 * If the order is reversed, a similar diagnostic is reported by AGP failing the build.
                 */
                id("com.android.library")
                kotlin("android")
            }
            buildAndFail("help") {
                assertHasDiagnostic(KotlinToolingDiagnostics.AgpWithBuiltInKotlinIsAlreadyApplied)
            }
        }
    }
}
