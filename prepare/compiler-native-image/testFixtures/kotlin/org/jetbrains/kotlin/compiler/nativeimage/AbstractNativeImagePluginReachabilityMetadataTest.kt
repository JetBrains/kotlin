/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compiler.nativeimage

import java.io.File

abstract class AbstractNativeImagePluginReachabilityMetadataTest : AbstractNativeImagePluginTest() {
    private val runner: ReachabilityMetadataGeneratingCompilerRunner by lazy { ReachabilityMetadataGeneratingCompilerRunner(javaHome) }

    override fun runCompiler(
        arguments: List<String>,
        classpath: List<File>,
    ): Pair<Int, String> = runner.run(
        workingDir = workingDir,
        arguments = arguments,
        classpath = classpath,
        jvmArgs = listOf("-Dkotlinc.test.allow.testonly.language.features=true"),
    )
}
