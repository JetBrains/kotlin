/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.process.ProcessForkOptions
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesTestExecutionSpec
import org.jetbrains.kotlin.gradle.targets.js.npm.RequiresNpmDependencies

interface KotlinJsIrTestFramework : RequiresNpmDependencies {
    val settingsState: String

    fun createTestExecutionSpec(
        task: KotlinJsIrTest,
        forkOptions: ProcessForkOptions,
        nodeJsArgs: MutableList<String>,
        debug: Boolean
    ): TCServiceMessagesTestExecutionSpec

    /* because KotlinJsTestFramework override it too
    override val nodeModulesRequired: Boolean
        get() = true
     */
}