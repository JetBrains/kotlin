package androidx.compose.compiler.plugins.kotlin.analysis

import androidx.compose.compiler.plugins.kotlin.lower.KeyInfo
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.irFlag

var IrExpression.isStaticFunctionExpression: Boolean by irFlag(copyByDefault = true)
var IrExpression.isStaticExpression: Boolean by irFlag(copyByDefault = true)
var IrElement.isComposableSingleton: Boolean by irFlag(copyByDefault = true)
var IrElement.isComposableSingletonClass: Boolean by irFlag(copyByDefault = true)
var IrElement.durableFunctionKey: KeyInfo? by irAttribute(copyByDefault = true)
var IrElement.hasTransformedLambda: Boolean by irFlag(copyByDefault = true)
var IrElement.isTransformedLambda: Boolean by irFlag(copyByDefault = true)
