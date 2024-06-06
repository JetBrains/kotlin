/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.Action
import org.gradle.api.Task

interface SubTargetConfigurator<BuildTask : Task, RunTask : Task> {

    fun setupBuild(compilation: KotlinJsIrCompilation)

    fun configureBuild(body: Action<BuildTask>)

    fun setupRun(compilation: KotlinJsIrCompilation)

    fun configureRun(body: Action<RunTask>)
}