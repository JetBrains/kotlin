/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(ExperimentalWasmDsl::class)

package org.jetbrains.kotlin.gradle.web

import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.kotlin
import org.gradle.kotlin.dsl.withType
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.npm.BaseNpmExtension
import org.jetbrains.kotlin.gradle.targets.js.npm.LockFileMismatchReport
import org.jetbrains.kotlin.gradle.targets.js.npm.RequiresNpmDependenciesTask
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockMismatchReport
import org.jetbrains.kotlin.gradle.targets.web.nodejs.CommonNodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.web.yarn.BaseYarnRootExtension
import org.jetbrains.kotlin.gradle.targets.web.yarn.CommonYarnPlugin
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.setupCustomKgpNpmToolingDependenciesDir
import kotlin.io.path.*
import kotlin.test.assertEquals

@GradleTestVersions(
    // Test does not depend on Gradle-version specifics.
    // If these tests fail in older Gradle versions, it's probably a Gradle bug.
    minVersion = TestVersions.Gradle.MAX_SUPPORTED
)
sealed class KotlinWebpackUpToDateIT(
    private val packageManager: String,
) : KGPBaseTest() {

    class Npm : KotlinWebpackUpToDateIT(packageManager = "npm")

    class Yarn : KotlinWebpackUpToDateIT(packageManager = "yarn")

    private val useYarn: Boolean =
        when (packageManager) {
            "yarn" -> true
            "npm" -> false
            else -> error("Unknown package manager: $packageManager")
        }

    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.disableIsolatedProjectsBecauseOfJsAndWasmKT75899()

    @GradleTest
    @JsGradlePluginTests
    fun `testChangingNpmDepVersion - js`(gradleVersion: GradleVersion) {
        testChangingNpmDepVersion(gradleVersion, buildTaskPath = ":sp1:jsBrowserDevelopmentWebpack")
    }

    @GradleTest
    @MppGradlePluginTests
    fun `testChangingNpmDepVersion - wasmjs`(gradleVersion: GradleVersion) {
        testChangingNpmDepVersion(gradleVersion, buildTaskPath = ":sp1:wasmJsBrowserDevelopmentWebpack")
    }

    private fun testChangingNpmDepVersion(
        gradleVersion: GradleVersion,
        buildTaskPath: String,
    ) {
        createTestProject(gradleVersion, useYarn = useYarn) {
            var cowsayVersion = "1.5.0"

            build(
                buildTaskPath,
                "-PcowsayVersion=$cowsayVersion",
                enableBuildCacheDebug = true,
            ) {
                assertTasksExecuted(buildTaskPath)
            }

            build(
                buildTaskPath,
                "-PcowsayVersion=$cowsayVersion",
                enableBuildCacheDebug = true,
            ) {
                assertTasksUpToDate(buildTaskPath)
            }

            cowsayVersion = "1.6.0"
            build(
                "upgradeAllLockFiles",
                buildTaskPath,
                "-PcowsayVersion=$cowsayVersion",
                enableBuildCacheDebug = true,
            ) {
                assertTasksExecuted(buildTaskPath)
            }
        }
    }

    @GradleTest
    @JsGradlePluginTests
    fun `test file npm dependency disables caching - js`(gradleVersion: GradleVersion) {
        testFileNpmDependencyDisablesCaching(gradleVersion, buildTaskPath = ":sp2:jsBrowserDevelopmentWebpack")
    }

    @GradleTest
    @MppGradlePluginTests
    fun `test file npm dependency disables caching - wasmjs`(gradleVersion: GradleVersion) {
        testFileNpmDependencyDisablesCaching(gradleVersion, buildTaskPath = ":sp2:wasmJsBrowserDevelopmentWebpack")
    }

    private fun testFileNpmDependencyDisablesCaching(
        gradleVersion: GradleVersion,
        buildTaskPath: String,
    ) {
        createTestProject(gradleVersion, useYarn = useYarn) {
            build(
                buildTaskPath,
                enableBuildCacheDebug = true,
            ) {
                assertTasksExecuted(buildTaskPath)

                val fileBasedNpmDepsLogLines =
                    outputReader.useLines { lines ->
                        lines.filter { buildTaskPath in it && "file-based NPM dependencies" in it }
                            .distinct()
                            .joinToString("\n")
                            .trim()
                    }

                val expectedFileBasedDepPath = projectPath.absolute().resolve("sp2/foo-npm-dep").pathString
                assertEquals(
                    "$buildTaskPath doNotCacheIf=true. Task depends on file-based NPM dependencies: [$expectedFileBasedDepPath]. See KT-86309.",
                    fileBasedNpmDepsLogLines,
                )
            }
        }
    }
}


/**
 * Create a test project for [KotlinWebpackUpToDateIT].
 *
 * Two subprojects:
 * - `sp1` depends on `cowsay` with a configurable version.
 * - `sp2` depends on a file-based npm dependency.
 *
 * Why use a single build with subprojects for this test, instead of separate builds?
 * Unfortunately, KGP uses cross-project configuration KT-80311.
 * Until this is remedied, we must test that multiple subprojects work correctly together,
 * and fixes works in subprojects, not just the root project.
 */
