/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abi.tools

import org.jetbrains.kotlin.abi.tools.api.AbiToolsInterface

public class AbiToolsFactory : org.jetbrains.kotlin.abi.tools.api.AbiToolsFactory {
    override fun get(): AbiToolsInterface = AbiTools
}
