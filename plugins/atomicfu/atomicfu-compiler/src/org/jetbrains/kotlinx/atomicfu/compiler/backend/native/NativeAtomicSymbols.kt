/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.backend.native

import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlinx.atomicfu.compiler.backend.AtomicSymbols

class NativeAtomicSymbols(
    irBuiltIns: IrBuiltIns,
    moduleFragment: IrModuleFragment
) : AtomicSymbols(irBuiltIns, moduleFragment) {

}
