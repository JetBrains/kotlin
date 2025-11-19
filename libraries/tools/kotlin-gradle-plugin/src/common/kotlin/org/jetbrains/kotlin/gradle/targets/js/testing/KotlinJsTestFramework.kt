/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.testing

import org.gradle.api.file.Directory
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.process.ProcessForkOptions
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesTestExecutionSpec
import org.jetbrains.kotlin.gradle.targets.js.npm.RequiresNpmDependencies
import org.jetbrains.kotlin.gradle.utils.processes.ProcessLaunchOptions
import org.jetbrains.kotlin.gradle.utils.processes.ProcessLaunchOptions.Companion.processLaunchOptions

/**
 * Base options used to control how JS, WasmJS, and WasmWASI tests are executed.
 *
 * **Note:** This interface is not intended for implementation by build script or plugin authors.
 *
 * @see KotlinJsTest
 */
interface KotlinJsTestFramework : RequiresNpmDependencies {

    /**
     * Provide a string that will be registered as a task input of [KotlinJsTest].
     *
     * The task will re-execute when this string has changed.
     *
     * It can be used to encode configuration specific to the implemented test framework.
     *
     * @see KotlinJsTest.testFrameworkSettings
     */
    // Note: currently the JS tests aren't build-cacheable KT-78586,
    // and they don't use the Provider API KT-77134.
    // This string encodes non-relocatable inputs (e.g. file paths)
    val settingsState: String

    /**
     *
     */
    val workingDir: Provider<Directory>

    /**
     *
     */
    val executable: Provider<String>

    @Deprecated(
        CREATE_TEST_EXEC_SPEC_DEPRECATION_MSG,
        level = DeprecationLevel.ERROR
    )
    fun createTestExecutionSpec(
        task: KotlinJsTest,
        forkOptions: ProcessForkOptions,
        nodeJsArgs: MutableList<String>,
        debug: Boolean,
    ): TCServiceMessagesTestExecutionSpec

    /**
     *
     */
    fun createTestExecutionSpec(
        task: KotlinJsTest,
        launchOpts: ProcessLaunchOptions,
        nodeJsArgs: MutableList<String>,
        debug: Boolean,
    ): TCServiceMessagesTestExecutionSpec

    companion object {

        internal const val CREATE_TEST_EXEC_SPEC_DEPRECATION_MSG =
            "Replaced with a new method that uses ProcessLaunchOptions instead of Gradle's ProcessForkOptions. " +
                    "Scheduled for removal in Kotlin 2.4."

        /**
         * Adapter for the deprecated [KotlinJsTestFramework.createTestExecutionSpec].
         * Can be re-used in subtypes of [KotlinJsTestFramework].
         *
         * (The intention is to avoid injecting [ObjectFactory] and [ProviderFactory] into
         * [KotlinJsTestFramework], which is awkward and verbose.)
         */
        internal fun KotlinJsTestFramework.createTestExecutionSpecDeprecated(
            task: KotlinJsTest,
            forkOptions: ProcessForkOptions,
            nodeJsArgs: MutableList<String>,
            debug: Boolean,
            objects: ObjectFactory,
            providers: ProviderFactory,
        ): TCServiceMessagesTestExecutionSpec =
            createTestExecutionSpec(
                task = task,
                launchOpts = objects.processLaunchOptions {
                    this.workingDir.set(forkOptions.workingDir)
                    this.executable.set(forkOptions.executable)
                    this.environment.set(
                        providers.provider { forkOptions.environment.mapValues { it.value.toString() } }
                    )
                },
                nodeJsArgs = nodeJsArgs,
                debug = debug
            )

    }
}
