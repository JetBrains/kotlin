/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.testing.playwright

import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesTestExecutionSpec
import org.jetbrains.kotlin.gradle.targets.js.RequiredKotlinJsDependency
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.testing.JSServiceMessagesTestExecutionSpec
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTestFramework
import org.jetbrains.kotlin.gradle.utils.processes.ProcessLaunchOptions

class KotlinPlaywright(
    override val compilation: KotlinJsIrCompilation,
) : KotlinJsTestFramework {
    private val project get() = compilation.project

    override val settingsState: String
        get() = ""

    override val workingDir: Provider<Directory>
        get() = project.layout.buildDirectory.dir("playwright/${compilation.name}")

    override val executable: Provider<String>
        get() = project.provider { "" }

    override fun createTestExecutionSpec(
        task: KotlinJsTest,
        launchOpts: ProcessLaunchOptions,
        nodeJsArgs: MutableList<String>,
        debug: Boolean,
    ): TCServiceMessagesTestExecutionSpec {

    }

    override fun getPath(): String = "$workingDir:kotlinKarma"

    override val requiredNpmDependencies: Set<RequiredKotlinJsDependency>
        get() = emptySet()
}


internal class KotlinPlaywrightTestExecutionSpec : JSServiceMessagesTestExecutionSpec()
