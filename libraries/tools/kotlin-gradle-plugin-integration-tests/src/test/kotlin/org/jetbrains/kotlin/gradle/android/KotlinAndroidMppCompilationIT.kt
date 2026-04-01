/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.android

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.gradle.tasks.CompilerPluginOptions
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.testResolveAllConfigurations
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.*

@DisplayName("KMP/Android: project compilation")
@AndroidGradlePluginTests
class KotlinAndroidMppCompilationIT : KGPBaseTest() {

    @DisplayName("android app can depend on mpp lib")
    @GradleAndroidTest
    fun testAndroidWithNewMppApp(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        val printOptionsTaskName = "printCompilerPluginOptions"

        project(
            "new-mpp-android",
            gradleVersion,
            buildOptions = defaultBuildOptions
                .copy(androidVersion = agpVersion)
                // KT-75899 Support Gradle Project Isolation in KGP JS & Wasm
                .disableIsolatedProjectsBecauseOfJsAndWasmKT75899(),
            buildJdk = jdkVersion.location
        ) {
            subProject("app").buildScriptInjection {
                project.tasks.register(printOptionsTaskName) { task ->
                    val compilations = project.provider {
                        kotlinMultiplatform.targets
                            .flatMap { it.compilations }
                            .mapNotNull { compilation ->
                                val sourceSetName = compilation.defaultSourceSet.name
                                val compileTask = compilation.compileTaskProvider.get()
                                when (compileTask) {
                                    is AbstractKotlinCompile<*> -> sourceSetName to compileTask
                                    else -> null
                                }
                            }
                            .associate { (sourceSetName, compileTask) ->
                                val args = compileTask
                                    .pluginOptions
                                    .get()
                                    .fold(CompilerPluginOptions()) { options, option -> options.plus(option) }
                                    .arguments
                                val cp = compileTask.pluginClasspath.files
                                sourceSetName to (args to cp)
                            }
                    }
                    task.doFirst {
                        compilations.get().forEach { sourceSetName, (args, cp) ->
                            println("$sourceSetName=args=>$args")
                            println("$sourceSetName=cp=>$cp")
                        }
                    }
                }
            }

            build("assemble", "compileDebugUnitTestJavaWithJavac", printOptionsTaskName) {
                // KT-30784
                assertOutputDoesNotContain("API 'variant.getPackageLibrary()' is obsolete and has been replaced")

                assertTasksExecuted(
                    ":lib:compileDebugKotlinAndroidLib",
                    ":lib:compileReleaseKotlinAndroidLib",
                    ":lib:compileKotlinJvmLib",
                    ":lib:compileKotlinJsLib",
                    ":lib:compileCommonMainKotlinMetadata",
                    ":app:compileDebugKotlinAndroidApp",
                    ":app:compileReleaseKotlinAndroidApp",
                    ":app:compileKotlinJvmApp",
                    ":app:compileKotlinJsApp",
                    ":app:compileCommonMainKotlinMetadata",
                    ":lib:compileDebugUnitTestJavaWithJavac",
                    ":app:compileDebugUnitTestJavaWithJavac"
                )

                listOf("debug", "release").forEach { variant ->
                    assertFileInProjectExists("lib/build/tmp/kotlin-classes/$variant/com/example/lib/ExpectedLibClass.class")
                    assertFileInProjectExists("lib/build/tmp/kotlin-classes/$variant/com/example/lib/CommonLibClass.class")
                    assertFileInProjectExists("lib/build/tmp/kotlin-classes/$variant/com/example/lib/AndroidLibClass.class")

                    assertFileInProjectExists("app/build/tmp/kotlin-classes/$variant/com/example/app/AKt.class")
                    assertFileInProjectExists("app/build/tmp/kotlin-classes/$variant/com/example/app/KtUsageKt.class")
                }
            }
        }
    }

