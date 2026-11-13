/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.Project
import org.gradle.api.tasks.StopExecutionException
import org.gradle.kotlin.dsl.kotlin
import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.targets.js.nodejs.JsPlatformDisambiguator
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.*
import org.jetbrains.kotlin.gradle.targets.js.npm.LockCopyTask.Companion.KOTLIN_JS_STORE
import org.jetbrains.kotlin.gradle.targets.js.npm.LockCopyTask.Companion.PACKAGE_LOCK
import org.jetbrains.kotlin.gradle.targets.js.npm.LockCopyTask.Companion.RESTORE_PACKAGE_LOCK_BASE_NAME
import org.jetbrains.kotlin.gradle.targets.js.npm.LockCopyTask.Companion.STORE_PACKAGE_LOCK_BASE_NAME
import org.jetbrains.kotlin.gradle.targets.js.npm.LockCopyTask.Companion.UPGRADE_PACKAGE_LOCK_BASE_NAME
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockMismatchReport
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.npm.WasmNpmExtension
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnRootExtension
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.kotlin.testFederation.MustRunOnChangesInGradle
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import kotlin.io.path.*
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

    @TestMetadata("kotlin-js-package-lock-project")
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

    @TestMetadata("js-composite-build")
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

    @TestMetadata("js-composite-build")
    @DisplayName("js composite build works with lock file persistence")
    @GradleTest
    @JsGradlePluginTests
    fun yarnStoreLockAlwaysTriggersNpmInstall(gradleVersion: GradleVersion) {
        project(
            "js-composite-build",
            gradleVersion,
        ) {
            // 1st run
            build(upgradeTaskName) {
                assertTasksExecuted(":kotlinNpmInstall")
            }

            // 1st run, npmInstall should be up to date
            // --force flag is not set because upgradeTaskName is not requested
            // all input/outputs didn't change since last run
            build(storeTaskName) {
                assertTasksUpToDate(":kotlinNpmInstall")
            }

            // 3rd run, npmInstall must re-run
            // upgradeTaskName is requested, so --force flag is set
            build(upgradeTaskName) {
                assertTasksExecuted(":kotlinNpmInstall")
            }
        }
    }
}

