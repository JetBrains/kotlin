/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.android

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.Kapt3BaseIT
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.*
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.appendText
import kotlin.io.path.writeText

@DisplayName("android with kapt3 tests")
@AndroidGradlePluginTests
class Kapt3AndroidIT : Kapt3BaseIT() {
    @DisplayName("KT-15001")
    @GradleAndroidTest
    fun testKt15001(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "kt15001".withPrefix,
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location
        ) {
            build("assembleDebug") {
                assertKaptSuccessful()
            }
        }
    }

    @DisplayName("KT-25374: kapt doesn't fail with anonymous classes with IC")
    @GradleAndroidTest
    fun testICWithAnonymousClasses(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "icAnonymousTypes".withPrefix,
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location
        ) {
            build("assembleDebug") {
                assertKaptSuccessful()
            }

            val modifiedSource = javaSourcesDir().resolve("a.kt")
            modifiedSource.modify {
                assert(it.contains("CrashMe2(1000)"))
                it.replace("CrashMe2(1000)", "CrashMe2(2000)")
            }

            build("assembleDebug")
        }
    }

    @DisplayName("static dsl options are passed to kapt")
    @GradleAndroidTest
    fun testStaticDslOptionsPassedToKapt(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "android-dagger".withPrefix,
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location
        ) {
            subProject("app").buildGradle.appendText(
                """
    
                apply plugin: 'kotlin-kapt'
    
                android {
                    defaultConfig {
                        javaCompileOptions {
                            annotationProcessorOptions {
                                arguments += ["enable.some.test.option": "true"]
                            }
                        }
                    }
                }
                """.trimIndent()
            )

            build(":app:kaptDebugKotlin") {
                assertOutputContains(Regex("AP options.*enable\\.some\\.test\\.option=true"))
            }
        }
    }

    @DisplayName("KT-45532: kapt tasks shouldn't create outputs at configuration time")
    @GradleAndroidTest
    fun kaptTasksShouldNotCreateOutputsOnConfigurationPhase(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "android-dagger".withPrefix,
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location
        ) {
            build("--dry-run", "assembleDebug") {
                assertFileInProjectNotExists("app/build/tmp")
                assertFileInProjectNotExists("app/build/generated")
            }
        }
    }

    @DisplayName("KT-31127: kapt doesn't break JavaCompile when using Filer API")
    @GradleAndroidTest
    fun testKotlinProcessorUsingFiler(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "AndroidLibraryKotlinProject",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location
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
                apply plugin: 'kotlin-kapt'
                android {
                    libraryVariants.all {
                        it.generateBuildConfig.enabled = false
                    }
                }
    
                dependencies {
                    kapt "org.jetbrains.kotlin:annotation-processor-example:${"$"}kotlin_version"
                    implementation "org.jetbrains.kotlin:annotation-processor-example:${"$"}kotlin_version"
                }
                """.trimIndent()
            )

            // The test must not contain any java sources in order to detect the issue.
            val javaSources = projectPath.allJavaSources
            assert(javaSources.isEmpty()) {
                "Test project shouldn't contain any java sources, but it contains: $javaSources"
            }
            kotlinSourcesDir().resolve("Dummy.kt").modify {
                it.replace("class Dummy", "@example.KotlinFilerGenerated class Dummy")
            }

            build("assembleDebug") {
                assertFileInProjectExists("build/generated/source/kapt/debug/demo/DummyGenerated.kt")
                assertTasksExecuted(":compileDebugKotlin")
                assertTasksNoSource(":compileDebugJavaWithJavac")
            }
        }
    }

    @DisplayName("kapt uses AGP annotation processor option providers as nested inputs")
    @GradleAndroidTest
    fun testKaptUsingApOptionProvidersAsNestedInputOutput(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "AndroidProject",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion, kaptOptions = null),
            buildJdk = jdkVersion.location
        ) {
            subProject("Android").buildGradle.appendText(
                //language=Gradle
                """

                apply plugin: 'kotlin-kapt'

                class MyNested implements org.gradle.process.CommandLineArgumentProvider {
                    private final File argsFile
                     
                    MyNested(File argsFile) {
                        this.argsFile = argsFile
                    } 
    
                    @InputFile
                    File inputFile = null
    
                    @Override
                    Iterable<String> asArguments() {
                        // Read the arguments from a file, because changing them in a build script is treated as an
                        // implementation change by Gradle:
                        return [argsFile.text]
                    }
                }
    
                def nested = new MyNested(rootProject.file("args.txt"))
                nested.inputFile = file("${'$'}projectDir/in.txt")
    
                android.applicationVariants.all {
                    it.javaCompileOptions.annotationProcessorOptions.compilerArgumentProviders.add(nested)
                }
                """.trimIndent()
            )

            val inFile = subProject("Android").projectPath.resolve("in.txt")
            inFile.writeText("1234")
            val argsFile = projectPath.resolve("args.txt")
            argsFile.writeText("1234")

            val kaptTasks = listOf(":Android:kaptFlavor1DebugKotlin")
            val javacTasks = listOf(":Android:compileFlavor1DebugJavaWithJavac")

            val buildTasks = (kaptTasks + javacTasks).toTypedArray()

            build(*buildTasks) {
                assertTasksExecuted(kaptTasks + javacTasks)
            }

            inFile.appendText("5678")

            build(*buildTasks) {
                assertTasksExecuted(kaptTasks)
                assertTasksUpToDate(javacTasks)
            }

            // Changing only the annotation provider arguments should not trigger the tasks to run, as the arguments may be outputs,
            // internals or neither:
            argsFile.appendText("5678")

            build(*buildTasks) {
                assertTasksUpToDate(javacTasks + kaptTasks)
            }
        }
    }

    @DisplayName("agp annotation processor nested arguments are not evaluated at configuration time")
    @GradleAndroidTest
    fun testAgpNestedArgsNotEvaluatedDuringConfiguration(
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
            subProject("Android").buildGradle.appendText(
                //language=Gradle
                """

                apply plugin: 'kotlin-kapt'

                class MyNested implements org.gradle.process.CommandLineArgumentProvider {
                    @Override
                    Iterable<String> asArguments() {
                        throw new RuntimeException("This should not be invoked during configuration.")
                    }
                }
    
                def nested = new MyNested()
    
                android.applicationVariants.all {
                    it.javaCompileOptions.annotationProcessorOptions.compilerArgumentProviders.add(nested)
                }
                """.trimIndent()
            )

            build(":Android:kaptFlavor1DebugKotlin", "--dry-run")

            build(
                ":Android:kaptFlavor1DebugKotlin", "--dry-run",
                buildOptions = buildOptions.copy(kaptOptions = BuildOptions.KaptOptions(verbose = false))
            )
        }
    }

    @DisplayName("KT-55334: Kapt generate stubs and related KotlinCompile tasks are using similar -module-name value")
    @GradleAndroidTest
    fun kaptGenerateStubsModuleName(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk
    ) {
        project(
            "kapt2/android-dagger",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location
        ) {
            build(":app:compileDebugAndroidTestKotlin") {
                val stubsFile = subProject("app")
                    .projectPath
                    .resolve("build/tmp/kapt3/stubs/debugAndroidTest/com/example/dagger/kotlin/TestClass.java")
                assertFileExists(stubsFile)
                assertFileContains(
                    stubsFile,
                    "public final void bar${'$'}app_debugAndroidTest() {"
                )

                val compiledClassFile = subProject("app")
                    .projectPath
                    .resolve("build/tmp/kotlin-classes/debugAndroidTest/com/example/dagger/kotlin/TestClass.class")
                assertFileExists(compiledClassFile)
                checkBytecodeContains(
                    compiledClassFile.toFile(),
                    "public final bar${'$'}app_debugAndroidTest()V"
                )
            }
        }
    }
}