/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.targets.js.nodejs.JsPlatformDisambiguator
import org.jetbrains.kotlin.gradle.targets.js.npm.LockCopyTask
import org.jetbrains.kotlin.gradle.targets.js.npm.LockCopyTask.Companion.KOTLIN_JS_STORE
import org.jetbrains.kotlin.gradle.targets.js.npm.LockCopyTask.Companion.PACKAGE_LOCK
import org.jetbrains.kotlin.gradle.targets.js.npm.LockCopyTask.Companion.RESTORE_PACKAGE_LOCK_BASE_NAME
import org.jetbrains.kotlin.gradle.targets.js.npm.LockCopyTask.Companion.STORE_PACKAGE_LOCK_BASE_NAME
import org.jetbrains.kotlin.gradle.targets.js.npm.LockCopyTask.Companion.UPGRADE_PACKAGE_LOCK_BASE_NAME
import org.jetbrains.kotlin.gradle.targets.js.npm.fromSrcPackageJson
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import kotlin.io.path.deleteRecursively
import kotlin.io.path.notExists
import kotlin.io.path.readText
import kotlin.test.assertEquals

class NpmGradlePluginIT : PackageManagerGradlePluginIT() {
    override val yarn: Boolean = false

    override val plugin = "nodejs.NodeJsRootPlugin"

    override val extension = "npm.NpmExtension"

    override val upgradeTaskName: String = JsPlatformDisambiguator.extensionName(UPGRADE_PACKAGE_LOCK_BASE_NAME)

    override val storeTaskName = JsPlatformDisambiguator.extensionName(STORE_PACKAGE_LOCK_BASE_NAME)

    override val restoreTaskName = JsPlatformDisambiguator.extensionName(RESTORE_PACKAGE_LOCK_BASE_NAME)

    override val reportNewLockFile = "reportNewPackageLock"

    override val lockMismatchReport = "packageLockMismatchReport"

    override val lockFileAutoReplace = "packageLockAutoReplace"

    override val mismatchReportType = "npm.LockFileMismatchReport"

    override val lockFileName: String = PACKAGE_LOCK

    override val setProperty: (String) -> String = { ".set($it)" }

    override val mismatchReportMessage: String = LockCopyTask.packageLockMismatchMessage(upgradeTaskName)

    @DisplayName("package-lock is OS independent")
    @GradleTest
    @JsGradlePluginTests
    @OsCondition(enabledOnCI = [OS.WINDOWS])
    fun testPackageLockOsIndependent(gradleVersion: GradleVersion) {
        project("kotlin-js-package-lock-project", gradleVersion) {

            build(":kotlinStorePackageLock") {
                val packageLock = projectPath.resolve(KOTLIN_JS_STORE).resolve(PACKAGE_LOCK)
                assertFileExists(packageLock)
                assertFileDoesNotContain(packageLock, "\\")
            }
        }
    }

    @GradleTest
    @JsGradlePluginTests
    fun `when transitive npm dependency version changes - expect package json is rebuilt`(
        gradleVersion: GradleVersion,
    ) {
        project("js-composite-build", gradleVersion) {

            /**
             * Verify the `package.json` task is initially executed, and afterwards is up-to-date.
             * And assert that compilation tasks don't run.
             */
            fun validateTasks() {

                fun BuildResult.assertCompileTasksNotRun() {
                    assertTasksAreNotInTaskGraph(
                        buildList {
                            add(":compileKotlinJs")
                            add(":lib:lib-2:compileKotlinJs")
                            add(":base:compileKotlinJs")
                        }
                    )
                }

                build(":rootPackageJson") {
                    assertTasksExecuted(":rootPackageJson")
                    assertCompileTasksNotRun()
                }
                build(":rootPackageJson") {
                    assertTasksUpToDate(":rootPackageJson")
                    assertCompileTasksNotRun()
                }
            }

            fun assertDependencyUpdateReRunsRootPackageJsonTask(
                old: String,
                new: String,
            ) {
                // modify a dependency in the composite 'base' build
                projectPath.resolve("base/build.gradle.kts").modify { content ->
                    require(old in content) { "dependency $old not defined in buildscript" }
                    content.replace(old, new)
                }

                // Because a npm dependency in a dependency changed,
                // the root project package.json task should be re-run.
                validateTasks()
            }

            assertDependencyUpdateReRunsRootPackageJsonTask(
                """implementation(npm("decamelize", "1.1.1"))""",
                """implementation(npm("decamelize", "1.1.2"))""",
            )

            assertDependencyUpdateReRunsRootPackageJsonTask(
                """api(npm("cowsay", "1.6.0"))""",
                """api(npm("cowsay", "1.5.0"))""",
            )

            assertDependencyUpdateReRunsRootPackageJsonTask(
                """runtimeOnly(npm("uuid", "11.1.0"))""",
                """runtimeOnly(npm("uuid", "10.0.0"))""",
            )
        }
    }
}

