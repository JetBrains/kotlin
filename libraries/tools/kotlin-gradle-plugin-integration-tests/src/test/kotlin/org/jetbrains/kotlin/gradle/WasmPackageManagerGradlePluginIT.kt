/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.process.ExecOperations
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.targets.js.NpmVersions
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.LockCopyTask.Companion.KOTLIN_JS_STORE
import org.jetbrains.kotlin.gradle.targets.js.npm.LockCopyTask.Companion.PACKAGE_LOCK
import org.jetbrains.kotlin.gradle.targets.js.npm.LockCopyTask.Companion.YARN_LOCK
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNpmTooling
import org.jetbrains.kotlin.gradle.targets.wasm.npm.WasmNpmExtension
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnRootEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnRootExtension
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.replaceText
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.reflect.KClass
import kotlin.test.assertEquals

class WasmNpmGradlePluginIT : WasmPackageManagerGradlePluginIT() {
    override val yarn: Boolean = false

    override val lockFileName: String = PACKAGE_LOCK

    override val toolingCustomDir: String
        get() = "npm"
}

class WasmYarnGradlePluginIT : WasmPackageManagerGradlePluginIT() {
    override val yarn: Boolean = true

    override val lockFileName: String = YARN_LOCK

    override val toolingCustomDir: String
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

    abstract val toolingCustomDir: String

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

            // extracted as val to be serializable
            val isYarn = yarn
            val toolingCustomDir = toolingCustomDir
            val kgpNpmToolingPackageJson = kgpNpmToolingPackageJson

            buildScriptInjection {
                project.rootProject.extensions.getByType(WasmNpmTooling::class.java).apply {
                    this.installationDir.fileValue(project.projectDir.resolve(toolingCustomDir))
                }

                val nodeJsEnvSpec = project.extensions.getByType(WasmNodeJsEnvSpec::class.java)

                val nodejsExecutable = nodeJsEnvSpec.executable

                val toolExecutable = if (isYarn) {
                    nodejsExecutable
                } else {
                    project.provider {
                        project.rootProject.extensions.getByType(WasmNpmExtension::class.java).requireConfigured().executable
                    }
                }

                val installArgs = if (isYarn) {
                    project.rootProject.extensions.getByType(WasmYarnRootEnvSpec::class.java).executable.map { listOf(it) }
                } else {
                    project.provider {
                        listOf("install", "--ignore-scripts")
                    }
                }

                val toolingCustomDir = project.projectDir.resolve(toolingCustomDir)
                toolingCustomDir.mkdirs()

                // A `package.json` file is required for `npm install` and `yarn install` commands to work.
                toolingCustomDir.resolve("package.json").writeText(kgpNpmToolingPackageJson)

                if (isYarn) {
                    toolingCustomDir.resolve("yarn.lock").writeText(kgpYarnLockFileContent)
                } else {
                    toolingCustomDir.resolve("package-lock.json").writeText(kgpPackageLockJsonFileContent)
                }

                project.tasks.register("toolingInstall").configure { task ->

                    with(nodeJsEnvSpec) {
                        task.dependsOn(project.nodeJsSetupTaskProvider)
                    }
                    if (isYarn) {
                        task.dependsOn(WasmYarnRootExtension[project.rootProject].yarnSetupTaskProvider)
                    }

                    val exec = project.serviceOf<ExecOperations>()

                    task.doLast { _ ->
                        val execOutput = ByteArrayOutputStream()
                        val result = exec.exec { exec ->
                            exec.executable(toolExecutable.get())
                            exec.args(installArgs.get())
                            exec.workingDir = toolingCustomDir
                            exec.standardOutput = execOutput
                            exec.errorOutput = execOutput
                            exec.isIgnoreExitValue = true

                            if (!isYarn) {
                                val nodePath = File(nodejsExecutable.get()).parent
                                exec.environment["PATH"] =
                                    "$nodePath${File.pathSeparator}${System.getenv("PATH")}"
                            }
                        }
                        require(result.exitValue == 0) {
                            buildString {
                                appendLine("${task.path}} failed")
                                appendLine(execOutput)
                            }
                        }
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

    companion object {
        private val kgpPackageLockJsonFileContent: String by lazy {
            NodeJsPlugin::class.loadResource("/org/jetbrains/kotlin/gradle/targets/js/npm/package-lock.json")
        }

        private val kgpYarnLockFileContent: String by lazy {
            NodeJsPlugin::class.loadResource("/org/jetbrains/kotlin/gradle/targets/js/yarn/yarn.lock")
        }

        private fun KClass<*>.loadResource(path: String): String {
            java.getResourceAsStream(path).use { source ->
                requireNotNull(source) { "Resource not found: $path" }
                return source.bufferedReader().readText()
            }
        }

        /**
         * Create a `package.json` file containing KGP's tooling dependencies.
         *
         * @see [org.jetbrains.kotlin.gradle.targets.js.NpmVersions]
         */
        private val kgpNpmToolingPackageJson: String by lazy {
            val json = Json {
                prettyPrint = true
            }

            val packageJson: JsonObject =
                buildJsonObject {
                    put("name", "kotlin-tooling-dependencies")
                    put("version", "1.0.0")
                    put("private", true)
                    put("dependencies", buildJsonObject {
                        NpmVersions().allDependencies.forEach { (name, version) ->
                            put(name, version)
                        }
                    })
                }

            json.encodeToString(JsonObject.serializer(), packageJson)
        }
    }
}
