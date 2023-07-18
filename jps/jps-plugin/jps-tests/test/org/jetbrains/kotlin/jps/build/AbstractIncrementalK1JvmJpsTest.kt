/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.build

import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments

abstract class AbstractIncrementalK1JvmJpsTest(
    allowNoFilesWithSuffixInTestData: Boolean = false
) : AbstractIncrementalJvmJpsTest(allowNoFilesWithSuffixInTestData = allowNoFilesWithSuffixInTestData) {
    override fun updateCommandLineArguments(arguments: CommonCompilerArguments) {
        if (KotlinVersion.CURRENT.major >= 2) {
            arguments.languageVersion = "1.9"
        }
        super.updateCommandLineArguments(arguments)
    }
}
