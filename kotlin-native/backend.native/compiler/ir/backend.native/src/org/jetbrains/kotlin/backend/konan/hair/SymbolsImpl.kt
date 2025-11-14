/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.hair

import hair.sym.HairFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction

internal data class HairFunctionImpl(val irFunction: IrSimpleFunction) : HairFunction {
    override fun toString() = irFunction.name.toString()
}
