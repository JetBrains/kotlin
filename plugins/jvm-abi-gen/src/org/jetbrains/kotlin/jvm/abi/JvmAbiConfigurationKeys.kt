/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.abi

import org.jetbrains.kotlin.config.CompilerConfigurationKey

object JvmAbiConfigurationKeys {
    val JVM_ABI_OUTPUT_PATH: CompilerConfigurationKey<String> =
        CompilerConfigurationKey.create("JVM_ABI_OUTPUT_PATH")
    val JVM_ABI_REMOVE_DEBUG_INFO: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create("JVM_ABI_REMOVE_DEBUG_INFO")
    val JVM_ABI_REMOVE_DATA_CLASS_COPY_IF_CONSTRUCTOR_IS_PRIVATE: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create("JVM_ABI_REMOVE_DATA_CLASS_COPY_IF_CONSTRUCTOR_IS_PRIVATE")
    val JVM_ABI_PRESERVE_DECLARATION_ORDER: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create("JVM_ABI_PRESERVE_DECLARATION_ORDER")
    val JVM_ABI_REMOVE_PRIVATE_CLASSES: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create("JVM_ABI_REMOVE_PRIVATE_CLASSES")
    val JVM_ABI_TREAT_INTERNAL_AS_PRIVATE: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create("JVM_ABI_TREAT_INTERNAL_AS_PRIVATE")
}
