/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalSerializationApi::class)

package org.jetbrains.kotlin.gradle

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.targets.js.npm.LockCopyTask.Companion.KOTLIN_JS_STORE
import org.jetbrains.kotlin.gradle.targets.js.npm.LockCopyTask.Companion.PACKAGE_LOCK
import org.jetbrains.kotlin.gradle.targets.js.npm.LockCopyTask.Companion.YARN_LOCK
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.kgpPackageLockJsonFileContent
import org.jetbrains.kotlin.gradle.util.kgpYarnLockFileContent
import org.jetbrains.kotlin.gradle.util.replaceText
import org.jetbrains.kotlin.gradle.util.setupCustomKgpNpmToolingDependenciesDir
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.assertEquals

class WasmNpmGradlePluginIT : WasmPackageManagerGradlePluginIT() {
    override val yarn: Boolean = false

    override val lockFileName: String = PACKAGE_LOCK

    override val toolingCustomDirName: String
        get() = "npm"
}

class WasmYarnGradlePluginIT : WasmPackageManagerGradlePluginIT() {
    override val yarn: Boolean = true

    override val lockFileName: String = YARN_LOCK

    override val toolingCustomDirName: String
        get() = "yarn"

    @GradleTest
    @MppGradlePluginTests
    @TestMetadata("kotlin-wasm-package-lock-project")
    fun `given project without npm dependencies - when lockfile tasks run - expect tasks succeed - and do not create yarn lock file`(
        gradleVersion: GradleVersion,
    ) {
        project("kotlin-wasm-package-lock-project", gradleVersion) {
            build(":kotlinWasmStoreYarnLock") {
                assertTasksExecuted(":kotlinWasmStoreYarnLock")
                assertFileNotExists(defaultYarnLockFile)
                assertOutputContains("[:kotlinWasmStoreYarnLock] No NPM dependencies detected, and stored lockfile does not exist - skipping copy")
            }
            build(":kotlinWasmUpgradeYarnLock") {
                assertTasksExecuted(":kotlinWasmUpgradeYarnLock")
                assertFileNotExists(defaultYarnLockFile)
            }
        }
    }

    @GradleTest
    @MppGradlePluginTests
    @TestMetadata("kotlin-wasm-package-lock-project")
    fun `given project with npm dependency - when 'store lockfile' task runs - expect task succeeds - and creates yarn lock file`(
        gradleVersion: GradleVersion,
    ) {
        project("kotlin-wasm-package-lock-project", gradleVersion) {
            assertFileNotExists(defaultYarnLockFile)

            addWasmJsNpmDependency()

            build(":kotlinWasmStoreYarnLock") {
                assertTasksExecuted(":kotlinWasmStoreYarnLock")
                assertFileExists(defaultYarnLockFile)
            }
        }
    }

    @GradleTest
    @MppGradlePluginTests
    @TestMetadata("kotlin-wasm-package-lock-project")
    fun `given project with npm dependency and non-empty lock file - when npm dependency is removed - expect KGP deletes empty lock file`(
        gradleVersion: GradleVersion,
    ) {
        project("kotlin-wasm-package-lock-project", gradleVersion) {

            // First, store a valid yarn lock file with an NPM dependency
            addWasmJsNpmDependency()
            build(":kotlinWasmStoreYarnLock") {
                assertTasksExecuted(":kotlinWasmStoreYarnLock")
                assertFileExists(defaultYarnLockFile)
            }

            // next, run without the npm dependency
            // expect failure, because the new lockfile is empty but the stored one is not
            removeWasmJsNpmDependency()
            buildAndFail(":kotlinWasmStoreYarnLock") {
                assertFileExists(defaultYarnLockFile)
                assertFileContains(defaultYarnLockFile, "decamelize@6.0.0:")
            }

            // updating the lockfile results in deletion of the stored file
            build(":kotlinWasmUpgradeYarnLock") {
                assertTasksExecuted(":kotlinWasmUpgradeYarnLock")
                assertOutputContains("[:kotlinWasmUpgradeYarnLock] No NPM dependencies detected. Deleting empty yarn.lock file")
                assertFileNotExists(defaultYarnLockFile)
            }
        }
    }

