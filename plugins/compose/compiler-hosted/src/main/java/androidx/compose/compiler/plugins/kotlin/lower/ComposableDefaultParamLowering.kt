/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.compiler.plugins.kotlin.lower

import androidx.compose.compiler.plugins.kotlin.ComposeNames
import androidx.compose.compiler.plugins.kotlin.FeatureFlags
import androidx.compose.compiler.plugins.kotlin.ModuleMetrics
import androidx.compose.compiler.plugins.kotlin.analysis.ComposeWritableSlices
import androidx.compose.compiler.plugins.kotlin.analysis.StabilityInferencer
import androidx.compose.compiler.plugins.kotlin.irTrace
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.util.addChild
import org.jetbrains.kotlin.ir.util.copyAnnotationsFrom
import org.jetbrains.kotlin.ir.util.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.name.Name

/**
 * Replaces abstract/open function calls with default parameters with a wrapper that will contain
 * the default parameter preamble and make a virtual call to correct override.
 *
 * Given:
 * ```
 * abstract class Test {
 *     @Composable open fun doSomething(arg1: Int = remember { 0 }) {}
 * }
 *
 * @Composable fun callWithDefaults(instance: Test) {
 *     instance.doSomething()
 *     instance.doSomething(0)
 * }
 * ```
 *
 * Generates:
 * ```
 * abstract class Test {
 *     @Composable open fun doSomething(arg1: Int) {}
 *
 *     class ComposeDefaultsImpl {
 *          /* static */ fun doSomething$composable$default(
 *              instance: Test,
 *              arg1: Int = remember { 0 },
 *          ) {
 *              return instance.doSomething(arg1)
 *          }
 *     }
 * }
 *
 *
 * @Composable fun callWithDefaults(
 *     instance: Test,
 *     $composer: Composer,
 *     $changed: Int
 * ) {
 *     Test$DefaultsImpl.doSomething(instance)
 *     Test$DefaultsImpl.doSomething(instance, 0)
 * }
 *```
 */
class ComposableDefaultParamLowering(
    context: IrPluginContext,
    symbolRemapper: DeepCopySymbolRemapper,
    metrics: ModuleMetrics,
    stabilityInferencer: StabilityInferencer,
    featureFlags: FeatureFlags,
) : AbstractComposeLowering(context, symbolRemapper, metrics, stabilityInferencer, featureFlags) {
    private val originalToTransformed = mutableMapOf<IrSimpleFunction, IrSimpleFunction>()

    override fun lower(module: IrModuleFragment) {
        module.transformChildrenVoid()
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
        if (declaration in originalToTransformed) {
            // Make sure that calls in declaration body are not transformed the second time
            return declaration
        }

        if (!declaration.isVirtualFunctionWithDefaultParam()) {
            return super.visitSimpleFunction(declaration)
        }

        declaration.transformIfNeeded()

        return super.visitSimpleFunction(declaration)
    }

    override fun visitCall(expression: IrCall): IrExpression {
        if (expression.superQualifierSymbol != null) {
            return super.visitCall(expression)
        }

        val callee = expression.symbol.owner
        if (!callee.hasComposableAnnotation()) {
            return super.visitCall(expression)
        }

        val wrapper = callee.findOverriddenFunWithDefaultParam()?.transformIfNeeded()
        if (wrapper == null) {
            return super.visitCall(expression)
        }

        val newCall = irCall(
            wrapper,
            expression.startOffset,
            expression.endOffset
        ).also { newCall ->
            var argCount = expression.valueArgumentsCount
            for (i in 0 until argCount) {
                newCall.putValueArgument(i, expression.getValueArgument(i))
            }
            if (expression.dispatchReceiver != null) {
                newCall.putValueArgument(argCount++, expression.dispatchReceiver)
            }
            if (expression.extensionReceiver != null) {
                newCall.putValueArgument(argCount, expression.extensionReceiver)
            }
        }

        return super.visitCall(newCall)
    }

    private fun IrSimpleFunction.transformIfNeeded(): IrSimpleFunction {
        if (this in originalToTransformed) return originalToTransformed[this]!!

        val wrapper = makeDefaultParameterWrapper(this)
        originalToTransformed[this] = wrapper
        // add to the set of transformed functions to ensure it is not transformed twice
        originalToTransformed[wrapper] = wrapper
        when (val parent = parent) {
            is IrClass -> getOrCreateDefaultImpls(parent).addChild(wrapper)
            else -> error("Cannot add wrapper function to $parent")
        }

        context.irTrace.record(
            ComposeWritableSlices.IS_VIRTUAL_WITH_DEFAULT_PARAM,
            this,
            true
        )

        valueParameters.forEach {
            it.defaultValue = null
        }

        return wrapper
    }

    private fun IrSimpleFunction.findOverriddenFunWithDefaultParam(): IrSimpleFunction? {
        if (this in originalToTransformed) {
            return this
        }

        if (isVirtualFunctionWithDefaultParam()) {
            return this
        }

        overriddenSymbols.forEach {
            val matchingOverride = it.owner.findOverriddenFunWithDefaultParam()
            if (matchingOverride != null) {
                return matchingOverride
            }
        }

        return null
    }

    private fun IrSimpleFunction.isVirtualFunctionWithDefaultParam() =
        hasComposableAnnotation() &&
            !isExpect &&
            (modality == Modality.OPEN || modality == Modality.ABSTRACT) && // virtual function
            overriddenSymbols.isEmpty() && // first in the chain of overrides
            valueParameters.any { it.defaultValue != null } // has a default parameter

    private fun makeDefaultParameterWrapper(
        source: IrSimpleFunction
    ): IrSimpleFunction {
        val wrapper = context.irFactory.createSimpleFunction(
            startOffset = source.startOffset,
            endOffset = source.endOffset,
            origin = IrDeclarationOrigin.DEFINED,
            name = Name.identifier("${source.name.asString()}\$default"),
            visibility = source.visibility,
            isInline = false,
            isExpect = false,
            returnType = source.returnType,
            modality = Modality.FINAL,
            symbol = IrSimpleFunctionSymbolImpl(),
            isTailrec = source.isTailrec,
            isSuspend = false,
            isOperator = false,
            isInfix = false,
        )
        wrapper.copyAnnotationsFrom(source)
        wrapper.copyParametersFrom(source)
        wrapper.valueParameters.forEach {
            it.defaultValue?.transformChildrenVoid()
        }
        // move receiver parameters to value parameters
        val dispatcherReceiver = wrapper.dispatchReceiverParameter
        var index = wrapper.valueParameters.size
        if (dispatcherReceiver != null) {
            dispatcherReceiver.index = index++
            wrapper.valueParameters += dispatcherReceiver
            wrapper.dispatchReceiverParameter = null
        }
        val extensionReceiver = wrapper.extensionReceiverParameter
        if (extensionReceiver != null) {
            extensionReceiver.index = index
            wrapper.valueParameters += extensionReceiver
            wrapper.extensionReceiverParameter = null
        }

        wrapper.body = DeclarationIrBuilder(
            context,
            wrapper.symbol
        ).irBlockBody {
            +irCall(
                source.symbol,
                dispatchReceiver = dispatcherReceiver?.let(::irGet),
                extensionReceiver = extensionReceiver?.let(::irGet),
                args = Array(source.valueParameters.size) {
                    irGet(wrapper.valueParameters[it])
                }
            )
        }

        return wrapper
    }

    private fun getOrCreateDefaultImpls(parent: IrClass): IrClass {
        val cls = parent.declarations.find {
            it is IrClass && it.name == ComposeNames.DEFAULT_IMPLS
        } as? IrClass

        return cls ?: context.irFactory.buildClass {
            startOffset = parent.startOffset
            endOffset = parent.endOffset
            name = ComposeNames.DEFAULT_IMPLS
        }.apply {
            parent.addChild(this)
            createImplicitParameterDeclarationWithWrappedDescriptor()
        }
    }
}
