/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.compilerRunner

import org.gradle.api.Project
import org.jetbrains.kotlin.descriptors.commonizer.CliCommonizer

/**
 * Creates an instance of [CliCommonizer] that is backed by [KotlinNativeCommonizerToolRunner] to adhere to user defined settings
 * when executing the commonizer (like jvm arguments, running in separate process, etc)
 */
internal fun GradleCliCommonizer(project: Project): CliCommonizer {
    return CliCommonizer(CliCommonizer.Executor { arguments ->
        KotlinNativeCommonizerToolRunner(project).run(arguments)
    })
}
