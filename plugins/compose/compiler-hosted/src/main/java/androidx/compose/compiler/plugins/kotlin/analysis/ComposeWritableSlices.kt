package androidx.compose.compiler.plugins.kotlin.analysis

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.declarations.IrAttributeContainer
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.util.slicedMap.BasicWritableSlice
import org.jetbrains.kotlin.util.slicedMap.RewritePolicy
import org.jetbrains.kotlin.util.slicedMap.WritableSlice

object ComposeWritableSlices {
    val INFERRED_COMPOSABLE_DESCRIPTOR: WritableSlice<FunctionDescriptor, Boolean> =
        BasicWritableSlice(RewritePolicy.DO_NOTHING)
    val LAMBDA_CAPABLE_OF_COMPOSER_CAPTURE: WritableSlice<FunctionDescriptor, Boolean> =
        BasicWritableSlice(RewritePolicy.DO_NOTHING)
    val INFERRED_COMPOSABLE_LITERAL: WritableSlice<KtLambdaExpression, Boolean> =
        BasicWritableSlice(RewritePolicy.DO_NOTHING)
    val IS_COMPOSABLE_CALL: WritableSlice<IrAttributeContainer, Boolean> =
        BasicWritableSlice(RewritePolicy.DO_NOTHING)
    val IS_SYNTHETIC_COMPOSABLE_CALL: WritableSlice<IrFunctionAccessExpression, Boolean> =
        BasicWritableSlice(RewritePolicy.DO_NOTHING)
    val IS_STATIC_FUNCTION_EXPRESSION: WritableSlice<IrExpression, Boolean> =
        BasicWritableSlice(RewritePolicy.DO_NOTHING)
    val IS_COMPOSABLE_SINGLETON: WritableSlice<IrAttributeContainer, Boolean> =
        BasicWritableSlice(RewritePolicy.DO_NOTHING)
    val IS_COMPOSABLE_SINGLETON_CLASS: WritableSlice<IrAttributeContainer, Boolean> =
        BasicWritableSlice(RewritePolicy.DO_NOTHING)
}