class YarnGradlePluginIT : PackageManagerGradlePluginIT() {
    override val yarn: Boolean = true

    override val plugin: String = "yarn.YarnPlugin"

    override val extension: String = "yarn.YarnRootExtension"

    override val upgradeTaskName: String = JsPlatformDisambiguator.extensionName(YarnPlugin.UPGRADE_YARN_LOCK_BASE_NAME)

    override val storeTaskName = JsPlatformDisambiguator.extensionName(YarnPlugin.STORE_YARN_LOCK_BASE_NAME)

    override val restoreTaskName: String = JsPlatformDisambiguator.extensionName(YarnPlugin.RESTORE_YARN_LOCK_BASE_NAME)

    override val reportNewLockFile: String = "reportNewYarnLock"

    override val lockMismatchReport: String = "yarnLockMismatchReport"

    override val lockFileAutoReplace: String = "yarnLockAutoReplace"

    override val mismatchReportType: String = "yarn.YarnLockMismatchReport"

    override val lockFileName: String = LockCopyTask.YARN_LOCK

    override val setProperty: (String) -> String = { " = $it" }

    override val mismatchReportMessage: String = YarnPlugin.yarnLockMismatchMessage(upgradeTaskName)
}

abstract class PackageManagerGradlePluginIT : KGPBaseTest() {

    abstract val yarn: Boolean

    abstract val plugin: String

    abstract val extension: String

    abstract val upgradeTaskName: String

    abstract val storeTaskName: String

    abstract val restoreTaskName: String

    abstract val reportNewLockFile: String

    abstract val lockMismatchReport: String

    abstract val lockFileAutoReplace: String

    abstract val mismatchReportType: String

    abstract val lockFileName: String

    abstract val setProperty: (String) -> String

    abstract val mismatchReportMessage: String

    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.copy(
            jsOptions = super.defaultBuildOptions.jsOptions?.copy(
                yarn = yarn
            ),
            // KT-75899 Support Gradle Project Isolation in KGP JS & Wasm
            isolatedProjects = BuildOptions.IsolatedProjectsMode.DISABLED,
        )

    @DisplayName("js composite build works with lock file persistence")
    @GradleTest
    @JsGradlePluginTests
    fun testJsCompositeBuildWithUpgradeLockFile(gradleVersion: GradleVersion) {
        project(
            "js-composite-build",
            gradleVersion,
            // `:compileKotlinJs` task is not compatible with CC on Gradle 7
            buildOptions = defaultBuildOptions.disableConfigurationCacheForGradle7(gradleVersion),
        ) {
            testJsCompositeBuildWithUpgradeLockFile(
                upgradeTaskName,
                storeTaskName
            )
        }
    }

    private fun TestProject.testJsCompositeBuildWithUpgradeLockFile(
        upgradeTask: String,
        storeTask: String,
    ) {
        build(upgradeTask) {
            assertTasksExecuted(":base:publicPackageJson")
            assertTasksExecuted(":lib:lib-2:publicPackageJson")
            assertTasksExecuted(":kotlinNpmInstall")
            assertTasksExecuted(":$upgradeTask")
        }

        build(":nodeTest") {
            assertTasksUpToDate(":base:publicPackageJson")
            assertTasksUpToDate(":lib:lib-2:publicPackageJson")
            assertTasksUpToDate(":kotlinNpmInstall")
            assertTasksExecuted(":$storeTask")
        }
    }

    @DisplayName("Failing with lock file update")
    @GradleTest
    @JsGradlePluginTests
    fun testFailingWithLockFileUpdate(gradleVersion: GradleVersion) {
        project(
            "kotlin-js-package-lock-project",
            gradleVersion,
        ) {
            testFailingWithLockFileUpdate(
                storeTaskName = storeTaskName,
                restoreTaskName = restoreTaskName,
                upgradeTaskName = upgradeTaskName,
                extension = extension,
                reportNewLockFile = reportNewLockFile,
                lockMismatchReport = lockMismatchReport,
                lockFileAutoReplace = lockFileAutoReplace,
                mismatchReport = mismatchReportType,
                set = setProperty
            )
        }
    }

