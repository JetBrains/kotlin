/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.js

import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalJsTestDsl
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject
import org.jetbrains.kotlin.gradle.targets.js.npm.fromSrcPackageJson
import org.jetbrains.kotlin.gradle.targets.web.npm.KotlinSharedNpmProjectPlugin
import org.jetbrains.kotlin.gradle.testbase.GradleTest
import org.jetbrains.kotlin.gradle.testbase.GradleTestVersions
import org.jetbrains.kotlin.gradle.testbase.JsGradlePluginTests
import org.jetbrains.kotlin.gradle.testbase.KGPBaseTest
import org.jetbrains.kotlin.gradle.testbase.TestVersions
import org.jetbrains.kotlin.gradle.testbase.assertTasksExecuted
import org.jetbrains.kotlin.gradle.testbase.build
import org.jetbrains.kotlin.gradle.testbase.buildScriptInjection
import org.jetbrains.kotlin.gradle.testbase.compileSource
import org.jetbrains.kotlin.gradle.testbase.enableIsolatedProjects
import org.jetbrains.kotlin.gradle.testbase.enableNodeJsToolchain
import org.jetbrains.kotlin.gradle.testbase.enableNpmResolution
import org.jetbrains.kotlin.gradle.testbase.plugins
import org.jetbrains.kotlin.gradle.testbase.project
import org.jetbrains.kotlin.gradle.testbase.TestProject
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.gradle.uklibs.include
import org.junit.jupiter.api.io.CleanupMode
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.test.assertTrue

@OptIn(ExperimentalKotlinGradlePluginApi::class)
@GradleTestVersions(
    minVersion = TestVersions.Gradle.MAX_SUPPORTED
)
@JsGradlePluginTests
class JsProjectIsolationIT : KGPBaseTest() {

    @Suppress("LateinitVarOverridesLateinitVar")
    @TempDir(cleanup = CleanupMode.NEVER)
    override lateinit var workingDir: Path

    @GradleTest
    fun testProjectIsolation(gradleVersion: GradleVersion) {
        project(
            "empty",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions
                .enableIsolatedProjects()
                .enableNpmResolution()
                .enableNodeJsToolchain()
        ) {
            val subproject = project("empty", gradleVersion = gradleVersion) {
                plugins {
                    kotlin("multiplatform")
                }
                buildScriptInjection {
                    project.applyMultiplatform {
                        js {
                            nodejs()
                        }

                        jvm()

                        sourceSets.commonMain.get().compileSource(
                            """
                                fun subproject() = ":)"
                        """.trimIndent())
                    }
                }
            }
            include(subproject, "subproject")

            plugins {
                kotlin("multiplatform")
            }

            buildScriptInjection {
                project.plugins.apply(KotlinSharedNpmProjectPlugin::class.java)
                project.applyMultiplatform {
                    js {
                        nodejs()
                    }

                    jvm()

                    sourceSets.commonMain {
                        dependencies {
                            implementation(project(":subproject"))
                        }
                    }

                    sourceSets.commonTest {
                        dependencies {
                            implementation(kotlin("test"))
                        }
                    }

                    sourceSets.commonTest.get().compileSource(
                        """
                        import kotlin.test.*
                        
                        class NodeJsSmokeTest {
                            @Test
                            fun assertOk() {
                                assertEquals(":)", subproject())
                            }
                        }
                        """.trimIndent()
                    )
                }
            }

            build("jsNodeTest", forwardBuildOutput = true) {
                assertTasksExecuted(":jsNodeTest")
            }
        }
    }

