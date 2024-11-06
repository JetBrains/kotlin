/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.backend.konan.ir.KonanSymbols
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrLocalDelegatedPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

internal class PropertyReferencesConstructorsSet(
    val local: IrConstructorSymbol,
    val byRecieversCount: List<IrConstructorSymbol>
) {
    constructor(local: IrClassSymbol, byRecieversCount: List<IrClassSymbol>) : this(
            local.constructors.single(),
            byRecieversCount.map { it.constructors.single() }
    )
}

internal val KonanSymbols.immutablePropertiesConstructors
    get() = PropertyReferencesConstructorsSet(
        kLocalDelegatedPropertyImpl,
        listOf(kProperty0Impl, kProperty1Impl, kProperty2Impl)
    )

internal val KonanSymbols.mutablePropertiesConstructors
    get() = PropertyReferencesConstructorsSet(
            kLocalDelegatedMutablePropertyImpl,
            listOf(kMutableProperty0Impl, kMutableProperty1Impl, kMutableProperty2Impl)
    )

internal class PropertyReferenceLowering(val generationState: NativeGenerationState) : FileLoweringPass {
    private val context = generationState.context
    private val symbols = context.ir.symbols
    private val irBuiltIns = context.irBuiltIns
    private val immutableSymbols = symbols.immutablePropertiesConstructors
    private val mutableSymbols = symbols.mutablePropertiesConstructors

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {
            override fun visitRichPropertyReference(expression: IrRichPropertyReference): IrExpression {
                expression.transformChildrenVoid(this)
                val irBuilder = context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol).at(expression).toNativeConstantReflectionBuilder(context.ir.symbols)
                val originalPropertySymbol = expression.reflectionTargetSymbol
                if (originalPropertySymbol is IrLocalDelegatedPropertySymbol) {
                    return irBuilder.createLocalKProperty(originalPropertySymbol.owner.name.asString(), expression.type)
                }
                require(originalPropertySymbol is IrPropertySymbol)
                val typeArguments = (expression.type as IrSimpleType).arguments.map { it.typeOrNull ?: irBuiltIns.anyNType  }
                val block =  irBuilder.irBlock {
                    val constructor = if (expression.setterFunction != null) {
                        mutableSymbols
                    } else {
                        immutableSymbols
                    }.byRecieversCount[typeArguments.size - 1]

                    +irCall(constructor, expression.type, typeArguments).apply {
                        arguments[0] = irString(originalPropertySymbol.owner.name.asString())
                        val getterReference = irRichFunctionReference(
                                function = expression.getterFunction,
                                superType = symbols.kFunctionN(typeArguments.size - 1).typeWith(typeArguments),
                                reflectionTarget = originalPropertySymbol.owner.getter!!.symbol,
                                captures = expression.boundValues,
                                origin = expression.origin
                        )
                        arguments[1] = getterReference
                        expression.setterFunction?.let { setterFunction ->
                            // we need to avoid calculation of bound values twice, so store them to temp variables
                            val tempVars = getterReference.boundValues.map {
                                if (it is IrGetValue) it.symbol.owner else irTemporary(it)
                            }
                            getterReference.boundValues.clear()
                            getterReference.boundValues += tempVars.map { irGet(it) }
                            arguments[2] = irRichFunctionReference(
                                    function = setterFunction,
                                    superType = symbols.kFunctionN(typeArguments.size).typeWith(typeArguments + irBuiltIns.unitType),
                                    reflectionTarget = originalPropertySymbol.owner.setter!!.symbol,
                                    captures = tempVars.map { irGet(it) },
                                    origin = expression.origin
                            )
                        }
                    }
                }
                if (expression.boundValues.isEmpty()) {
                    return block.statements.single() as IrExpression
                }
                return block
            }

            override fun visitPropertyReference(expression: IrPropertyReference): IrExpression {
                shouldNotBeCalled("Property references should've been lowered at this point")
            }

            override fun visitLocalDelegatedPropertyReference(expression: IrLocalDelegatedPropertyReference): IrExpression {
                shouldNotBeCalled("Property references should've been lowered at this point")
            }
        })
    }

    private fun IrBuilderWithScope.irRichFunctionReference(
            function: IrSimpleFunction,
            superType: IrSimpleType,
            reflectionTarget: IrSimpleFunctionSymbol,
            captures: List<IrExpression>,
            origin: IrStatementOrigin?,
    ): IrRichFunctionReferenceImpl = irRichFunctionReference(
            invokeFunction = function,
            superType = superType,
            reflectionTargetSymbol = reflectionTarget,
            overriddenFunctionSymbol = UpgradeCallableReferences.selectSAMOverriddenFunction(superType),
            captures = captures,
            origin = origin
    )

    private fun NativeConstantReflectionIrBuilder.createLocalKProperty(propertyName: String,
                                                                       propertyType: IrType): IrConstantValue {
        return irConstantObject(
                symbols.kLocalDelegatedPropertyImpl.owner,
                mapOf(
                        "name" to irConstantPrimitive(irString(propertyName)),
                        "returnType" to irKType(propertyType)
                )
        )
    }
}
