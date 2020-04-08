/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.compiler.ir

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.jvm.compiler.AbstractLoadJavaWithPsiClassReadingTest

abstract class AbstractIrLoadJavaTest : AbstractLoadJavaWithPsiClassReadingTest() {
    override fun updateConfiguration(configuration: CompilerConfiguration) = configuration.put(JVMConfigurationKeys.IR, true)
}
