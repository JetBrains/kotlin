/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.KonanFqNames
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetObjectValue
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

/**
 * Dissolves references to CUDA namespace objects (e.g. `threadIdx`, `blockIdx`)
 * left over after inlining of their `inline get()` member properties.
 *
 * Those getters delegate to top-level `@GCUnsafeCall("llvm.nvvm.read.ptx.sreg.*")`
 * externals that ignore `this`, but the inliner still preserves an
 * `IrGetObjectValue` of the namespace object for evaluation-order semantics.
 * That preserved read anchors the singleton's class descriptor and initializer
 * in LLVM bitcode, which we don't want in GPU kernels.
 *
 * Runs after all inlining so that member-access sites have already been
 * collapsed to their inlined bodies.
 */
internal class CudaIntrinsicsLowering(val context: Context) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitGetObjectValue(expression: IrGetObjectValue): IrExpression {
                val target = expression.symbol.owner
                if (target.isCudaIntrinsicNamespace()) {
                    return IrCompositeImpl(expression.startOffset, expression.endOffset, context.irBuiltIns.unitType)
                }
                return super.visitGetObjectValue(expression)
            }
        })
    }

    private fun IrClass.isCudaIntrinsicNamespace(): Boolean {
        val pkg = parent as? IrPackageFragment ?: return false
        return pkg.packageFqName == KonanFqNames.cudaPackageName
    }
}
