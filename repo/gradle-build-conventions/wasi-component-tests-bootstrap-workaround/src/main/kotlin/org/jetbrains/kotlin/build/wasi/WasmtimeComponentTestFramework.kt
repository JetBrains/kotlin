/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.wasi

import org.gradle.api.internal.tasks.testing.TestExecutionSpec
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesTestExecutionSpec
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTestFramework
import org.jetbrains.kotlin.gradle.utils.processes.ProcessLaunchOptions
import java.io.File

/**
 * TODO(remove after bootstrap): Runs the tests of [delegate], the `KotlinWasmtime` of the bootstrap KGP, on [component],
 *  not on the core module. `--invoke startUnitTests <core module>` becomes `--invoke startunittests() <component>`,
 *  because `--invoke` takes a call expression for a component. [delegate] does all other work, for example the parsing
 *  of the TeamCity service messages.
 */
internal class WasmtimeComponentTestFramework(
    private val delegate: KotlinJsTestFramework,
    private val component: File,
) : KotlinJsTestFramework by delegate {
    override val settingsState: String
        get() = "WasmtimeComponentTestFramework(${delegate.settingsState})"

    override fun createTestExecutionSpec(
        task: KotlinJsTest,
        launchOpts: ProcessLaunchOptions,
        nodeJsArgs: MutableList<String>,
        debug: Boolean,
    ): TestExecutionSpec {
        val spec = delegate.createTestExecutionSpec(task, launchOpts, nodeJsArgs, debug) as TCServiceMessagesTestExecutionSpec
        val coreModule = task.inputFileProperty.get().asFile.absolutePath

        fun List<String>.onComponent(): List<String> = map { argument ->
            when (argument) {
                BOOTSTRAP_UNIT_TESTS_EXPORT_NAME -> "$WASI_UNIT_TESTS_EXPORT_NAME()"
                coreModule -> component.absolutePath
                else -> argument
            }
        }

        return TCServiceMessagesTestExecutionSpec(
            processLaunchOptions = launchOpts,
            processArgs = spec.processArgs.onComponent(),
            checkExitCode = spec.checkExitCode,
            clientSettings = spec.clientSettings,
            dryRunArgs = spec.dryRunArgs?.onComponent(),
        )
    }
}
