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
    private val immutableSymbols = context.ir.symbols.immutablePropertiesConstructors
    private val mutableSymbols = context.ir.symbols.mutablePropertiesConstructors
    private var tempIndex = 0
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
                val irBuilder = context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol, startOffset, endOffset).toNativeConstantReflectionBuilder(context.ir.symbols)
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
        val startOffset = expression.startOffset
        val endOffset = expression.endOffset
        return irBuilder.irBlock(expression) {
            val receiverTypes = mutableListOf<IrType>()
            val dispatchReceiver = expression.dispatchReceiver?.let {
                irTemporary(value = it, nameHint = "\$dispatchReceiver${tempIndex++}")
            }
            val extensionReceiver = expression.extensionReceiver?.let {
                irTemporary(value = it, nameHint = "\$extensionReceiver${tempIndex++}")
            }
            val returnType = expression.getter?.owner?.returnType ?: expression.field!!.owner.type

            val getterCallableReference = expression.getter!!.owner.let { getter ->
                getter.extensionReceiverParameter.let {
                    if (it != null && expression.extensionReceiver == null)
                        receiverTypes.add(it.type)
                }
                getter.dispatchReceiverParameter.let {
                    if (it != null && expression.dispatchReceiver == null)
                        receiverTypes.add(it.type)
                }
                val getterKFunctionType = this@PropertyReferenceLowering.context.ir.symbols.getKFunctionType(
                        returnType,
                        receiverTypes
                )
                IrFunctionReferenceImpl(
                        startOffset = startOffset,
                        endOffset = endOffset,
                        type = getterKFunctionType,
                        symbol = expression.getter!!,
                        typeArgumentsCount = getter.typeParameters.size,
                        reflectionTarget = expression.getter!!
                ).apply {
                    this.dispatchReceiver = dispatchReceiver?.let { irGet(it) }
                    this.extensionReceiver = extensionReceiver?.let { irGet(it) }
                    for (index in expression.typeArguments.indices) {
                        typeArguments[index] = expression.typeArguments[index]
                    }
                }
            }

            val setterCallableReference = expression.setter?.owner?.takeIf { isKMutablePropertyType(expression.type) }?.let { setter ->
                val setterKFunctionType = this@PropertyReferenceLowering.context.ir.symbols.getKFunctionType(
                        context.irBuiltIns.unitType,
                        receiverTypes + returnType
                )
                IrFunctionReferenceImpl(
                        startOffset = startOffset,
                        endOffset = endOffset,
                        type = setterKFunctionType,
                        symbol = expression.setter!!,
                        typeArgumentsCount = setter.typeParameters.size,
                        reflectionTarget = expression.setter!!
                ).apply {
                    this.dispatchReceiver = dispatchReceiver?.let { irGet(it) }
                    this.extensionReceiver = extensionReceiver?.let { irGet(it) }
                    for (index in expression.typeArguments.indices) {
                        typeArguments[index] = expression.typeArguments[index]
                    }
                }
            }

            val constructor = if (setterCallableReference != null) {
                mutableSymbols
            } else {
                immutableSymbols
            }.byRecieversCount[receiverTypes.size]

            +irCallWithSubstitutedType(constructor, receiverTypes + listOf(returnType)).apply {
                putValueArgument(0, irString(expression.symbol.owner.name.asString()))
                putValueArgument(1, getterCallableReference)
                if (setterCallableReference != null) {
                    putValueArgument(2, setterCallableReference)
                }
            }
        }
    }

    private fun NativeConstantReflectionIrBuilder.createLocalKProperty(propertyName: String,
                                                                       propertyType: IrType): IrConstantValue {
        val symbols = this@PropertyReferenceLowering.context.ir.symbols
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
            1 -> context.ir.symbols.kMutableProperty0
            2 -> context.ir.symbols.kMutableProperty1
            3 -> context.ir.symbols.kMutableProperty2
            else -> throw AssertionError("More than 2 receivers is not allowed")
        }
        return type.classifier == expectedClass
    }
}