@MustRunOnChangesInGradle
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
        ).disableIsolatedProjectsBecauseOfJsAndWasmKT75899()

    @TestMetadata("js-composite-build")
    @DisplayName("js composite build works with lock file persistence")
    @GradleTest
    @JsGradlePluginTests
    fun testJsCompositeBuildWithUpgradeLockFile(gradleVersion: GradleVersion) {
        project(
            "js-composite-build",
            gradleVersion,
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
            assertTasksExecuted(":base:jsPublicPackageJson")
            assertTasksExecuted(":lib:lib-2:jsPublicPackageJson")
            assertTasksExecuted(":kotlinNpmInstall")
            assertTasksExecuted(":$upgradeTask")
        }

        build(":jsNodeTest") {
            assertTasksUpToDate(":base:jsPublicPackageJson")
            assertTasksUpToDate(":lib:lib-2:jsPublicPackageJson")
            assertTasksUpToDate(":kotlinNpmInstall")
            assertTasksExecuted(":$storeTask")
        }
    }

    @TestMetadata("kotlin-js-package-lock-project")
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
                        kotlin {
                            sourceSets {
                                jsMain {
                                    dependencies {
                                        implementation(npm("decamelize", "6.0.0"))
                                    }
                                }
                            }
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
                        kotlin {
                            sourceSets {
                                jsMain {
                                    dependencies {
                                        implementation(npm("decamelize", "6.0.0"))
                                    }
                                }
                            }
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
                        kotlin {
                            sourceSets {
                                jsMain {
                                    dependencies {
                                        implementation(npm("decamelize", "6.0.0"))
                                    }
                                }
                            }
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

    @TestMetadata("nodeJsDownload")
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

    @TestMetadata("nodeJsDownload")
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
                        kotlin {
                            sourceSets {
                                jsMain {
                                    dependencies {
                                        implementation(npm("puppeteer", "11.0.0"))
                                    }
                                }
                            }
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

    @TestMetadata("kotlin-js-package-lock-project")
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

    @TestMetadata("js-only-npm")
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

    @DisplayName("tasks that require npm dependencies must re-run and not be up-to-date after npm dependencies change")
    @GradleTest
    @JsGradlePluginTests
    @OptIn(ExperimentalJsTestDsl::class)
    fun testRequiresNpmDependenciesTasksRerunWhenNpmDependenciesChange(gradleVersion: GradleVersion) {
        project("empty", gradleVersion) {
            plugins {
                kotlin("multiplatform")
            }

            val expectedNpmTasks = trackedNpmTasks + untrackedNpmTasks
            buildScriptInjection {
                project.applyMultiplatform {
                    js {
                        browser {
                            test.chromium()
                        }
                        nodejs()
                        binaries.executable()
                        generateTypeScriptDefinitions()
                    }
                    wasmJs {
                        browser {
                            test.chromium()
                        }
                        nodejs()
                        binaries.executable()
                    }

                    sourceSets.commonMain.dependencies {
                        // Both npm DSLs are covered, so the test keeps working when the deprecated one is removed.
                        @Suppress("DEPRECATION")
                        implementation(npm("decamelize", project.providers.gradleProperty("decamelizeVersion").get()))
                        npm("left-pad", project.providers.gradleProperty("leftPadVersion"))
                    }
                    sourceSets.commonTest.dependencies {
                        implementation(kotlin("test"))
                    }
                }

                // Lock file validation aren't relevant to this test, so just disable them to avoid unrelated failures
                project.disableNpmLockFileValidations()

                val npmTasks = project.tasks.withType(RequiresNpmDependenciesTask::class.java)

                npmTasks.configureEach { task ->
                    task.doFirst("Skip real task actions") { _ ->
                        val lockFiles = task.npmDependenciesLockFiles.files
                        check(lockFiles.isNotEmpty()) { "${task.path} has no npm dependencies lock files" }
                        throw StopExecutionException(
                            "Skipping ${task.path}. Running task actions for this test is slow and unnecessary. The test only needs to check whether tasks are up-to-date"
                        )
                    }
                }

                project.tasks.register("checkNpmTaskList") { task ->
                    task.description = "Fail when a task is added or removed, so that the lists in the test are updated."
                    val actualNpmTaskPaths = npmTasks.names.map { ":$it" }.sorted()
                    task.doLast { _ ->
                        check(actualNpmTaskPaths == expectedNpmTasks.sorted()) {
                            "Unexpected npm tasks. Expected: ${expectedNpmTasks.sorted()}, actual: $actualNpmTaskPaths"
                        }
                    }
                }
            }

            kotlinSourcesDir("commonMain").resolve("Main.kt").createParentDirectories().writeText(
                """
                |fun main() {
                |    println("main")
                |}
                |""".trimMargin()
            )
            kotlinSourcesDir("commonTest").resolve("MainTest.kt").createParentDirectories().writeText(
                """
                |import kotlin.test.*
                |class MainTest {
                |    @Test
                |    fun test() {}
                |}
                |""".trimMargin()
            )

            // Export something, so that the TypeScript validation tasks have declarations to validate.
            listOf("jsMain", "jsTest").forEach { sourceSet ->
                kotlinSourcesDir(sourceSet).resolve("Exported.kt").createParentDirectories().writeText(
                    """
                    |@OptIn(ExperimentalJsExport::class)
                    |@JsExport
                    |fun exportedFrom_${sourceSet}(): Int = 42
                    |""".trimMargin()
                )
            }

            // Keep Playwright away from the shared browsers cache in the user's home directory.
            val playwrightBrowsers = projectPath.resolve("playwright-browsers").createDirectories()

            var decamelizeVersion = "6.0.0"
            var leftPadVersion = "1.2.0"

            fun buildArguments() = arrayOf(
                "-PdecamelizeVersion=$decamelizeVersion",
                "-PleftPadVersion=$leftPadVersion",
                "-Pkotlin.gradle.playwright.browsers.path=$playwrightBrowsers",
            )

            // Development and production executables are synced into the same directory,
            // so Gradle refuses to run development and production tasks in one build.
            val (productionNpmTasks, otherNpmTasks) = expectedNpmTasks.partition { "Production" in it }

            fun buildNpmTasks(assertions: BuildResult.(tasks: List<String>) -> Unit) {
                listOf(otherNpmTasks, productionNpmTasks).forEach { tasks ->
                    build(*tasks.toTypedArray(), *buildArguments()) {
                        assertions(tasks)
                    }
                }
            }

            // Step 1: check that the project has exactly the npm tasks listed in the test.
            build("checkNpmTaskList", *buildArguments())

            // Step 2: run every npm task for the first time, so each one gets an execution history.
            buildNpmTasks { tasks ->
                assertTasksExecuted(tasks)
            }

            // Step 3: run again with nothing changed.
            // Tasks that track their state are up-to-date; the others always run.
            buildNpmTasks { tasks ->
                assertTasksUpToDate(tasks.filter { it in trackedNpmTasks })
                assertTasksExecuted(tasks.filter { it in untrackedNpmTasks })
            }

            // Step 4: change the npm dependencies declared with both the old and the new DSL.
            // Every task that was up-to-date in step 3 must run again.
            decamelizeVersion = "6.0.1"
            leftPadVersion = "1.3.0"
            buildNpmTasks { tasks ->
                assertTasksExecuted(tasks.filter { it in trackedNpmTasks })
            }
        }
    }

    companion object {
        private fun Project.disableNpmLockFileValidations() {
            plugins.withType(YarnPlugin::class.java) { _ ->
                extensions.getByType(YarnRootExtension::class.java).apply {
                    yarnLockMismatchReportProperty.set(YarnLockMismatchReport.NONE)
                    yarnLockAutoReplaceProperty.set(true)
                }
            }
            plugins.withType(WasmYarnPlugin::class.java) { _ ->
                extensions.getByType(WasmYarnRootExtension::class.java).apply {
                    yarnLockMismatchReportProperty.set(YarnLockMismatchReport.NONE)
                    yarnLockAutoReplaceProperty.set(true)
                }
            }
            plugins.withType(NodeJsRootPlugin::class.java) { _ ->
                extensions.getByType(NpmExtension::class.java).apply {
                    packageLockMismatchReport.set(LockFileMismatchReport.NONE)
                    packageLockAutoReplace.set(true)
                }
            }
            plugins.withType(WasmNodeJsRootPlugin::class.java) { _ ->
                extensions.getByType(WasmNpmExtension::class.java).apply {
                    packageLockMismatchReport.set(LockFileMismatchReport.NONE)
                    packageLockAutoReplace.set(true)
                }
            }
        }

    }
}

/**
 * [org.jetbrains.kotlin.gradle.targets.js.npm.RequiresNpmDependenciesTask]s that track their state,
 * so they are up-to-date when nothing changed.
 */
private val trackedNpmTasks = listOf(
    ":jsBrowserDevelopmentWebpack",
    ":jsBrowserProductionWebpack",
    ":jsBrowserTest",
    ":jsNodeTest",
    ":prepareWebpackBundleForKotlinJsTests",
    ":wasmJsBrowserDevelopmentWebpack",
    ":wasmJsBrowserProductionWebpack",
    ":wasmJsBrowserTest",
    ":wasmJsNodeTest",
    ":wasmJsTestBundleAsEsm",
)

/**
 * [org.jetbrains.kotlin.gradle.targets.js.npm.RequiresNpmDependenciesTask]s that always run.
 */
private val untrackedNpmTasks = listOf(
    // webpack dev servers don't track their state
    ":jsBrowserDevelopmentRun",
    ":jsBrowserProductionRun",
    ":wasmJsBrowserDevelopmentRun",
    ":wasmJsBrowserProductionRun",
    // NodeJsExec has no outputs
    ":jsNodeDevelopmentRun",
    ":jsNodeProductionRun",
    ":wasmJsNodeDevelopmentRun",
    ":wasmJsNodeProductionRun",
    // TypeScriptValidationTask has no outputs
    ":jsDevelopmentExecutableValidateGeneratedByCompilerTypeScript",
    ":jsProductionExecutableValidateGeneratedByCompilerTypeScript",
    ":jsTestTestDevelopmentExecutableValidateGeneratedByCompilerTypeScript",
    ":jsTestTestProductionExecutableValidateGeneratedByCompilerTypeScript",
    // the installed browsers are not a declared output
    ":kotlinInstallPlaywrightChromium",
)
