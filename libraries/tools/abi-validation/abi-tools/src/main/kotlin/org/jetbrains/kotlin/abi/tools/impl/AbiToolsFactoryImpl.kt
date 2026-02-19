/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abi.tools.impl

import org.jetbrains.kotlin.abi.tools.AbiTools
import org.jetbrains.kotlin.abi.tools.AbiToolsFactory

internal class AbiToolsFactoryImpl : AbiToolsFactory {
    override fun get(): AbiTools = AbiToolsImpl
}