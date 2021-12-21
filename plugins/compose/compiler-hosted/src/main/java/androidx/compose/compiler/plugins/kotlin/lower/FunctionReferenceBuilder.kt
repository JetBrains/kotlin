/*
 * Copyright 2020 The Android Open Source Project
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

import org.jetbrains.kotlin.backend.common.ir.addFakeOverrides
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.ir.moveBodyTo
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.builders.IrGeneratorContext
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irDelegatingConstructorCall
import org.jetbrains.kotlin.ir.builders.setSourceRange
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.copyAttributes
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeSystemContext
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.explicitParameters
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.name.SpecialNames

@Suppress("SuspiciousCollectionReassignment")
class FunctionReferenceBuilder(
    private val irFunctionExpression: IrFunctionExpression,
    functionSuperClass: IrClassSymbol,
    private val superType: IrType,
    private val currentDeclarationParent: IrDeclarationParent,
    private val generatorContext: IrGeneratorContext,
    private val currentScopeOwnerSymbol: IrSymbol,
    private val irTypeSystemContext: IrTypeSystemContext
) {
    private val callee = irFunctionExpression.function
    private val superMethod =
        functionSuperClass.functions.single { it.owner.modality == Modality.ABSTRACT }

    private val functionReferenceClass = generatorContext.irFactory.buildClass {
        setSourceRange(irFunctionExpression)
        visibility = DescriptorVisibilities.LOCAL
        origin = JvmLoweredDeclarationOrigin.LAMBDA_IMPL
        name = SpecialNames.NO_NAME_PROVIDED
    }.apply {
        parent = currentDeclarationParent
        superTypes = listOfNotNull(superType)
        createImplicitParameterDeclarationWithWrappedDescriptor()
        copyAttributes(irFunctionExpression)
        metadata = irFunctionExpression.function.metadata
    }

    fun build(): IrExpression = DeclarationIrBuilder(
        generatorContext,
        currentScopeOwnerSymbol
    ).run {
        irBlock(irFunctionExpression.startOffset, irFunctionExpression.endOffset) {
            val constructor = createConstructor()
            createInvokeMethod()
            functionReferenceClass.addFakeOverrides(irTypeSystemContext)
            +functionReferenceClass
            +irCall(constructor.symbol)
        }
    }

    private fun createConstructor(): IrConstructor =
        functionReferenceClass.addConstructor {
            origin = JvmLoweredDeclarationOrigin.GENERATED_MEMBER_IN_CALLABLE_REFERENCE
            returnType = functionReferenceClass.defaultType
            isPrimary = true
        }.apply {
            val constructor = irTypeSystemContext.irBuiltIns.anyClass.owner.constructors.single()
            body = DeclarationIrBuilder(generatorContext, symbol).run {
                irBlockBody(startOffset, endOffset) {
                    +irDelegatingConstructorCall(constructor)
                    +IrInstanceInitializerCallImpl(
                        startOffset,
                        endOffset,
                        functionReferenceClass.symbol,
                        context.irBuiltIns.unitType
                    )
                }
            }
        }

    private fun createInvokeMethod(): IrSimpleFunction =
        functionReferenceClass.addFunction {
            setSourceRange(callee)
            name = superMethod.owner.name
            returnType = callee.returnType
            isSuspend = callee.isSuspend
        }.apply {
            overriddenSymbols += superMethod
            dispatchReceiverParameter = parentAsClass.thisReceiver!!.copyTo(this)
            createLambdaInvokeMethod()
        }

    // Inline the body of an anonymous function into the generated lambda subclass.
    private fun IrSimpleFunction.createLambdaInvokeMethod() {
        annotations += callee.annotations
        val valueParameterMap = callee.explicitParameters.withIndex().associate { (index, param) ->
            param to param.copyTo(this, index = index)
        }
        valueParameters += valueParameterMap.values
        body = callee.moveBodyTo(this, valueParameterMap)
    }
}
