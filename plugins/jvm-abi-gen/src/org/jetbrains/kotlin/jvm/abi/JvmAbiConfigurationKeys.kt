/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.abi

import org.jetbrains.kotlin.config.CompilerConfigurationKey

object JvmAbiConfigurationKeys {
    val OUTPUT_PATH: CompilerConfigurationKey<String> =
        CompilerConfigurationKey.create(JvmAbiCommandLineProcessor.OUTPUT_PATH_OPTION.description)
    val REMOVE_DEBUG_INFO: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create(JvmAbiCommandLineProcessor.REMOVE_DEBUG_INFO_OPTION.description)
    val REMOVE_COPY_ALONG_WITH_CONSTRUCTOR: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create(JvmAbiCommandLineProcessor.REMOVE_COPY_ALONG_WITH_CONSTRUCTOR_OPTION.description)
}
