/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.testing

import org.gradle.api.file.Directory
import org.gradle.api.internal.tasks.testing.TestExecuter
import org.gradle.api.internal.tasks.testing.TestExecutionSpec
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.jetbrains.kotlin.gradle.targets.js.npm.RequiresNpmDependencies
import org.jetbrains.kotlin.gradle.utils.processes.ProcessLaunchOptions

interface KotlinJsTestFramework : RequiresNpmDependencies {
    val settingsState: String

    val workingDir: Provider<Directory>

    val executable: Provider<String>

    fun createTestExecutionSpec(
        task: KotlinJsTest,
        launchOpts: ProcessLaunchOptions,
        nodeJsArgs: MutableList<String>,
        debug: Boolean,
    ): TestExecutionSpec

    /** Will be assigned to task property with @[org.gradle.api.tasks.Nested] annotation. So it must comply with its requirements */
    val frameworkTaskInputs: Any?
        get() = null

    /**  */
    fun createTestExecuter(): TestExecuter<*>? = null

    companion object
}

internal const val DEFAULT_DEBUG_PORT = 9222
internal const val DEFAULT_DEBUGGER_READY_TIMEOUT_MILLIS = 60_000

internal interface KotlinJsBrowserDebugOptions {
    /** Name of the browser runner to debug. It must be a Chromium one, the framework rejects the rest. */
    @get:Input
    val runnerName: Property<String>

    @get:Input
    val debugPort: Property<Int>

    /** Set only when the run should wait until a debugger is attached. */
    @get:Input
    @get:Optional
    val debuggerReadyPort: Property<Int>

    @get:Input
    val debuggerReadyTimeoutMillis: Property<Int>
}

internal interface KotlinJsBrowserDebuggableFramework {
    /** Set only for a run that is being debugged, so an unset value means no browser debugging. */
    @get:Nested
    @get:Optional
    val kotlinJsBrowserDebugOptions: Property<KotlinJsBrowserDebugOptions>
}
