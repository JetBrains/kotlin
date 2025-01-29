/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.gradle.targets.wasm.runtime

import org.gradle.api.file.Directory
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.process.ProcessForkOptions
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesClientSettings
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesTestExecutionSpec
import org.jetbrains.kotlin.gradle.targets.js.RequiredKotlinJsDependency
import org.jetbrains.kotlin.gradle.targets.js.internal.parseNodeJsStackTraceAsJvm
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTestFramework
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTestFramework.Companion.CREATE_TEST_EXEC_SPEC_DEPRECATION_MSG
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTestFramework.Companion.createTestExecutionSpecDeprecated
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinTestRunnerCliArgs
import org.jetbrains.kotlin.gradle.utils.processes.ProcessLaunchOptions
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly

internal class CommonKotlinWasmTestFramework(
    kotlinJsTest: KotlinJsTest,
    private val name: String,
    private val objects: ObjectFactory,
    private val providers: ProviderFactory,
) : KotlinJsTestFramework {
    override val settingsState: String = "KotlinWasm${name.capitalizeAsciiOnly()}"

    private val testPath = kotlinJsTest.path

    @Transient
    override val compilation: KotlinJsIrCompilation = kotlinJsTest.compilation

    private val projectLayout = kotlinJsTest.project.layout

    override val workingDir: Provider<Directory> =
        projectLayout.dir(
            kotlinJsTest.inputFileProperty.asFile.map { it.parentFile.parentFile.resolve("static/$name") }
        )

    override val executable: Property<String> = kotlinJsTest.project.objects.property(String::class.java)

    val argsProperty: Property<ArgsProvider> = kotlinJsTest.project.objects.property(ArgsProvider::class.java)

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

        val args = mutableListOf<String>()

        val workingDirFile = workingDir.get().asFile

        workingDirFile.mkdirs()

        with(args) {
            addAll(
                argsProperty.get().provideArgs(
                    workingDirFile.toPath(),
                    task.inputFileProperty.map {
                        it.asFile.parentFile.resolve(it.asFile.nameWithoutExtension + ".wasm")
                    }.get().toPath()
                )
            )
            addAll(cliArgs.toList())
        }

        return TCServiceMessagesTestExecutionSpec(
            processLaunchOptions = launchOpts,
            processArgs = args,
            checkExitCode = false,
            clientSettings = clientSettings,
            dryRunArgs = null,
        )
    }

    override val requiredNpmDependencies: Set<RequiredKotlinJsDependency> = emptySet()

    override fun getPath(): String = "$testPath:kotlinTestFrameworkStub"
}