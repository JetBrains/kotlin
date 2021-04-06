/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.compiler.plugins.kotlin.lower.decoys

import androidx.compose.compiler.plugins.kotlin.lower.AbstractComposeLowering
import androidx.compose.compiler.plugins.kotlin.lower.ModuleLoweringPass
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.remapTypeParameters
import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureSerializer
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrDelegatingConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.util.copyTypeAndValueArgumentsFrom
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.resolve.BindingTrace

/**
 * Replaces all decoys references to their implementations created in [CreateDecoysTransformer].
 */
class SubstituteDecoyCallsTransformer(
    pluginContext: IrPluginContext,
    symbolRemapper: DeepCopySymbolRemapper,
    bindingTrace: BindingTrace,
    override val signatureBuilder: IdSignatureSerializer
) : AbstractComposeLowering(
    context = pluginContext,
    symbolRemapper = symbolRemapper,
    bindingTrace = bindingTrace
),
    ModuleLoweringPass,
    DecoyTransformBase {
    override fun lower(module: IrModuleFragment) {
        module.transformChildrenVoid()

        module.patchDeclarationParents()
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
        // Apart from function / constructor calls, decoys can surface in overridden symbols,
        // so we need to replace them as well.
        // They are replaced only for decoy implementations however, as decoys should match
        // original descriptors.

        if (declaration.isDecoy()) {
            return super.visitSimpleFunction(declaration)
        }

        val newOverriddenSymbols = declaration.overriddenSymbols.map {
            if (it.owner.isDecoy()) {
                it.owner.getComposableForDecoy() as IrSimpleFunctionSymbol
            } else {
                it
            }
        }

        declaration.overriddenSymbols = newOverriddenSymbols
        return super.visitSimpleFunction(declaration)
    }

    override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
        val callee = expression.symbol.owner
        if (!callee.isDecoy()) {
            return super.visitConstructorCall(expression)
        }

        val actualConstructor = callee.getComposableForDecoy().owner as IrConstructor

        val updatedCall = IrConstructorCallImpl(
            symbol = actualConstructor.symbol,
            origin = expression.origin,
            startOffset = expression.startOffset,
            endOffset = expression.endOffset,
            type = expression.type.remapTypeParameters(callee, actualConstructor),
            typeArgumentsCount = expression.typeArgumentsCount,
            valueArgumentsCount = expression.valueArgumentsCount,
            constructorTypeArgumentsCount = expression.constructorTypeArgumentsCount
        ).let {
            it.copyTypeAndValueArgumentsFrom(expression)
            return@let it.copyWithNewTypeParams(callee, actualConstructor)
        }

        return super.visitConstructorCall(updatedCall)
    }

    override fun visitDelegatingConstructorCall(
        expression: IrDelegatingConstructorCall
    ): IrExpression {
        val callee = expression.symbol.owner
        if (!callee.isDecoy()) {
            return super.visitDelegatingConstructorCall(expression)
        }

        val actualConstructor = callee.getComposableForDecoy().owner as IrConstructor

        val updatedCall = IrDelegatingConstructorCallImpl(
            symbol = actualConstructor.symbol,
            startOffset = expression.startOffset,
            endOffset = expression.endOffset,
            type = expression.type.remapTypeParameters(callee, actualConstructor),
            typeArgumentsCount = expression.typeArgumentsCount,
            valueArgumentsCount = expression.valueArgumentsCount,
        ).let {
            it.copyTypeAndValueArgumentsFrom(expression)
            return@let it.copyWithNewTypeParams(callee, actualConstructor)
        }

        return super.visitDelegatingConstructorCall(updatedCall)
    }

    override fun visitCall(expression: IrCall): IrExpression {
        val callee = expression.symbol.owner
        if (!callee.isDecoy()) {
            return super.visitCall(expression)
        }

        val actualFunction = callee.getComposableForDecoy().owner as IrSimpleFunction

        val updatedCall = IrCallImpl(
            symbol = actualFunction.symbol,
            origin = expression.origin,
            startOffset = expression.startOffset,
            endOffset = expression.endOffset,
            type = expression.type.remapTypeParameters(callee, actualFunction),
            typeArgumentsCount = expression.typeArgumentsCount,
            valueArgumentsCount = expression.valueArgumentsCount,
            superQualifierSymbol = expression.superQualifierSymbol
        ).let {
            it.copyTypeAndValueArgumentsFrom(expression)
            return@let it.copyWithNewTypeParams(callee, actualFunction)
        }
        return super.visitCall(updatedCall)
    }

    override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
        val callee = expression.symbol.owner
        if (!callee.isDecoy()) {
            return super.visitFunctionReference(expression)
        }

        val actualFunction = callee.getComposableForDecoy().owner as IrSimpleFunction

        val updatedReference = IrFunctionReferenceImpl(
            symbol = actualFunction.symbol,
            origin = expression.origin,
            startOffset = expression.startOffset,
            endOffset = expression.endOffset,
            type = expression.type.remapTypeParameters(callee, actualFunction),
            typeArgumentsCount = expression.typeArgumentsCount,
            valueArgumentsCount = expression.valueArgumentsCount,
            reflectionTarget = expression.reflectionTarget
        ).let {
            it.copyTypeAndValueArgumentsFrom(expression)
            return@let it.copyWithNewTypeParams(callee, actualFunction)
        }
        return super.visitFunctionReference(updatedReference)
    }
}