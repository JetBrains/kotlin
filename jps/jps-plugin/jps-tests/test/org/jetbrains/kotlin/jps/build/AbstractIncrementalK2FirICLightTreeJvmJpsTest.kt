/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.build

import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.incremental.testingUtils.BuildLogFinder
import org.jetbrains.kotlin.jps.model.k2JvmCompilerArguments

abstract class AbstractIncrementalK2FirICLightTreeJvmJpsTest(
    allowNoFilesWithSuffixInTestData: Boolean = false
) : AbstractIncrementalJpsTest(allowNoFilesWithSuffixInTestData = allowNoFilesWithSuffixInTestData) {
    override fun overrideModuleSettings() {
        myProject.k2JvmCompilerArguments = K2JVMCompilerArguments().also {
            it.useIR = true
        }
    }

    override fun updateCommandLineArguments(arguments: CommonCompilerArguments) {
        additionalCommandLineArguments = additionalCommandLineArguments + listOf("-Xuse-k2", "-Xuse-fir-ic", "-Xuse-fir-lt")
        super.updateCommandLineArguments(arguments)
    }

    override val buildLogFinder: BuildLogFinder
        get() = BuildLogFinder(isJpsBuild = true, isFirEnabled = true)
}
