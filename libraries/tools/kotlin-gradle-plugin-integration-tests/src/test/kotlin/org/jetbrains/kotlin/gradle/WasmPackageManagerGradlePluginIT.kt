/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.targets.js.npm.LockCopyTask
import org.jetbrains.kotlin.gradle.targets.js.npm.LockCopyTask.Companion.KOTLIN_JS_STORE
import org.jetbrains.kotlin.gradle.targets.js.npm.LockCopyTask.Companion.PACKAGE_LOCK
import org.jetbrains.kotlin.gradle.targets.js.npm.LockCopyTask.Companion.PACKAGE_LOCK_MISMATCH_MESSAGE
import org.jetbrains.kotlin.gradle.targets.js.npm.LockCopyTask.Companion.RESTORE_PACKAGE_LOCK_NAME
import org.jetbrains.kotlin.gradle.targets.js.npm.LockCopyTask.Companion.STORE_PACKAGE_LOCK_NAME
import org.jetbrains.kotlin.gradle.targets.js.npm.LockCopyTask.Companion.UPGRADE_PACKAGE_LOCK
import org.jetbrains.kotlin.gradle.targets.js.npm.fromSrcPackageJson
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.testbase.assertFilesContentEquals
import org.jetbrains.kotlin.gradle.testbase.modify
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import kotlin.io.path.deleteRecursively
import kotlin.io.path.notExists
import kotlin.io.path.readText
import kotlin.test.assertEquals
import kotlin.text.trimIndent

class WasmNpmGradlePluginIT : WasmPackageManagerGradlePluginIT() {
    override val yarn: Boolean = false

    override val lockFileName: String = PACKAGE_LOCK
}

class WasmYarnGradlePluginIT : WasmPackageManagerGradlePluginIT() {
    override val yarn: Boolean = true

    override val lockFileName: String = LockCopyTask.YARN_LOCK
}

@MppGradlePluginTests
abstract class WasmPackageManagerGradlePluginIT : KGPBaseTest() {

    abstract val yarn: Boolean

    abstract val lockFileName: String

    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.copy(
            jsOptions = super.defaultBuildOptions.jsOptions?.copy(
                yarn = yarn
            )
        )

    @DisplayName("Check NPM dependencies not installed for empty project")
    @GradleTest
    @TestMetadata("kotlin-wasm-package-lock-project")
    fun testWasmInstallWithoutTooling(gradleVersion: GradleVersion) {
        project("kotlin-wasm-package-lock-project", gradleVersion) {
            build(":wasmKotlinNpmInstall") {
                assertTasksExecuted(":wasmKotlinNpmInstall")

                assertFilesContentEquals(projectPath.resolve(lockFileName), projectPath.resolve("build/wasm/$lockFileName"))
            }
        }
    }

    @DisplayName("Check NPM tooling dependencies installed")
    @GradleTest
    @TestMetadata("kotlin-wasm-package-lock-project")
    fun testWasmInstallTooling(gradleVersion: GradleVersion) {
        project("kotlin-wasm-package-lock-project", gradleVersion) {
            build(":wasmKotlinToolingInstall") {
                assertTasksExecuted(":wasmKotlinToolingInstall")
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

            build(":wasmKotlinNpmInstall") {
                assertTasksExecuted(":wasmKotlinNpmInstall")

                assertDirectoryExists(
                    projectPath.resolve("build/wasm/node_modules/onigasm")
                )

                assertDirectoryDoesNotExist(
                    projectPath.resolve("build/wasm/node_modules/webpack")
                )
            }
        }
    }
}
