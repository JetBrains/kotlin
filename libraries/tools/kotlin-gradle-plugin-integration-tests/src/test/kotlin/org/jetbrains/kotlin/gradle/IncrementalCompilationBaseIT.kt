/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.BuildOptions
import org.jetbrains.kotlin.gradle.testbase.KGPBaseTest
import org.jetbrains.kotlin.gradle.testbase.TestProject
import org.jetbrains.kotlin.gradle.testbase.project

abstract class IncrementalCompilationBaseIT : KGPBaseTest() {

    protected abstract val defaultProjectName: String

    open fun defaultProject(
        gradleVersion: GradleVersion,
        buildOptions: BuildOptions = defaultBuildOptions,
        test: TestProject.() -> Unit = {}
    ): TestProject = project(
        defaultProjectName,
        gradleVersion,
        buildOptions,
        test = test
    )

    override val defaultBuildOptions = super.defaultBuildOptions.copy(
        incremental = true,
        logLevel = LogLevel.DEBUG
    )
}
