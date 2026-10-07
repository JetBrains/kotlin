/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.errortolerance

import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.incremental.AbstractIncrementalJvmCompilerRunnerTest
import org.jetbrains.kotlin.incremental.testingUtils.BuildLogFinder
import java.io.File

/**
 * Incremental compilation with the error tolerance plugin enabled: compilation errors don't fail the build,
 * but files with tolerated errors are recompiled during the next build.
 */
abstract class AbstractErrorToleranceIncrementalTest : AbstractIncrementalJvmCompilerRunnerTest() {
    override fun createCompilerArguments(destinationDir: File, testDir: File): K2JVMCompilerArguments =
        super.createCompilerArguments(destinationDir, testDir).apply {
            pluginClasspaths = arrayOf(ForTestCompileRuntime.getFileFromProperty("errorTolerance.jar.path").canonicalPath)
        }

    override val buildLogFinder: BuildLogFinder
        get() = BuildLogFinder(isGradleEnabled = true, isFirEnabled = true)
}
