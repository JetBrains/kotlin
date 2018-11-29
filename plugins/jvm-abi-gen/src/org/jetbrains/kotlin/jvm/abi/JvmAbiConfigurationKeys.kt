/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.abi

import org.jetbrains.kotlin.config.CompilerConfigurationKey

object JvmAbiConfigurationKeys {
    val OUTPUT_DIR: CompilerConfigurationKey<String> =
        CompilerConfigurationKey.create<String>(JvmAbiCommandLineProcessor.OUTPUT_DIR_OPTION.description)

}