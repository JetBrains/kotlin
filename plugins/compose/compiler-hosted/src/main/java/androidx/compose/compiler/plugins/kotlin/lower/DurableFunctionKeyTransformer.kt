/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package androidx.compose.compiler.plugins.kotlin.lower

import androidx.compose.compiler.plugins.kotlin.ComposeClassIds
import androidx.compose.compiler.plugins.kotlin.FeatureFlags
import androidx.compose.compiler.plugins.kotlin.ModuleMetrics
import androidx.compose.compiler.plugins.kotlin.analysis.ComposeWritableSlices.DURABLE_FUNCTION_KEY
import androidx.compose.compiler.plugins.kotlin.analysis.StabilityInferencer
import androidx.compose.compiler.plugins.kotlin.irTrace
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

class KeyInfo(
    val name: String,
    val startOffset: Int,
    val endOffset: Int,
    val hasDuplicates: Boolean,
) {
    var used: Boolean = false
    val key: Int get() = name.hashCode()
}

/**
 * This transform will generate a "durable" and mostly unique key for every function in the module.
 * In this case "durable" means that when the code is edited over time, a function with the same
 * semantic identity will usually have the same key each time it is compiled. This is important so
 * that new code can be recompiled and the key that the function gets after that recompile ought to
 * be the same as before, so one could inject this new code and signal to the runtime that
 * composable functions with that key should be considered invalid.
 *
 * This transform runs early on in the lowering pipeline, and stores the keys for every function in
 * the file in the BindingTrace for each function. These keys are then retrieved later on by other
 * lowerings and marked as used. After all lowerings have completed, one can use the
 * [realizeKeyMetaAnnotations] method to generate additional annotations
 * with the keys of each function and their source locations for tooling to utilize.
 *
 * For example, this transform will run on code like the following:
 *
 *     @Composable fun Example() {
 *       Box {
 *          Text("Hello World")
 *       }
 *     }
 *
 * And produce code like the following:
 *
 *     @FunctionKeyMeta(key=123, startOffset=24, endOffset=56)
 *     @Composable fun Example() {
 *       startGroup(123)
 *       Box {
 *         startGroup(345)
 *         Text("Hello World")
 *         endGroup()
 *       }
 *       endGroup()
 *     }
 *
 * @see DurableKeyVisitor
 */
class DurableFunctionKeyTransformer(
    context: IrPluginContext,
    metrics: ModuleMetrics,
    stabilityInferencer: StabilityInferencer,
    featureFlags: FeatureFlags,
) : DurableKeyTransformer(
    DurableKeyVisitor(),
    context,
    stabilityInferencer,
    metrics,
    featureFlags,
) {

    fun realizeKeyMetaAnnotations(moduleFragment: IrModuleFragment) {
        moduleFragment.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
                val functionKey = context.irTrace[DURABLE_FUNCTION_KEY, declaration] ?: return declaration
                if (!declaration.hasComposableAnnotation()) return declaration
                if (declaration.hasAnnotation(ComposeClassIds.FunctionKeyMeta)) return declaration

                declaration.annotations += irKeyMetaAnnotation(functionKey)
                return super.visitSimpleFunction(declaration)
            }
        })
    }


    private val keyMetaAnnotation =
        getTopLevelClassOrNull(ComposeClassIds.FunctionKeyMeta)

    private fun irKeyMetaAnnotation(
        key: KeyInfo,
    ): IrConstructorCall = IrConstructorCallImpl(
        UNDEFINED_OFFSET,
        UNDEFINED_OFFSET,
        keyMetaAnnotation!!.defaultType,
        keyMetaAnnotation.constructors.single(),
        typeArgumentsCount = 0,
        constructorTypeArgumentsCount = 0,
    ).apply {
        putValueArgument(0, irConst(key.key.hashCode()))
        putValueArgument(1, irConst(key.startOffset))
        putValueArgument(2, irConst(key.endOffset))
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
        val signature = declaration.signatureString()
        val (fullName, success) = buildKey("fun-$signature")
        val info = KeyInfo(
            fullName,
            declaration.startOffset,
            declaration.endOffset,
            !success,
        )
        context.irTrace.record(DURABLE_FUNCTION_KEY, declaration, info)
        return super.visitSimpleFunction(declaration)
    }
}
