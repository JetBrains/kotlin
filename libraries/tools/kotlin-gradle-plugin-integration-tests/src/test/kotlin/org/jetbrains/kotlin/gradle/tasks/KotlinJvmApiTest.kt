/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.appendText
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively
import kotlin.io.path.writeText

@DisplayName("JVM API validation")
class KotlinJvmApiTest : KGPBaseTest() {
    @DisplayName("Kotlin compilation can be set up using APIs")
    @JvmGradlePluginTests
    @GradleTest
    internal fun kotlinCompilationShouldRunIfSetUpWithApi(gradleVersion: GradleVersion) {
        project(
            projectName = "kotlinJavaProject",
            gradleVersion = gradleVersion
        ) {
            projectPath.resolve("src").let {
                it.deleteRecursively()
                it.resolve("main").createDirectories()
                it.resolve("main/foo.kt").writeText(
                    """
                    class Foo
                    """.trimIndent()
                )
            }

            buildGradle.modify {
                it.replace("id 'org.jetbrains.kotlin.jvm'", "id 'org.jetbrains.kotlin.jvm' apply false") +
                        """
                        import org.jetbrains.kotlin.gradle.plugin.KotlinApiPlugin
                        KotlinApiPlugin apiPlugin = plugins.apply(KotlinApiPlugin.class)
                                                
                        apiPlugin.registerKotlinJvmCompileTask("foo").configure {
                            it.source("src/main")
                            it.multiPlatformEnabled.set(false)
                            it.moduleName.set("main")
                            it.ownModuleName.set("main")
                            it.sourceSetName.set("main")
                            it.useModuleDetection.set(false)
                            it.destinationDirectory.fileValue(new File(project.buildDir, "fooOutput"))
                        }
                        """.trimIndent()
            }

            val expectedOutput = projectPath.resolve("build/fooOutput/Foo.class")

            build("foo") {
                assertFileExists(expectedOutput)
            }
        }
    }


    @DisplayName("KT-60541: checks that configuring custom KotlinCompile does not require using internals")
    @JvmGradlePluginTests
    @GradleTest
    internal fun kotlinCompileCustomTask(gradleVersion: GradleVersion) {
        project(projectName = "jvm-with-common", gradleVersion = gradleVersion) {

            val customModuleName = "customModuleName"
            val customTaskName = "customTask"
            val outputDirName = "customTaskOutput"
            buildGradleKts.appendText(
                """
                val compileKotlin = tasks.getByName("compileKotlinJvm") as KotlinCompile

                apply<KotlinBaseApiPlugin>()

                val myCustomTask = plugins
                    .findPlugin(KotlinBaseApiPlugin::class)!!
                    .registerKotlinJvmCompileTask("$customTaskName", moduleName = "$customModuleName")
                    
                myCustomTask {
                    source("src/jvmMain", "src/commonMain")
                    libraries.from(compileKotlin.libraries)
                    useModuleDetection.set(false)
                    multiPlatformEnabled.set(false)
                    destinationDirectory.set(File(project.buildDir, "$outputDirName"))
                }
                """.trimIndent()
            )

            build(customTaskName) {
                assertOutputDoesNotContain("No value has been specified for property 'ownModuleName'")
                assertFileInProjectExists("build/$outputDirName/org/example/application/MainKt.class")
                assertFileInProjectExists("build/$outputDirName/org/example/Lib.class")
                assertFileInProjectExists("build/$outputDirName/META-INF/$customModuleName.kotlin_module")
            }
        }
    }

    @DisplayName("KAPT can be set up using APIs")
    @OtherGradlePluginTests
    @GradleTestVersions(minVersion = TestVersions.Gradle.G_7_0)
    @GradleTest
    internal fun kaptShouldRunIfSetUpWithApi(gradleVersion: GradleVersion) {
        project(
            projectName = "kotlinJavaProject",
            gradleVersion = gradleVersion
        ) {
            projectPath.resolve("src").let {
                it.deleteRecursively()
                it.resolve("main").createDirectories()
                it.resolve("main/foo.kt").writeText(
                    """
                    class Foo
                    """.trimIndent()
                )
            }

            buildGradle.modify {
                it.replace("id 'org.jetbrains.kotlin.jvm'", "id 'org.jetbrains.kotlin.jvm' apply false") +
                        """
                        import org.jetbrains.kotlin.gradle.plugin.KotlinApiPlugin
                        KotlinApiPlugin apiPlugin = plugins.apply(KotlinApiPlugin.class)
                        
                        File kaptFakeJar = new File(project.projectDir, "kapt.jar")
                        kaptFakeJar.createNewFile()
                        
                        apiPlugin.addCompilerPluginDependency(
                            project.provider {
                                "org.jetbrains.kotlin:kotlin-annotation-processing-embeddable:${"$"}kotlin_version"
                            }
                        )
                        
                        apiPlugin.registerKaptGenerateStubsTask("foo").configure {
                            it.source("src/main")
                            it.multiPlatformEnabled.set(false)
                            it.moduleName.set("main")
                            it.ownModuleName.set("main")
                            it.sourceSetName.set("main")
                            it.useModuleDetection.set(false)
                            it.destinationDirectory.fileValue(new File(project.buildDir, "fooOutput"))
                            it.stubsDir.fileValue(new File(project.buildDir, "fooOutputStubs"))
                            it.kaptClasspath.from(kaptFakeJar)
                            it.pluginClasspath.from(apiPlugin.getCompilerPlugins())
                        }
                        """.trimIndent()
            }

            val expectedOutputClass = projectPath.resolve("build/fooOutput/Foo.class")
            val expectedOutputStubs = listOf(
                projectPath.resolve("build/fooOutputStubs/Foo.java"),
                projectPath.resolve("build/fooOutputStubs/Foo.kapt_metadata"),
                projectPath.resolve("build/fooOutputStubs/error/NonExistentClass.java")
            )

            build("foo") {
                assertFileExists(expectedOutputClass)
                expectedOutputStubs.forEach { assertFileExists(it) }
            }
        }
    }
}