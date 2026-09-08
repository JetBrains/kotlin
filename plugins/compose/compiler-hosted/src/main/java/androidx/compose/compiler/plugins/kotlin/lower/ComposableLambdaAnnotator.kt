/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.compose.compiler.plugins.kotlin.FeatureFlags
import androidx.compose.compiler.plugins.kotlin.ModuleMetrics
import androidx.compose.compiler.plugins.kotlin.analysis.StabilityInferencer
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrLocalDelegatedProperty
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrAnnotationImpl
import org.jetbrains.kotlin.ir.expressions.impl.fromSymbolOwner
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

/**
 * In K1, the frontend used to annotate inferred composable lambdas with `@Composable`.
 * The K2 frontend instead uses a different type for composable lambdas. This pass adds
 * the annotation, since the backend expects it.
 */
class ComposableLambdaAnnotator(
    context: IrPluginContext,
    irModule: IrModuleFragment,
    metrics: ModuleMetrics,
    stabilityInferencer: StabilityInferencer,
    featureFlags: FeatureFlags,
) : AbstractComposeLowering(context, irModule, metrics, stabilityInferencer, featureFlags),
    ModuleLoweringPass {

    override fun lower(irModule: IrModuleFragment) {
        irModule.transformChildrenVoid(this)
    }

    override fun visitFunctionExpression(expression: IrFunctionExpression): IrExpression {
        if (expression.type.isSyntheticComposableFunction()) {
            expression.function.mark()
        }
        return super.visitFunctionExpression(expression)
    }

    override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
        if (expression.type.isSyntheticComposableFunction() && expression.symbol.owner.visibility == DescriptorVisibilities.LOCAL) {
            expression.symbol.owner.mark()
        }
        return super.visitFunctionReference(expression)
    }

    override fun visitRichFunctionReference(expression: IrRichFunctionReference): IrExpression {
        if (expression.type.isSyntheticComposableFunction() || expression.type.isKComposableFunction()) {
            expression.invokeFunction.mark()
        }
        return super.visitRichFunctionReference(expression)
    }

    override fun visitRichPropertyReference(expression: IrRichPropertyReference): IrExpression {
        val getter = when (val p = expression.reflectionTargetSymbol?.owner) {
            is IrProperty -> p.getter
            is IrLocalDelegatedProperty -> p.getter
            else -> null
        }
        if (getter?.hasComposableAnnotation() == true || getter?.isComposableDelegatedAccessor() == true) {
            expression.getterFunction.mark()
        }
        val setter = when (val p = expression.reflectionTargetSymbol?.owner) {
            is IrProperty -> p.setter
            is IrLocalDelegatedProperty -> p.setter
            else -> null
        }
        if (setter?.hasComposableAnnotation() == true || setter?.isComposableDelegatedAccessor() == true) {
            expression.setterFunction?.mark()
        }
        return super.visitRichPropertyReference(expression)
    }

    override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty): IrStatement {
        if (declaration.getter.isComposableDelegatedAccessor()) {
            declaration.getter.mark()
        }
        if (declaration.setter?.isComposableDelegatedAccessor() == true) {
            declaration.setter!!.mark()
        }
        return super.visitLocalDelegatedProperty(declaration)
    }

    private fun IrFunction.mark() {
        if (!hasComposableAnnotation()) {
            annotations = annotations + IrAnnotationImpl.fromSymbolOwner(
                composableIrClass.defaultType,
                composableIrClass.constructors.first().symbol,
            )
        }
    }
}
