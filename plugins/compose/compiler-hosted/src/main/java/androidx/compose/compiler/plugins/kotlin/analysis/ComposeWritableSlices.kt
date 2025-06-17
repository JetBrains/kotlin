package androidx.compose.compiler.plugins.kotlin.analysis

import androidx.compose.compiler.plugins.kotlin.lower.KeyInfo
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.irFlag
import org.jetbrains.kotlin.util.slicedMap.BasicWritableSlice
import org.jetbrains.kotlin.util.slicedMap.RewritePolicy
import org.jetbrains.kotlin.util.slicedMap.WritableSlice

var IrExpression.isStaticFunctionExpression: Boolean by irFlag(copyByDefault = true)
var IrExpression.isStaticExpression: Boolean by irFlag(copyByDefault = true)

object ComposeWritableSlices {
    val IS_COMPOSABLE_SINGLETON: WritableSlice<IrElement, Boolean> =
        BasicWritableSlice(RewritePolicy.DO_NOTHING)
    val IS_COMPOSABLE_SINGLETON_CLASS: WritableSlice<IrElement, Boolean> =
        BasicWritableSlice(RewritePolicy.DO_NOTHING)
    val DURABLE_FUNCTION_KEY: WritableSlice<IrElement, KeyInfo> =
        BasicWritableSlice(RewritePolicy.DO_NOTHING)
    val HAS_TRANSFORMED_LAMBDA: WritableSlice<IrElement, Boolean> =
        BasicWritableSlice(RewritePolicy.DO_NOTHING)
    val IS_TRANSFORMED_LAMBDA: WritableSlice<IrElement, Boolean> =
        BasicWritableSlice(RewritePolicy.DO_NOTHING)
}
