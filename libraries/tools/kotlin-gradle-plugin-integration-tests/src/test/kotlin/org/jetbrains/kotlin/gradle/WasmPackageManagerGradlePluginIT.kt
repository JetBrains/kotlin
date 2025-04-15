/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.tasks.Exec
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.targets.js.npm.LockCopyTask
import org.jetbrains.kotlin.gradle.targets.js.npm.LockCopyTask.Companion.PACKAGE_LOCK
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNpmTooling
import org.jetbrains.kotlin.gradle.targets.wasm.npm.WasmNpmExtension
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnRootEnvSpec
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName
import java.io.File

class WasmNpmGradlePluginIT : WasmPackageManagerGradlePluginIT() {
    override val yarn: Boolean = false

    override val lockFileName: String = PACKAGE_LOCK

    override val toolingCustomDir: String
        get() = "npm"
}

class WasmYarnGradlePluginIT : WasmPackageManagerGradlePluginIT() {
    override val yarn: Boolean = true

    override val lockFileName: String = LockCopyTask.YARN_LOCK

    override val toolingCustomDir: String
        get() = "yarn"
}

@MppGradlePluginTests
abstract class WasmPackageManagerGradlePluginIT : KGPBaseTest() {

    abstract val yarn: Boolean

    abstract val lockFileName: String

    abstract val toolingCustomDir: String

    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.copy(
            jsOptions = super.defaultBuildOptions.jsOptions?.copy(
                yarn = yarn
            ),
            // KT-75899 Support Gradle Project Isolation in KGP JS & Wasm
            isolatedProjects = BuildOptions.IsolatedProjectsMode.DISABLED,
        )

    @DisplayName("Check NPM dependencies not installed for empty project")
    @GradleTest
    @TestMetadata("kotlin-wasm-package-lock-project")
    fun testWasmInstallWithoutTooling(gradleVersion: GradleVersion) {
        project("kotlin-wasm-package-lock-project", gradleVersion) {
            build(":kotlinWasmNpmInstall") {
                assertTasksExecuted(":kotlinWasmNpmInstall")

                assertFilesContentEquals(projectPath.resolve(lockFileName), projectPath.resolve("build/wasm/$lockFileName"))
            }
        }
    }

    @DisplayName("Check NPM tooling dependencies installed")
    @GradleTest
    @TestMetadata("kotlin-wasm-package-lock-project")
    fun testWasmSetupTooling(gradleVersion: GradleVersion) {
        project("kotlin-wasm-package-lock-project", gradleVersion) {
            build(":kotlinWasmToolingSetup") {
                assertTasksExecuted(":kotlinWasmToolingSetup")
            }
        }
    }

    @DisplayName("Check NPM dependencies installed for external NPM dependency")
    @GradleTest
    @TestMetadata("kotlin-wasm-package-lock-project")
    fun testWasmInstallExternalNpmDependency(gradleVersion: GradleVersion) {
        project("kotlin-wasm-package-lock-project", gradleVersion) {
            buildGradleKts.modify {
                it + "\n" +
                        """
                        kotlin {
                            sourceSets {
                                wasmJsMain {
                                    dependencies {
                                        implementation(npm("onigasm", "2.2.5"))
                                    }
                                }
                            }
                        }
                        """.trimIndent()
            }

            build(":kotlinWasmNpmInstall") {
                assertTasksExecuted(":kotlinWasmNpmInstall")

                assertDirectoryExists(
                    projectPath.resolve("build/wasm/node_modules/onigasm")
                )

                assertDirectoryDoesNotExist(
                    projectPath.resolve("build/wasm/node_modules/webpack")
                )
            }
        }
    }

    @DisplayName("Check NPM dependencies pre installed in a directory")
    @GradleTest
    @TestMetadata("kotlin-wasm-tooling-inside-project")
    fun testWasmUsePredefinedTooling(gradleVersion: GradleVersion) {
        project("kotlin-wasm-tooling-inside-project", gradleVersion) {

            // extracted as val to be serializable
            val isYarn = yarn
            val toolingCustomDir = toolingCustomDir

            buildScriptInjection {
                project.rootProject.extensions.getByType(WasmNpmTooling::class.java).apply {
                    this.installationDir.fileValue(project.projectDir.resolve(toolingCustomDir))
                }

                project.tasks.register("toolingInstall", Exec::class.java).configure { task: Exec ->
                    val nodeJsEnvSpec = project.extensions.getByType(WasmNodeJsEnvSpec::class.java)

                    with(nodeJsEnvSpec) {
                        task.dependsOn(project.nodeJsSetupTaskProvider)
                    }
                    task.workingDir = project.projectDir.resolve(toolingCustomDir)

                    val nodejsExecutable = nodeJsEnvSpec.executable.get()

                    if (isYarn) {
                        task.executable(nodejsExecutable)
                        task.args(
                            listOf(project.rootProject.extensions.getByType(WasmYarnRootEnvSpec::class.java).executable.get())
                        )
                    } else {
                        task.executable(
                            project.rootProject.extensions.getByType(WasmNpmExtension::class.java).requireConfigured().executable
                        )
                        task.args("install", "--ignore-scripts")

                        val nodePath = File(nodejsExecutable).parent
                        task.environment["PATH"] =
                            "$nodePath${File.pathSeparator}${System.getenv("PATH")}"
                    }
                }
            }

            build(":toolingInstall") {
                assertTasksExecuted(":toolingInstall")

                assertDirectoryExists(projectPath.resolve("$toolingCustomDir/node_modules"))
            }

            build("build") {
                assertTasksExecuted(":wasmJsBrowserDistribution")

                assertFileExists(projectPath.resolve("build/dist/wasmJs/productionExecutable/kotlin-wasm-tooling-inside-project.js"))
            }
        }
    }

    @DisplayName("Setup tooling for wasm browser tasks")
    @GradleTest
    @TestMetadata("kotlin-wasm-package-lock-project")
    fun testWasmInstallToolingBeforeTests(gradleVersion: GradleVersion) {
        project("kotlin-wasm-package-lock-project", gradleVersion) {
            build(":wasmJsBrowserTest") {
                assertTasksExecuted(":kotlinWasmToolingSetup")
            }
        }
    }
}