    /**
     * Validate the behaviour should a user manually create an empty `yarn.lock` file.
     * We may want to change this in future, to allow users to manually define an empty lockfile.
     */
    @GradleTest
    @MppGradlePluginTests
    @TestMetadata("kotlin-wasm-package-lock-project")
    fun `given project with npm dependency and empty lock file - when npm dependency is removed - expect KGP deletes empty lock file`(
        gradleVersion: GradleVersion,
    ) {
        val emptyYarnLockFileContent =
            """
            |# THIS IS AN AUTOGENERATED FILE. DO NOT EDIT THIS FILE DIRECTLY.
            |# yarn lockfile v1
            |
            |
            |""".trimMargin()

        project("kotlin-wasm-package-lock-project", gradleVersion) {

            // First, create a valid, but empty, lockfile
            defaultYarnLockFile.parent.createDirectories()
            defaultYarnLockFile.writeText(emptyYarnLockFileContent)

            // expect success, because both the new and stored lock files are empty
            build(":kotlinWasmStoreYarnLock") {
                assertFileExists(defaultYarnLockFile)
                assertEquals(
                    emptyYarnLockFileContent,
                    defaultYarnLockFile.readText(),
                    "Verify stored yarn.lock file is effectively empty (it should only contain comments)"
                )
            }

            // updating the lockfile results in deletion of the stored file
            build(":kotlinWasmUpgradeYarnLock") {
                assertTasksExecuted(":kotlinWasmUpgradeYarnLock")
                assertOutputContains("[:kotlinWasmUpgradeYarnLock] No NPM dependencies detected. Deleting empty yarn.lock file")
                assertFileNotExists(defaultYarnLockFile)
            }
        }
    }

    companion object {
        @Suppress("ConstPropertyName")
        private const val `implementation(npm(decamelize))` =
            """implementation(npm("decamelize", "6.0.0"))"""

        private fun TestProject.addWasmJsNpmDependency() {
            buildGradleKts.append(
                """
                kotlin {
                  sourceSets.wasmJsMain.dependencies {
                    $`implementation(npm(decamelize))`
                  }
                }
                """.trimIndent()
            )
        }

        private fun TestProject.removeWasmJsNpmDependency() {
            buildGradleKts.replaceText(`implementation(npm(decamelize))`, "")
        }

        private val TestProject.defaultYarnLockFile: Path
            get() = projectPath.resolve(KOTLIN_JS_STORE)
                .resolve("wasm")
                .resolve(YARN_LOCK)
    }
}

@MppGradlePluginTests
abstract class WasmPackageManagerGradlePluginIT : KGPBaseTest() {

    abstract val yarn: Boolean

    abstract val lockFileName: String

    /**
     * The name of the custom directory used to install KGP's npm tooling dependencies,
     * within the root project directory.
     */
    abstract val toolingCustomDirName: String

    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.copy(
            jsOptions = super.defaultBuildOptions.jsOptions?.copy(
                yarn = yarn
            ),
        ).disableIsolatedProjectsBecauseOfJsAndWasmKT75899()

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

            val toolingCustomDir = projectPath.resolve(toolingCustomDirName)

            setupCustomKgpNpmToolingDependenciesDir(
                toolingCustomDir = toolingCustomDir,
                useYarn = yarn,
            )

            build(":toolingInstall") {
                assertTasksExecuted(":toolingInstall")

                assertFileExists(toolingCustomDir.resolve("package.json"))
                assertDirectoryExists(toolingCustomDir.resolve("node_modules"))

                checkLockFiles(isYarn = yarn, toolingCustomDir = toolingCustomDir)
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

    @DisplayName("Npm Install in the project with braces in path")
    @GradleTest
    @TestMetadata("wasm-project-with-(braces)")
    fun testWasmNpmInstallInProjectWithBraces(gradleVersion: GradleVersion) {
        project("wasm-project-with-(braces)", gradleVersion) {
            build("build") {
                assertTasksExecuted(":kotlinWasmToolingSetup")
                assertTasksExecuted(":kotlinWasmNpmInstall")
            }
        }
    }
}

private val json = Json {
    prettyPrint = true
    prettyPrintIndent = "  "
}

/**
 * Verify the lockfiles exist, and they have not been modified during `npm/yarn install`
 * (i.e. they are up-to-date and in sync with the `package.json`, so they will work in offline mode).
 */
private fun checkLockFiles(
    isYarn: Boolean,
    toolingCustomDir: Path,
) {
    val lockfile = toolingCustomDir.resolve(if (isYarn) "yarn.lock" else "package-lock.json")
    assertFileExists(lockfile)

    assertFileNotExists(
        toolingCustomDir.resolve(if (!isYarn) "yarn.lock" else "package-lock.json")
    )

    // Must make sure KGP's store lockfiles are not outdated.
    // Package managers might silently update them after an installation,
    // which is a problem for offline installs.
    if (isYarn) {
        val expectedLockfile = kgpYarnLockFileContent
        val actualLockfile = lockfile.readText()
        assertEquals(expectedLockfile, actualLockfile)

    } else {
        // Only check the `packages` key.
        // The root package, with key `""`, is for the current project and can be ignored.
        fun extractPackages(packageLockJson: String) =
            json.decodeFromString<JsonObject>(packageLockJson)
                .getValue("packages")
                .jsonObject
                .filterKeys { it != "" }
                .let { json.encodeToString(it) }

        val expectedLockfilePackages =
            extractPackages(kgpPackageLockJsonFileContent)

        val actualLockfilePackages =
            extractPackages(lockfile.readText())

        assertEquals(
            expectedLockfilePackages,
            actualLockfilePackages,
        )
    }
}
