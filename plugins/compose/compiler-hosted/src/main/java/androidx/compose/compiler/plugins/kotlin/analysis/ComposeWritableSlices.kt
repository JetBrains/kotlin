package androidx.compose.compiler.plugins.kotlin.analysis

import androidx.compose.compiler.plugins.kotlin.FunctionMetrics
import androidx.compose.compiler.plugins.kotlin.lower.KeyInfo
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.util.slicedMap.BasicWritableSlice
import org.jetbrains.kotlin.util.slicedMap.RewritePolicy
import org.jetbrains.kotlin.util.slicedMap.WritableSlice

object ComposeWritableSlices {
    val IS_SYNTHETIC_COMPOSABLE_CALL: WritableSlice<IrFunctionAccessExpression, Boolean> =
        BasicWritableSlice(RewritePolicy.DO_NOTHING)
    val IS_STATIC_FUNCTION_EXPRESSION: WritableSlice<IrExpression, Boolean> =
        BasicWritableSlice(RewritePolicy.DO_NOTHING)
    val IS_STATIC_EXPRESSION: WritableSlice<IrExpression, Boolean> =
        BasicWritableSlice(RewritePolicy.DO_NOTHING)
    val IS_COMPOSABLE_SINGLETON: WritableSlice<IrElement, Boolean> =
        BasicWritableSlice(RewritePolicy.DO_NOTHING)
    val IS_COMPOSABLE_SINGLETON_CLASS: WritableSlice<IrElement, Boolean> =
        BasicWritableSlice(RewritePolicy.DO_NOTHING)
    val DURABLE_FUNCTION_KEY: WritableSlice<IrElement, KeyInfo> =
        BasicWritableSlice(RewritePolicy.DO_NOTHING)
    val DURABLE_FUNCTION_KEYS: WritableSlice<IrElement, List<KeyInfo>> =
        BasicWritableSlice(RewritePolicy.DO_NOTHING)
    val HAS_TRANSFORMED_LAMBDA: WritableSlice<IrElement, Boolean> =
        BasicWritableSlice(RewritePolicy.DO_NOTHING)
    val IS_TRANSFORMED_LAMBDA: WritableSlice<IrElement, Boolean> =
        BasicWritableSlice(RewritePolicy.DO_NOTHING)
    val FUNCTION_METRICS: WritableSlice<IrElement, FunctionMetrics> =
        BasicWritableSlice(RewritePolicy.DO_NOTHING)
    val IS_VIRTUAL_WITH_DEFAULT_PARAM: WritableSlice<IrElement, Boolean> =
        BasicWritableSlice(RewritePolicy.DO_NOTHING)
}
