/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.backend

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlinx.atomicfu.compiler.backend.jvm.AtomicSymbols

abstract class AtomicfuIrTransformerBase(
    private val context: IrPluginContext,
    private val atomicSymbols: AtomicSymbols
) {

}
