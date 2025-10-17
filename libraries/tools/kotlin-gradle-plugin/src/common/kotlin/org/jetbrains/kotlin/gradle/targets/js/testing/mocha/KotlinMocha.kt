/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.testing.mocha

import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.process.ProcessForkOptions
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesClientSettings
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesTestExecutionSpec
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.targets.js.RequiredKotlinJsDependency
import org.jetbrains.kotlin.gradle.targets.js.internal.parseNodeJsStackTraceAsJvm
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin.Companion.kotlinNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin.Companion.kotlinNodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProjectModules
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTestFramework
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTestFramework.Companion.CREATE_TEST_EXEC_SPEC_DEPRECATION_MSG
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTestFramework.Companion.createTestExecutionSpecDeprecated
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinTestRunnerCliArgs
import org.jetbrains.kotlin.gradle.targets.js.webTargetVariant
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.gradle.utils.getValue
import org.jetbrains.kotlin.gradle.utils.processes.ProcessLaunchOptions
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootPlugin.Companion.kotlinNodeJsRootExtension as wasmKotlinNodeJsRootExtension

class KotlinMocha internal constructor(
    @Transient
    override val compilation: KotlinJsIrCompilation,
    private val basePath: String,
    private val objects: ObjectFactory,
    private val providers: ProviderFactory,
) :
    KotlinJsTestFramework {

    @Deprecated(
        "Manually creating instances of this class is deprecated. Scheduled for removal in Kotlin 2.4.",
        level = DeprecationLevel.ERROR
    )
    constructor(
        compilation: KotlinJsIrCompilation,
        basePath: String,
    ) : this(
        compilation = compilation,
        basePath = basePath,
        objects = compilation.target.project.objects,
        providers = compilation.target.project.providers,
    )

    @Transient
    private val project: Project = compilation.target.project
    private val npmProject = compilation.npmProject

    @Transient
    private val nodeJsRoot = compilation.webTargetVariant(
        { project.rootProject.kotlinNodeJsRootExtension },
        { project.rootProject.wasmKotlinNodeJsRootExtension },
    )

    private val versions by lazy {
        nodeJsRoot.versions
    }

    private val npmProjectDir by project.provider { npmProject.dir }

    @Transient
    private val nodeJs = project.kotlinNodeJsEnvSpec

    override val workingDir: Provider<Directory>
        get() = npmProjectDir

    override val executable: Provider<String> = nodeJs.executable

    override val settingsState: String
        get() = "mocha"

    override val requiredNpmDependencies: Set<RequiredKotlinJsDependency>
        get() = setOf(
            versions.mocha,
            versions.sourceMapSupport,
            versions.kotlinWebHelpers,
        )

    override fun getPath() = "$basePath:kotlinMocha"

    // https://mochajs.org/#-timeout-ms-t-ms
    var timeout: String = DEFAULT_TIMEOUT

    private val platformType = compilation.platformType

    override fun createTestExecutionSpec(
        task: KotlinJsTest,
        launchOpts: ProcessLaunchOptions,
        nodeJsArgs: MutableList<String>,
        debug: Boolean,
    ): TCServiceMessagesTestExecutionSpec {
        val clientSettings = TCServiceMessagesClientSettings(
            task.name,
            testNameSuffix = task.targetName,
            prependSuiteName = true,
            stackTraceParser = ::parseNodeJsStackTraceAsJvm,
            ignoreOutOfRootNodes = true,
        )

        val cliArgs = KotlinTestRunnerCliArgs(
            include = task.includePatterns,
            exclude = task.excludePatterns
        )

        val modules = NpmProjectModules(npmProjectDir.getFile())

        val mocha = modules.require("mocha/bin/mocha")

        val file = task.inputFileProperty.getFile().toString()

        val args = nodeJsArgs + mutableListOf(
            "--require",
            modules.require("source-map-support/register.js")
        ).apply {
            if (debug) {
                add("--inspect-brk")
            }
            add(mocha)
            add(file)
            addAll(cliArgs.toList())
            addAll(cliArg("--reporter", modules.require("kotlin-web-helpers/dist/mocha-kotlin-reporter.js")))
            addAll(cliArg("--require", modules.require("kotlin-web-helpers/dist/kotlin-test-nodejs-runner.js")))
            if (debug) {
                add(NO_TIMEOUT_ARG)
            } else {
                addAll(cliArg(TIMEOUT_ARG, timeout))
            }
        }

        val dryRunArgs = if (platformType == KotlinPlatformType.wasm)
            null
        else {
            nodeJsArgs + mutableListOf(
                "--require",
                modules.require("source-map-support/register.js")
            ).apply {
                add(mocha)
                add(file)
                addAll(cliArgs.toList())
                addAll(cliArg("--require", modules.require("kotlin-web-helpers/dist/kotlin-test-nodejs-empty-runner.js")))
            }
        }

        return TCServiceMessagesTestExecutionSpec(
            processLaunchOptions = launchOpts,
            processArgs = args,
            checkExitCode = false,
            clientSettings = clientSettings,
            dryRunArgs = dryRunArgs,
        )
    }

    private fun cliArg(cli: String, value: String?): List<String> {
        return value?.let { listOf(cli, it) } ?: emptyList()
    }

    @Deprecated(
        CREATE_TEST_EXEC_SPEC_DEPRECATION_MSG,
        ReplaceWith("createTestExecutionSpec(task, launchOpts, nodeJsArgs, debug)"),
        DeprecationLevel.ERROR
    )
    override fun createTestExecutionSpec(
        task: KotlinJsTest,
        forkOptions: ProcessForkOptions,
        nodeJsArgs: MutableList<String>,
        debug: Boolean,
    ): TCServiceMessagesTestExecutionSpec =
        createTestExecutionSpecDeprecated(
            task = task,
            forkOptions = forkOptions,
            nodeJsArgs = nodeJsArgs,
            debug = debug,
            objects = objects,
            providers = providers,
        )

    companion object {
        private const val DEFAULT_TIMEOUT = "2s"
    }
}

private const val TIMEOUT_ARG = "--timeout"
private const val NO_TIMEOUT_ARG = "--no-timeout"