    @DisplayName("KT-29343: mpp source set dependencies are propagated to android tests")
    @GradleAndroidTest
    fun testAndroidMppProductionDependenciesInTests(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "new-mpp-android",
            gradleVersion,
            buildOptions = defaultBuildOptions
                .copy(androidVersion = agpVersion)
                // `resolveAllConfigurations` task is not compatible with CC and isolated projects
                .disableIsolatedProjects(),
            buildJdk = jdkVersion.location
        ) {
            // Test the fix for KT-29343
            subProject("lib").buildGradleKts.appendText(
                """

                kotlin.sourceSets {
                    commonMain {
                        dependencies {
                            implementation(kotlin("stdlib-common"))
                        }
                    }
                    val androidLibDebug by creating {
                        dependencies {
                            implementation(kotlin("reflect"))
                        }
                    }
                    val androidLibRelease by creating {
                        dependencies {
                            implementation(kotlin("test-junit"))
                        }
                    }
                }
                """.trimIndent()
            )

            val kotlinVersion = buildOptions.kotlinVersion
            testResolveAllConfigurations("lib") { _, buildResult ->
                // androidLibDebug:
                buildResult.assertOutputContains(">> :lib:debugCompileClasspath --> kotlin-reflect-$kotlinVersion.jar")
                buildResult.assertOutputDoesNotContain(">> :lib:releaseCompileClasspath --> kotlin-reflect-$kotlinVersion.jar")
                buildResult.assertOutputContains(">> :lib:debugAndroidTestCompileClasspath --> kotlin-reflect-$kotlinVersion.jar")
                buildResult.assertOutputContains(">> :lib:debugUnitTestCompileClasspath --> kotlin-reflect-$kotlinVersion.jar")
                buildResult.assertOutputDoesNotContain(">> :lib:releaseUnitTestCompileClasspath --> kotlin-reflect-$kotlinVersion.jar")

                // androidLibRelease:
                buildResult.assertOutputDoesNotContain(">> :lib:debugCompileClasspath --> kotlin-test-junit-$kotlinVersion.jar")
                buildResult.assertOutputContains(">> :lib:releaseCompileClasspath --> kotlin-test-junit-$kotlinVersion.jar")
                buildResult.assertOutputDoesNotContain(">> :lib:debugAndroidTestCompileClasspath --> kotlin-test-junit-$kotlinVersion.jar")
                buildResult.assertOutputDoesNotContain(">> :lib:debugUnitTestCompileClasspath --> kotlin-test-junit-$kotlinVersion.jar")
                buildResult.assertOutputContains(">> :lib:releaseUnitTestCompileClasspath --> kotlin-test-junit-$kotlinVersion.jar")
            }
        }
    }

    @DisplayName("KT-27714: custom attributes are copied to android compilation configurations")
    @GradleAndroidTest
    fun testCustomAttributesInAndroidTargets(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "new-mpp-android",
            gradleVersion,
            buildOptions = defaultBuildOptions
                .copy(androidVersion = agpVersion)
                // KT-75899 Support Gradle Project Isolation in KGP JS & Wasm
                .disableIsolatedProjectsBecauseOfJsAndWasmKT75899(),
            buildJdk = jdkVersion.location
        ) {
            val libBuildScript = subProject("lib").buildGradleKts
            val appBuildScript = subProject("app").buildGradleKts

            // Enable publishing for all Android variants:
            libBuildScript.appendText(
                """

                kotlin.androidTarget("androidLib") { publishAllLibraryVariants() }
                """.trimIndent()
            )

            val groupDir = subProject("lib").projectPath.resolve("build/repo/com/example")

            build("publish") {
                // Also check that custom user-specified attributes are written in all Android modules metadata:
                assertFileContains(
                    groupDir.resolve("lib-androidlib/1.0/lib-androidlib-1.0.module"),
                    "\"com.example.target\": \"androidLib\"",
                    "\"com.example.compilation\": \"release\""
                )
                assertFileContains(
                    groupDir.resolve("lib-androidlib-debug/1.0/lib-androidlib-debug-1.0.module"),
                    "\"com.example.target\": \"androidLib\"",
                    "\"com.example.compilation\": \"debug\""
                )
            }
            groupDir.deleteRecursively()

            // Check that the consumer side uses custom attributes specified in the target and compilations:
            val appBuildScriptBackup = appBuildScript.readText()
            val libBuildScriptBackup = libBuildScript.readText()

            libBuildScript.appendText(
                """

                kotlin.targets.all { 
                    attributes.attribute(
                        Attribute.of("com.example.target", String::class.java),
                        targetName
                    )
                }
                """.trimIndent()
            )
            appBuildScript.appendText(
                """

                kotlin.targets.getByName("androidApp").attributes.attribute(
                    Attribute.of("com.example.target", String::class.java),
                    "notAndroidLib"
                )
                """.trimIndent()
            )

            buildAndFail(":app:compileDebugKotlinAndroidApp") {
                // dependency resolution should fail
                assertOutputContainsAny(
                    "Required com.example.target 'notAndroidLib'",
                    "attribute 'com.example.target' with value 'notAndroidLib'",
                )
            }

            libBuildScript.writeText(
                libBuildScriptBackup +
                        """

                        kotlin.targets.all {
                            compilations.all {
                                attributes.attribute(
                                    Attribute.of("com.example.compilation", String::class.java),
                                    target.name + compilationName.capitalize()
                                )
                            }
                        }
                        """.trimIndent()
            )
            appBuildScript.writeText(
                appBuildScriptBackup +
                        """

                        kotlin.targets.getByName("androidApp").compilations.all {
                            attributes.attribute(
                                Attribute.of("com.example.compilation", String::class.java),
                                "notDebug"
                            )
                        }
                        """.trimIndent()
            )

            buildAndFail(":app:compileDebugKotlinAndroidApp") {
                assertOutputContainsAny(
                    "Required com.example.compilation 'notDebug'",
                    "attribute 'com.example.compilation' with value 'notDebug'",
                )
            }
        }
    }

    @DisplayName("KT-49877, KT-35916: associate compilation dependencies are passed correctly to android test compilations")
    @GradleAndroidTest
    fun testAssociateCompilationDependenciesArePassedToAndroidTestCompilations(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "kt-49877",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location
        ) {
            build(
                ":compileDebugUnitTestKotlinAndroid",
                ":compileReleaseUnitTestKotlinAndroid",
            ) {
                assertTasksExecuted(
                    ":compileDebugKotlinAndroid",
                    ":compileReleaseKotlinAndroid",
                    ":compileDebugUnitTestKotlinAndroid",
                    ":compileReleaseUnitTestKotlinAndroid",
                )
            }

            // instrumented tests don't work without a device, so we only compile them
            build("packageDebugAndroidTest") {
                assertTasksExecuted(
                    ":compileDebugAndroidTestKotlinAndroid",
                )
            }
        }
    }

    @DisplayName("KT-63753: K2 File \"does not belong to any module\" when it is generated by `registerJavaGeneratingTask` in AGP")
    @GradleAndroidTest
    fun sourceGenerationTaskAddedToAndroidVariant(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "new-mpp-android", gradleVersion,
            defaultBuildOptions
                .copy(androidVersion = agpVersion)
                // KT-75899 Support Gradle Project Isolation in KGP JS & Wasm
                .disableIsolatedProjectsBecauseOfJsAndWasmKT75899(),
            buildJdk = jdkVersion.location
        ) {
            // Code copied from the reproducer from KT-63753
            subProject("app").buildGradleKts.appendText(
                """
                    
                    abstract class FileGeneratingTask : DefaultTask() {
                        @get:OutputDirectory
                        abstract val outputDir: DirectoryProperty

                        @TaskAction
                        fun taskAction() {
                            val outputDirFile = outputDir.asFile.get()
                            outputDirFile.mkdirs()
                            val file = File(outputDirFile, "Test.kt")
                            val text = ""${'"'}
                                val hello = "World!"
                            ""${'"'}
                            file.writeText(text)
                        }
                    }
                    
                    android {
                        applicationVariants.configureEach {
                            val variant = this
                            val outputDir = File(buildDir, "generateExternalFile/${'$'}{variant.dirName}")
                            val task = project.tasks.register("generateExternalFile${'$'}{variant.name.capitalize()}", FileGeneratingTask::class.java) {
                                this.outputDir.set(outputDir)
                            }
                            variant.registerJavaGeneratingTask(task, outputDir)
                        }                    
                    }
                """.trimIndent()
            )
            build(":app:compileDebugKotlinAndroidApp") {
                assertTasksExecuted(":app:compileDebugKotlinAndroidApp")
            }
        }
    }
}