    @GradleTest
    fun testProjectIsolationInSubprojects(gradleVersion: GradleVersion) {
        project(
            "empty",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions
                .enableIsolatedProjects()
                .enableNpmResolution()
                .enableNodeJsToolchain()
        ) {
            val subproject2 = project("empty", gradleVersion = gradleVersion) {
                plugins {
                    kotlin("multiplatform")
                }
                buildScriptInjection {
                    project.applyMultiplatform {
                        js {
                            nodejs()
                            // Required by the '@JsModule' declaration below
                            useCommonJs()
                        }

                        sourceSets.getByName("jsMain").dependencies {
                            implementation(npm("is-odd", "3.0.1"))
                        }

                        sourceSets.commonMain.get().compileSource(
                            """
                            fun subproject2() = ":)"
                            """.trimIndent()
                        )

                        // The npm module is loaded by the JS runtime, so it only resolves when the compiled
                        // files are executed in the context of the npm workspace of the compilation.
                        sourceSets.getByName("jsMain").compileSource(
                            """
                            @JsModule("is-odd")
                            external fun isOdd(value: Int): Boolean
                            """.trimIndent()
                        )

                        sourceSets.commonTest {
                            dependencies {
                                implementation(kotlin("test"))
                            }
                        }

                        sourceSets.commonTest.get().compileSource(
                            """
                            import kotlin.test.*
                            
                            class Subproject2SmokeTest {
                                @Test
                                fun assertOk() {
                                    assertEquals(":)", subproject2())
                                }
                            }
                            """.trimIndent()
                        )

                        sourceSets.getByName("jsTest").compileSource(
                            """
                            import kotlin.test.*
                            
                            class Subproject2NpmModuleTest {
                                @Test
                                fun npmModuleIsResolved() {
                                    assertTrue(isOdd(3))
                                }
                            }
                            """.trimIndent()
                        )
                    }
                }
            }
            include(subproject2, "subproject2")

            val subproject1 = project("empty", gradleVersion = gradleVersion) {
                plugins {
                    kotlin("multiplatform")
                }
                buildScriptInjection {
                    project.applyMultiplatform {
                        js {
                            nodejs()
                            // Required by the '@JsModule' declaration of ':subproject2'
                            useCommonJs()
                        }

                        sourceSets.commonMain {
                            dependencies {
                                implementation(project(":subproject2"))
                            }
                        }

                        sourceSets.commonTest {
                            dependencies {
                                implementation(kotlin("test"))
                            }
                        }

                        sourceSets.commonTest.get().compileSource(
                            """
                            import kotlin.test.*
                            
                            class NodeJsSmokeTest {
                                @Test
                                fun assertOk() {
                                    assertEquals(":)", subproject2())
                                }
                            }
                            """.trimIndent()
                        )

                        // The npm dependency of ':subproject2' must also be resolvable from the npm workspace
                        // of this project, which depends on it.
                        sourceSets.getByName("jsTest").compileSource(
                            """
                            import kotlin.test.*
                            
                            class Subproject1NpmModuleTest {
                                @Test
                                fun npmModuleIsResolved() {
                                    assertTrue(isOdd(3))
                                }
                            }
                            """.trimIndent()
                        )
                    }
                }
            }
            include(subproject1, "subproject1")

            // The project that assembles the shared npm project does not have to have a Kotlin target itself
            plugins {
                kotlin("multiplatform").apply(false)
            }

            buildScriptInjection {
                project.plugins.apply(KotlinSharedNpmProjectPlugin::class.java)
            }

            build(":subproject1:jsNodeTest", ":subproject2:jsNodeTest", forwardBuildOutput = true) {
                assertTasksExecuted(":isolatedNpmInstall", ":subproject1:jsNodeTest", ":subproject2:jsNodeTest")
            }

            // The npm dependency declared on the 'jsMain' source set of ':subproject2' must be visible to
            // the 'jsTest' compilation of the same project, because 'jsTest' is associated with 'jsMain',
            // and to both compilations of ':subproject1', which depends on ':subproject2'.
            assertPackageJsonsContainNpmDependency("subproject2", "is-odd")
            assertPackageJsonsContainNpmDependency("subproject1", "is-odd")

            // The compiled JS files must be synced into the npm workspace of their compilation,
            // otherwise the JS runtime cannot resolve the installed npm dependencies.
            assertNpmWorkspaceContainsCompiledJsFiles("empty-subproject1-test")
            assertNpmWorkspaceContainsCompiledJsFiles("empty-subproject2-test")
        }
    }

    /**
     * Asserts that the npm workspace [npmProjectName] of the shared npm project contains the compiled JS files.
     */
    private fun TestProject.assertNpmWorkspaceContainsCompiledJsFiles(npmProjectName: String) {
        val distDirectory = projectPath
            .resolve("build/js/shared-npm-project/packages")
            .resolve(npmProjectName)
            .resolve(NpmProject.DIST_FOLDER)

        assertTrue(
            distDirectory.isDirectory() && distDirectory.listDirectoryEntries("*.js").isNotEmpty(),
            "Expected compiled JS files in $distDirectory"
        )
    }

    /**
     * Asserts that the `package.json` of every compilation of [subprojectName] declares the [npmDependency] package.
     */
    private fun TestProject.assertPackageJsonsContainNpmDependency(subprojectName: String, npmDependency: String) {
        val packagesDirectory = projectPath.resolve(subprojectName).resolve("build/js/packages")

        val packageJsons = packagesDirectory.listDirectoryEntries()
            .map { it.resolve(NpmProject.PACKAGE_JSON) }

        assertTrue(
            packageJsons.size >= 2,
            "Expected the main and the test package.json in $packagesDirectory, but got $packageJsons"
        )

        packageJsons.forEach { packageJson ->
            val dependencies = fromSrcPackageJson(packageJson.toFile())?.dependencies.orEmpty()
            assertTrue(
                npmDependency in dependencies,
                "Expected '$npmDependency' in the dependencies of $packageJson, but got $dependencies"
            )
        }
    }
}