    private fun TestProject.testFailingWithLockFileUpdate(
        storeTaskName: String,
        restoreTaskName: String,
        upgradeTaskName: String,
        extension: String,
        reportNewLockFile: String,
        lockMismatchReport: String,
        lockFileAutoReplace: String,
        mismatchReport: String,
        set: (String) -> String,
    ) {
        build(storeTaskName) {
            assertTasksSkipped(":$restoreTaskName")
            assertTasksExecuted(":$storeTaskName")
        }

        projectPath.resolve(KOTLIN_JS_STORE).deleteRecursively()

        buildGradleKts.modify {
            it + "\n" +
                    """
                        rootProject.plugins.withType(org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin::class.java) {
                            rootProject.the<org.jetbrains.kotlin.gradle.targets.js.$extension>().$reportNewLockFile${set("true")}
                        }
                        """.trimIndent()
        }

        buildAndFail(storeTaskName) {
            assertTasksSkipped(":$restoreTaskName")
            assertTasksFailed(":$storeTaskName")
        }

        buildGradleKts.modify {
            it + "\n" +
                    """
                        dependencies {
                            implementation(npm("decamelize", "6.0.0"))
                        }
                            
                        rootProject.plugins.withType(org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin::class.java) {
                            rootProject.the<org.jetbrains.kotlin.gradle.targets.js.$extension>().$lockMismatchReport${
                        set(
                            "org.jetbrains.kotlin.gradle.targets.js.$mismatchReport.FAIL"
                        )
                    }
                        }
                        """.trimIndent()
        }

        buildAndFail(storeTaskName) {
            assertTasksExecuted(":$restoreTaskName")
            assertTasksFailed(":$storeTaskName")
        }

        // yarn.lock was not updated
        buildAndFail(storeTaskName) {
            assertTasksExecuted(":$restoreTaskName")
            assertTasksFailed(":$storeTaskName")
        }

        build(upgradeTaskName) {
            assertTasksExecuted(":$restoreTaskName")
            assertTasksExecuted(":$upgradeTaskName")
        }

        buildGradleKts.modify {
            val replaced = it.replace("implementation(npm(\"decamelize\", \"6.0.0\"))", "")
            replaced + "\n" +
                    """
                        rootProject.plugins.withType(org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin::class.java) {
                            rootProject.the<org.jetbrains.kotlin.gradle.targets.js.$extension>().$lockMismatchReport${
                        set(
                            "org.jetbrains.kotlin.gradle.targets.js.$mismatchReport.WARNING"
                        )
                    }
                        }
                        """.trimIndent()
        }

        build(storeTaskName) {
            assertTasksExecuted(":$restoreTaskName")
            assertTasksExecuted(":$storeTaskName")

            assertOutputContains(mismatchReportMessage)
        }

        build(upgradeTaskName) {
            assertTasksExecuted(":$restoreTaskName")
            assertTasksExecuted(":$upgradeTaskName")
        }

        buildGradleKts.modify {
            it + "\n" +
                    """
                        dependencies {
                            implementation(npm("decamelize", "6.0.0"))
                        }
                            
                        rootProject.plugins.withType(org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin::class.java) {
                            rootProject.the<org.jetbrains.kotlin.gradle.targets.js.$extension>().$lockMismatchReport${
                        set(
                            "org.jetbrains.kotlin.gradle.targets.js.$mismatchReport.NONE"
                        )
                    }
                        }
                        """.trimIndent()
        }

        build(storeTaskName) {
            assertTasksExecuted(":$restoreTaskName")
            assertTasksExecuted(":$storeTaskName")

            assertOutputDoesNotContain(mismatchReportMessage)
        }

        build(upgradeTaskName) {
            assertTasksExecuted(":$restoreTaskName")
            assertTasksExecuted(":$upgradeTaskName")
        }

        buildGradleKts.modify {
            it.replace("implementation(npm(\"decamelize\", \"6.0.0\"))", "")
        }

        projectPath.resolve(KOTLIN_JS_STORE).deleteRecursively()

        buildGradleKts.modify {
            it + "\n" +
                    """
                        rootProject.plugins.withType(org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin::class.java) {
                            rootProject.the<org.jetbrains.kotlin.gradle.targets.js.$extension>().$lockMismatchReport${
                        set(
                            "org.jetbrains.kotlin.gradle.targets.js.$mismatchReport.NONE"
                        )
                    }
                            rootProject.the<org.jetbrains.kotlin.gradle.targets.js.$extension>().$reportNewLockFile${
                        set(
                            "true"
                        )
                    }
                        }
                        """.trimIndent()
        }

        build(storeTaskName) {
            assertTasksSkipped(":$restoreTaskName")
            assertTasksExecuted(":$storeTaskName")

            assertOutputDoesNotContain(mismatchReportMessage)
        }

        buildGradleKts.modify {
            it + "\n" +
                    """
                        dependencies {
                            implementation(npm("decamelize", "6.0.0"))
                        }
                            
                        rootProject.plugins.withType(org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin::class.java) {
                            rootProject.the<org.jetbrains.kotlin.gradle.targets.js.$extension>().$lockMismatchReport${
                        set(
                            "org.jetbrains.kotlin.gradle.targets.js.$mismatchReport.FAIL"
                        )
                    }
                            rootProject.the<org.jetbrains.kotlin.gradle.targets.js.$extension>().$lockFileAutoReplace${
                        set(
                            "true"
                        )
                    }
                        }
                        """.trimIndent()
        }

        buildAndFail(storeTaskName) {
            assertTasksUpToDate(":$restoreTaskName")
            assertTasksFailed(":$storeTaskName")
        }

        //yarn.lock was updated
        build(storeTaskName) {
            assertTasksExecuted(":$restoreTaskName")
            assertTasksExecuted(":$storeTaskName")
        }

        buildGradleKts.modify {
            it.replace("implementation(npm(\"decamelize\", \"6.0.0\"))", "")
        }

        // check if everything ok without build/js/yarn.lock
        build("clean") {
            assertDirectoryInProjectExists(KOTLIN_JS_STORE)
            assertFileInProjectExists("$KOTLIN_JS_STORE/$lockFileName")
            assertFileInProjectNotExists("build/js/${lockFileName}")
        }

        build("clean") {
            assertDirectoryInProjectExists(KOTLIN_JS_STORE)
            assertFileInProjectExists("$KOTLIN_JS_STORE/$lockFileName")
            assertFileInProjectNotExists("build/js/${lockFileName}")
        }

        buildAndFail(storeTaskName) {
            assertTasksExecuted(":$restoreTaskName")
            assertTasksFailed(":$storeTaskName")
        }

        projectPath.resolve(KOTLIN_JS_STORE).deleteRecursively()

        //check if independent tasks can be executed
        build("help") {
            assertTasksExecuted(":help")
        }
    }

