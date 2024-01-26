/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.targets.js.npm.LockCopyTask
import org.jetbrains.kotlin.gradle.targets.js.npm.LockCopyTask.Companion.PACKAGE_LOCK_MISMATCH_MESSAGE
import org.jetbrains.kotlin.gradle.targets.js.npm.LockCopyTask.Companion.RESTORE_PACKAGE_LOCK_NAME
import org.jetbrains.kotlin.gradle.targets.js.npm.LockCopyTask.Companion.STORE_PACKAGE_LOCK_NAME
import org.jetbrains.kotlin.gradle.targets.js.npm.LockCopyTask.Companion.UPGRADE_PACKAGE_LOCK
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.testbase.TestVersions.Gradle.G_7_6
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.deleteRecursively
import kotlin.io.path.notExists
import kotlin.io.path.readText

class NpmGradlePluginIT : PackageManagerGradlePluginIT() {
    override val yarn: Boolean = false

    override val plugin = "nodejs.NodeJsRootPlugin"

    override val extension = "npm.NpmExtension"

    override val upgradeTaskName: String = UPGRADE_PACKAGE_LOCK

    override val storeTaskName = STORE_PACKAGE_LOCK_NAME

    override val restoreTaskName = RESTORE_PACKAGE_LOCK_NAME

    override val reportNewLockFile = "reportNewPackageLock"

    override val lockMismatchReport = "packageLockMismatchReport"

    override val lockFileAutoReplace = "packageLockAutoReplace"

    override val mismatchReportType = "npm.LockFileMismatchReport"

    override val lockFileName: String = LockCopyTask.PACKAGE_LOCK

    override val setProperty: (String) -> String = { ".set($it)" }
}

class YarnGradlePluginIT : PackageManagerGradlePluginIT() {
    override val yarn: Boolean = true

    override val plugin: String = "yarn.YarnPlugin"

    override val extension: String = "yarn.YarnRootExtension"

    override val upgradeTaskName: String = YarnPlugin.UPGRADE_YARN_LOCK

    override val storeTaskName = YarnPlugin.STORE_YARN_LOCK_NAME

    override val restoreTaskName: String = YarnPlugin.RESTORE_YARN_LOCK_NAME

    override val reportNewLockFile: String = "reportNewYarnLock"

    override val lockMismatchReport: String = "yarnLockMismatchReport"

    override val lockFileAutoReplace: String = "yarnLockAutoReplace"

    override val mismatchReportType: String = "yarn.YarnLockMismatchReport"

    override val lockFileName: String = LockCopyTask.YARN_LOCK

    override val setProperty: (String) -> String = { " = $it" }
}

@JsGradlePluginTests
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

    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.copy(
            jsOptions = super.defaultBuildOptions.jsOptions?.copy(
                yarn = yarn
            )
        )

    @DisplayName("js composite build works with lock file persistence")
    @GradleTest
    @GradleTestVersions(minVersion = G_7_6)
    fun testJsCompositeBuildWithUpgradeLockFile(gradleVersion: GradleVersion) {
        project("js-composite-build", gradleVersion) {
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
        buildGradleKts.modify(::transformBuildScriptWithPluginsDsl)

        subProject("lib").apply {
            buildGradleKts.modify(::transformBuildScriptWithPluginsDsl)
        }

        subProject("base").apply {
            buildGradleKts.modify(::transformBuildScriptWithPluginsDsl)
        }

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
    fun testFailingWithLockFileUpdate(gradleVersion: GradleVersion) {
        project("kotlin-js-package-lock-project", gradleVersion) {
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

        projectPath.resolve(LockCopyTask.KOTLIN_JS_STORE).deleteRecursively()

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

            assertOutputContains(PACKAGE_LOCK_MISMATCH_MESSAGE)
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

            assertOutputDoesNotContain(PACKAGE_LOCK_MISMATCH_MESSAGE)
        }

        build(upgradeTaskName) {
            assertTasksExecuted(":$restoreTaskName")
            assertTasksExecuted(":$upgradeTaskName")
        }

        buildGradleKts.modify {
            it.replace("implementation(npm(\"decamelize\", \"6.0.0\"))", "")
        }

        projectPath.resolve(LockCopyTask.KOTLIN_JS_STORE).deleteRecursively()

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

            assertOutputDoesNotContain(PACKAGE_LOCK_MISMATCH_MESSAGE)
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
            assertTasksExecuted(":clean")
        }

        build("clean") {
            assertTasksUpToDate(":clean")
        }

        buildAndFail(storeTaskName) {
            assertTasksExecuted(":$restoreTaskName")
            assertTasksFailed(":$storeTaskName")
        }

        projectPath.resolve(LockCopyTask.KOTLIN_JS_STORE).deleteRecursively()

        //check if independent tasks can be executed
        build("help") {
            assertTasksExecuted(":help")
        }
    }

    @DisplayName("Lock file persistence")
    @GradleTest
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
            assertFileExists(projectPath.resolve(LockCopyTask.KOTLIN_JS_STORE).resolve(lockFile))
            assert(
                projectPath
                    .resolve(LockCopyTask.KOTLIN_JS_STORE)
                    .resolve(lockFile)
                    .readText() == projectPath.resolve("build/js/${lockFile}").readText()
            )
        }
    }

    @DisplayName("Package manager ignore scripts")
    @GradleTest
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
}
