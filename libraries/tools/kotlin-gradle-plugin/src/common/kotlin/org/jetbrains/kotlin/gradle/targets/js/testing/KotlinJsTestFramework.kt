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

interface KotlinJsTestFramework : RequiresNpmDependencies {
    val settingsState: String

    val workingDir: Provider<Directory>

    val executable: Provider<String>

    @Deprecated(message = createTestExecutionSpecDeprecationMsg)
    fun createTestExecutionSpec(
        task: KotlinJsTest,
        forkOptions: ProcessForkOptions,
        nodeJsArgs: MutableList<String>,
        debug: Boolean,
    ): TCServiceMessagesTestExecutionSpec

    fun createTestExecutionSpec(
        task: KotlinJsTest,
        launchOpts: ProcessLaunchOptions,
        nodeJsArgs: MutableList<String>,
        debug: Boolean,
    ): TCServiceMessagesTestExecutionSpec

    companion object {

        @Suppress("ConstPropertyName")
        internal const val createTestExecutionSpecDeprecationMsg =
            "Replaced with a new method that uses ProcessLaunchOptions instead of Gradle's ProcessForkOptions."

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
