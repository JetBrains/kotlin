/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.testbase.buildScriptBuildscriptBlockInjection
import org.jetbrains.kotlin.gradle.uklibs.applyJvm
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.kotlin.testFederation.AffectedByCompilerPlugins
import org.junit.jupiter.api.DisplayName

@DisplayName("Kapt with compiler plugins")
@OtherGradlePluginTests
@AffectedByCompilerPlugins
class KaptCompilerPluginsIT : KaptBaseIT() {
    override val defaultBuildOptions: BuildOptions =
        super.defaultBuildOptions.copyEnsuringK2()

    @DisplayName("K2 kapt stubs use kotlin.jvm.functions.Function0 instead of compiler plugin function kinds")
    @GradleTest
    @TestMetadata("kapt2/compilerPluginFunctionKind")
    fun testFunctionTypeKindCompilerPluginInKapt(gradleVersion: GradleVersion) {
        val projectName = "compilerPluginFunctionKind".withPrefix
        val buildOptions = defaultBuildOptions.copy(
            // KT-76289 KAPT does not support isolated projects
            isolatedProjects = BuildOptions.IsolatedProjectsMode.DISABLED
        )
        val kotlinVersion = buildOptions.kotlinVersion

        project(
            projectName,
            gradleVersion,
            buildOptions = buildOptions,
        ) {
            transferPluginRepositoriesIntoBuildScript()

            for (subproject in subprojects("plugin", "annotation-processor", "example")) {
                subproject.buildScriptBuildscriptBlockInjection {
                    buildscript.configurations.getByName("classpath").dependencies.add(
                        buildscript.dependencies.create("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
                    )
                }
            }

            subProject("plugin").buildScriptInjection {
                project.applyJvm {
                    jvmToolchain(8)
                    compilerOptions.jvmTarget.set(JvmTarget.JVM_1_8)
                    compilerOptions.freeCompilerArgs.add("-opt-in=org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")
                }

                dependencies.add("implementation", "org.jetbrains.kotlin:kotlin-gradle-plugin-api:$kotlinVersion")
                dependencies.add("compileOnly", "org.jetbrains.kotlin:kotlin-compiler-embeddable:$kotlinVersion")
            }

            subProject("annotation-processor").buildScriptInjection {
                project.applyJvm {
                    jvmToolchain(8)
                }

                dependencies.add("implementation", "org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
            }

            subProject("example").buildScriptInjection {
                project.applyJvm {
                    jvmToolchain(8)
                }
                project.plugins.apply("org.jetbrains.kotlin.kapt")

                dependencies.add("implementation", "org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
                dependencies.add("implementation", dependencies.project(mapOf("path" to ":annotation-processor")))
                dependencies.add("kapt", dependencies.project(mapOf("path" to ":annotation-processor")))
                dependencies.add("kotlinCompilerPluginClasspath", dependencies.project(mapOf("path" to ":plugin")))
            }

            build(":example:kaptGenerateStubsKotlin") {
                assertTasksExecuted(":example:kaptGenerateStubsKotlin")

                val interfaceStub = "example/build/tmp/kapt3/stubs/main/repro/TestInterface.java"
                assertFileInProjectDoesNotContain(interfaceStub, "repro.internal.PluginFunction0<kotlin.Unit>")
                assertFileInProjectContains(interfaceStub, "kotlin.jvm.functions.Function0<kotlin.Unit> block")
                assertFileInProjectContains(
                    interfaceStub,
                    "java.util.List<? extends kotlin.jvm.functions.Function0<kotlin.Unit>> blocks",
                )
                assertFileInProjectContains(interfaceStub, "kotlin.jvm.functions.Function0<kotlin.Unit> testReturn()")
                assertFileInProjectContains(
                    interfaceStub,
                    "java.util.List<kotlin.jvm.functions.Function0<kotlin.Unit>> testListReturn()",
                )

                val classStub = "example/build/tmp/kapt3/stubs/main/repro/TestClass.java"
                assertFileInProjectDoesNotContain(classStub, "repro.internal.PluginFunction0<kotlin.Unit>")
                assertFileInProjectContains(classStub, "private final kotlin.jvm.functions.Function0<kotlin.Unit> directProperty")
                assertFileInProjectContains(
                    classStub,
                    "private final java.util.List<kotlin.jvm.functions.Function0<kotlin.Unit>> listProperty",
                )
                assertFileInProjectContains(classStub, "private kotlin.jvm.functions.Function0<kotlin.Unit> mutableDirectProperty")
                assertFileInProjectContains(
                    classStub,
                    "private java.util.List<? extends kotlin.jvm.functions.Function0<kotlin.Unit>> mutableListProperty",
                )
                assertFileInProjectContains(classStub, "kotlin.jvm.functions.Function0<kotlin.Unit> getDirectProperty()")
                assertFileInProjectContains(
                    classStub,
                    "java.util.List<kotlin.jvm.functions.Function0<kotlin.Unit>> getListProperty()",
                )
                assertFileInProjectContains(classStub, "kotlin.jvm.functions.Function0<kotlin.Unit> p0")
                assertFileInProjectContains(
                    classStub,
                    "java.util.List<? extends kotlin.jvm.functions.Function0<kotlin.Unit>> p0",
                )
            }
        }
    }
}
