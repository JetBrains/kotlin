/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.InteropFqNames
import org.jetbrains.kotlin.backend.konan.reportCompilationError
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrInlinedFunctionBlock
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

internal class SpecialObjCValidationLowering(val context: Context) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.acceptChildrenVoid(object : IrVisitorVoid() {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitInlinedFunctionBlock(inlinedBlock: IrInlinedFunctionBlock) {
                if (inlinedBlock.inlinedFunctionSymbol?.owner?.isAutoreleasepool() == true) {
                    // Prohibit calling suspend functions from `autoreleasepool {}` block.
                    // See https://youtrack.jetbrains.com/issue/KT-50786 for more details.
                    // Note: we can't easily check this in frontend, because we need to prohibit indirect cases like
                    ///    inline fun <T> myAutoreleasepool(block: () -> T) = autoreleasepool(block)
                    ///    myAutoreleasepool { suspendHere() }

                    inlinedBlock.acceptVoid(object : IrVisitorVoid() {
                        override fun visitElement(element: IrElement) {
                            element.acceptChildrenVoid(this)
                        }

                        override fun visitClass(declaration: IrClass) {
                            // Skip local declarations.
                        }

                        override fun visitFunction(declaration: IrFunction) {
                            // Skip local declarations.
                        }

                        override fun visitCall(expression: IrCall) {
                            super.visitCall(expression)

                            if (expression.symbol.owner.isSuspend) {
                                val message = "Calling suspend functions from `autoreleasepool {}` is prohibited, " +
                                        "see https://youtrack.jetbrains.com/issue/KT-50786"
                                context.reportCompilationError(message, irFile, expression)
                            }
                        }
                    })
                }
                super.visitInlinedFunctionBlock(inlinedBlock)
            }
        })
    }

    private fun IrFunction.isAutoreleasepool(): Boolean {
        return this.name.asString() == "autoreleasepool" && this.parent.let { parent ->
            parent is IrPackageFragment && parent.packageFqName == InteropFqNames.packageName
        }
    }
}