private fun KGPBaseTest.createTestProject(
    gradleVersion: GradleVersion,
    useYarn: Boolean,
    useCustomKgpToolingDir: Boolean = false,
    runTest: TestProject.() -> Unit,
) {

    project("empty", gradleVersion) {

        //region configure root project
        plugins {
            kotlin("multiplatform").apply(false)
        }

        if (useCustomKgpToolingDir) {
            setupCustomKgpNpmToolingDependenciesDir(
                toolingCustomDir = projectPath.resolve("kgp-npm-tooling"),
                useYarn = useYarn,
            )
        }

        //region disable lockfile checks
        // Lock file validation aren't relevant to this test, so just disable them to avoid UX issues KT-84782
        buildScriptInjection {
            project.plugins.withType<CommonNodeJsRootPlugin>().configureEach { _ ->
                project.extensions.configure<BaseNpmExtension> {
                    packageLockMismatchReport.set(LockFileMismatchReport.NONE)
                    packageLockAutoReplace.set(true)
                }
            }
            project.plugins.withType<CommonYarnPlugin>().configureEach { _ ->
                project.extensions.configure<BaseYarnRootExtension> {
                    yarnLockMismatchReportProperty.set(YarnLockMismatchReport.NONE)
                    yarnLockAutoReplaceProperty.set(true)
                }
            }

            project.tasks.register("upgradeAllLockFiles") { t ->
                t.description = "helper task to run all lock file upgrade tasks"
                if (useYarn) {
                    t.dependsOn(project.tasks.matching { "UpgradeYarnLock" in it.name })
                } else {
                    t.dependsOn(project.tasks.matching { "UpgradePackageLock" in it.name })
                }
            }
        }
        //endregion
        //endregion

        gradleProperties.appendText("\nkotlin.js.yarn=$useYarn")

        createSubproject("sp1") {
            projectPath.resolve("src/commonMain/kotlin/main.kt")
                .apply { parent.createDirectories() }
                .createFile()
                .writeText(
                    """
                    |package com.example
                    |
                    |fun main() {
                    |  println("Hello, world!")
                    |}
                    |""".trimMargin()
                )
            buildScriptInjection {
                project.plugins.apply("org.jetbrains.kotlin.multiplatform")

                kotlinMultiplatform.apply {
                    js {
                        browser()
                        binaries.executable()
                    }
                    wasmJs {
                        browser()
                        binaries.executable()
                    }

                    sourceSets.commonMain.dependencies {
                        val cowsayVersion = project.providers.gradleProperty("cowsayVersion").getOrElse("1.4.0")
                        project.logger.lifecycle("using cowsayVersion: $cowsayVersion")
                        implementation(npm("cowsay", cowsayVersion))
                    }
                }
            }
        }

        createSubproject("sp2") {
            val fooNpmDepName = "foo-npm-dep"

            projectPath.resolve(fooNpmDepName).apply {
                createDirectories()
                resolve("package.json").writeText(
                    """
                    |{
                    |  "name": "$fooNpmDepName",
                    |  "version": "1.0.0",
                    |  "main": "index.js",
                    |  "private": true
                    |}
                    |""".trimMargin()
                )
                resolve("index.js").writeText(
                    """
                    |console.log('Hello from $fooNpmDepName');
                    |""".trimMargin()
                )
            }

            projectPath.resolve("src/commonMain/kotlin/main.kt")
                .apply { parent.createDirectories() }
                .createFile()
                .writeText(
                    """
                    |package com.example
                    |
                    |fun main() {
                    |  println("Hello, world!")
                    |}
                    |""".trimMargin()
                )

            buildScriptInjection {
                project.plugins.apply("org.jetbrains.kotlin.multiplatform")

                kotlinMultiplatform.apply {
                    js {
                        browser()
                        binaries.executable()
                    }
                    wasmJs {
                        browser()
                        binaries.executable()
                    }

                    sourceSets.commonMain.dependencies {
                        implementation(npm(fooNpmDepName, project.projectDir.resolve(fooNpmDepName)))
                    }
                }

                project.tasks.withType<RequiresNpmDependenciesTask>().configureEach { task ->
                    task.doFirst { _ ->
                        // Log extra information for debugging purposes, in case of test failure on CI
                        task.logger.lifecycle(
                            "${task.path} npmDependenciesLockFiles:\n" +
                                    task.npmDependenciesLockFiles.files.joinToString("\n") { f -> f.invariantSeparatorsPath }
                        )
                    }
                }
            }
        }

        runTest()
    }
}

private fun TestProject.createSubproject(
    name: String,
    block: GradleProject.() -> Unit = {},
) {
    val sp = subProject(name)
    sp.projectPath
        .createDirectories()
        .resolve("build.gradle.kts").writeText("")
    settingsBuildScriptInjection {
        settings.include(":${name}")
    }
    block(sp)
}
