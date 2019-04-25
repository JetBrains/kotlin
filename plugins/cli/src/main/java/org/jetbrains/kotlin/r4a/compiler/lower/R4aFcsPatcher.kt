package org.jetbrains.kotlin.r4a.compiler.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.r4a.R4aCallResolutionInterceptorExtension

class R4aFcsPatcher(val context: JvmBackendContext) :
    IrElementTransformerVoid(),
    FileLoweringPass {

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(this)
    }

    override fun visitCall(expression: IrCall): IrExpression {
        if (expression.descriptor is R4aCallResolutionInterceptorExtension.ComposableInvocationDescriptor) {
        //    System.out.println("******"+expression.descriptor.name)
        //    return IrBlockImpl(expression.startOffset, expression.endOffset, context.irBuiltIns.unitType, null, emptyList())
            TODO("Implement me")
        }
        return super.visitCall(expression)
    }
}