    @DisplayName("Lock file persistence")
    @GradleTest
    @JsGradlePluginTests
    fun testLockStore(gradleVersion: GradleVersion) {
        project("nodeJsDownload", gradleVersion) {
            testLockStore(
                storeTaskName,
                lockFileName
            )
        }
    }

    private fun TestProject.testLockStore(
        taskName: String,
        lockFile: String,
    ) {
        build("assemble", taskName) {
            assertFileExists(projectPath.resolve(KOTLIN_JS_STORE).resolve(lockFile))
            assert(
                projectPath
                    .resolve(KOTLIN_JS_STORE)
                    .resolve(lockFile)
                    .readText() == projectPath.resolve("build/js/${lockFile}").readText()
            )
        }
    }

    @DisplayName("Package manager ignore scripts")
    @GradleTest
    @JsGradlePluginTests
    fun testIgnoreScripts(gradleVersion: GradleVersion) {
        project("nodeJsDownload", gradleVersion) {
            testIgnoreScripts(
                plugin,
                extension,
                setProperty
            )
        }
    }

    private fun TestProject.testIgnoreScripts(
        plugin: String,
        extension: String,
        set: (String) -> String,
    ) {
        buildGradleKts.modify {
            it + "\n" +
                    """
                        dependencies {
                            implementation(npm("puppeteer", "11.0.0"))
                        }
                        """.trimIndent()
        }
        build("assemble", "kotlinNpmInstall") {
            assert(
                projectPath
                    .resolve("build")
                    .resolve("js")
                    .resolve("node_modules")
                    .resolve("puppeteer")
                    .resolve(".local-chromium")
                    .notExists()
            ) {
                "Chromium should not be installed with --ignore-scripts"
            }
        }

        buildGradleKts.modify {
            it + "\n" +
                    """
                        rootProject.plugins.withType<org.jetbrains.kotlin.gradle.targets.js.$plugin> {
                            rootProject.the<org.jetbrains.kotlin.gradle.targets.js.$extension>().ignoreScripts${set("false")}
                        }
                        """.trimIndent()
        }

        build("clean")

        build("assemble", "kotlinNpmInstall") {
            assertDirectoryExists(
                projectPath
                    .resolve("build")
                    .resolve("js")
                    .resolve("node_modules")
                    .resolve("puppeteer")
                    .resolve(".local-chromium")
            )
        }
    }

