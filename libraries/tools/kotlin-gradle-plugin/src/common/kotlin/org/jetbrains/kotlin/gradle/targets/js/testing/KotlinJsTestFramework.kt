/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.testing

import org.gradle.api.file.Directory
import org.gradle.api.internal.tasks.testing.TestExecuter
import org.gradle.api.internal.tasks.testing.TestExecutionSpec
import org.gradle.api.provider.Provider
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
