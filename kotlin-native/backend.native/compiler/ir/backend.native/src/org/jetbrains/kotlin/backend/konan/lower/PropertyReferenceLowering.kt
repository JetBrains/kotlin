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
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

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
    private val immutableSymbols = symbols.immutablePropertiesConstructors
    private val mutableSymbols = symbols.mutablePropertiesConstructors

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {

            override fun visitPropertyReference(expression: IrPropertyReference): IrExpression {
                expression.transformChildrenVoid(this)

                return context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol).at(expression).run {
                    createKProperty(expression, this)
                }
            }

            override fun visitLocalDelegatedPropertyReference(expression: IrLocalDelegatedPropertyReference): IrExpression {
                expression.transformChildrenVoid(this)

                val startOffset = expression.startOffset
                val endOffset = expression.endOffset
                val irBuilder = context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol, startOffset, endOffset).toNativeConstantReflectionBuilder(symbols)
                val receiversCount = listOf(expression.dispatchReceiver, expression.extensionReceiver).count { it != null }
                if (receiversCount == 2)
                    throw AssertionError("Callable reference to properties with two receivers is not allowed: ${expression}")
                else { // Cache KProperties with no arguments.
                    return irBuilder.createLocalKProperty(
                            expression.symbol.owner.name.asString(),
                            expression.getter.owner.returnType,
                    )
                }
            }
        })
    }

    private fun createKProperty(
            expression: IrPropertyReference,
            irBuilder: IrBuilderWithScope
    ): IrExpression {
        fun createAccessorReference(accessor: IrSimpleFunction): Pair<IrFunctionReference, List<IrType>> {
            val parameterTypes = accessor.parameters
                    .filterIndexed { index, _ -> index >= expression.arguments.size || expression.arguments[index] == null }
                    .map { it.type }
            val accessorReference = IrFunctionReferenceImpl(
                    startOffset = expression.startOffset,
                    endOffset = expression.endOffset,
                    type = symbols.getKFunctionType(accessor.returnType, parameterTypes),
                    symbol = accessor.symbol,
                    typeArgumentsCount = accessor.typeParameters.size,
                    reflectionTarget = accessor.symbol
            ).apply {
                for (index in 0..<expression.typeArgumentsCount)
                    putTypeArgument(index, expression.getTypeArgument(index))
            }
            return Pair(accessorReference, parameterTypes)
        }

        val property = expression.symbol.owner
        val getter = expression.getter!!.owner
        val (getterCallableReference, receiverTypes) = createAccessorReference(getter)
        val setterCallableReference = expression.setter?.owner
                ?.takeIf { isKMutablePropertyType(expression.type) }
                ?.let { createAccessorReference(it).first }

        val typeArguments = receiverTypes + listOf(getter.returnType)
        return if (setterCallableReference == null) {
            expression.arguments.forEachIndexed { index, argument ->
                getterCallableReference.arguments[index] = argument
            }
            irBuilder.at(expression).irCallWithSubstitutedType(immutableSymbols.byRecieversCount[receiverTypes.size], typeArguments).apply {
                arguments[0] = irBuilder.irString(property.name.asString())
                arguments[1] = getterCallableReference
            }
        } else irBuilder.irBlock(expression) {
            expression.arguments.forEachIndexed { index, argument ->
                if (argument == null) return@forEachIndexed
                val temp = irTemporary(value = argument)
                getterCallableReference.arguments[index] = irGet(temp)
                setterCallableReference.arguments[index] = irGet(temp)
            }
            +irCallWithSubstitutedType(mutableSymbols.byRecieversCount[receiverTypes.size], typeArguments).apply {
                arguments[0] = irBuilder.irString(property.name.asString())
                arguments[1] = getterCallableReference
                arguments[2] = setterCallableReference
            }
        }
    }

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

    private fun isKMutablePropertyType(type: IrType): Boolean {
        if (type !is IrSimpleType) return false
        val expectedClass = when (type.arguments.size) {
            0 -> return false
            1 -> symbols.kMutableProperty0
            2 -> symbols.kMutableProperty1
            3 -> symbols.kMutableProperty2
            else -> throw AssertionError("More than 2 receivers is not allowed")
        }
        return type.classifier == expectedClass
    }
}