    @DisplayName("Change rootPackageJson after its generation")
    @GradleTest
    @JsGradlePluginTests
    fun testChangeRootPackageJsonAfterGeneration(gradleVersion: GradleVersion) {
        project("kotlin-js-package-lock-project", gradleVersion) {
            buildGradleKts.modify {
                //language=kotlin
                """
                |$it
                |
                |plugins.withType(org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin::class.java) {
                |    the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension>().apply {
                |        rootPackageJsonTaskProvider.configure {
                |            doLast {
                |                val file = rootPackageJsonFile.get().asFile
                |                val rootPackageJson = org.jetbrains.kotlin.gradle.targets.js.npm.fromSrcPackageJson(file) 
                |                    ?: error("Null PackageJson from ${'$'}file")
                |                rootPackageJson.version = "foo"
                |                rootPackageJson.saveTo(file)
                |            }
                |        }
                |    }
                |}
                """.trimMargin()
            }

            fun assertRootPackageJsonVersion(expectedVersion: String) {
                val packageJsonFile = projectPath
                    .resolve("build/js/package.json")
                    .toFile()

                val packageJson = fromSrcPackageJson(packageJsonFile)

                assertEquals(expectedVersion, packageJson?.version)
            }

            build("kotlinNpmInstall", storeTaskName) {
                assertTasksExecuted(":rootPackageJson", ":kotlinNpmInstall")
                assertRootPackageJsonVersion("foo")
            }

            projectPath.resolve("build/js/$lockFileName").deleteRecursively()
            projectPath.resolve(KOTLIN_JS_STORE).deleteRecursively()

            build("kotlinNpmInstall", storeTaskName) {
                assertTasksUpToDate(":rootPackageJson")
                assertTasksExecuted(":kotlinNpmInstall")
                assertRootPackageJsonVersion("foo")
            }
        }
    }

    @DisplayName("Ensure that gradle offline mode passes --offline argument to npm executable")
    @GradleTest
    @JsGradlePluginTests
    fun testOfflineFlag(gradleVersion: GradleVersion) {
        project("js-only-npm", gradleVersion, enableOfflineMode = true) {
            buildScriptInjection {
                this.project.tasks.named("kotlinNpmInstall") {
                    val buildDir = this.project.layout.buildDirectory

                    it.doFirst {
                        val npmCache = buildDir.dir("npm-cache").get().asFile.also {
                            it.mkdirs()
                        }
                        buildDir.file("js/.npmrc").get().asFile.writeText(
                            """
                                cache=${npmCache.invariantSeparatorsPath}
                            """.trimIndent()
                        )
                        buildDir.file("js/.yarnrc").get().asFile.writeText(
                            """
                                cache-folder "${npmCache.invariantSeparatorsPath}"
                            """.trimIndent()
                        )
                    }
                }
            }

            // populate Gradle cache
            build("kotlinNpmInstall", enableOfflineMode = false) {
                assertTasksExecuted(":kotlinNpmInstall")
            }

            // clean everything including NPM cache
            build("clean")

            buildAndFail("kotlinNpmInstall") {
                assertTasksFailed(":kotlinNpmInstall")
            }

            build("kotlinNpmInstall", "--rerun", enableOfflineMode = false) {
                assertTasksExecuted(":kotlinNpmInstall")
            }

            build("kotlinNpmInstall", "--rerun") {
                assertTasksExecuted(":kotlinNpmInstall")
            }
        }
    }
}
