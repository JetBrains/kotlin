/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlock
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.backend.konan.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.konan.ir.buildSimpleAnnotation
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

internal class PropertyDelegationLowering(val generationState: NativeGenerationState) : FileLoweringPass {
    private val context = generationState.context
    private var tempIndex = 0

    private fun getKPropertyImpl(receiverTypes: List<IrType>,
                                 isLocal: Boolean,
                                 isMutable: Boolean): IrClass {

        val symbols = context.ir.symbols

        val classSymbol =
                if (isLocal) {
                    assert(receiverTypes.isEmpty()) { "Local delegated property cannot have explicit receiver" }
                    when {
                        isMutable -> symbols.kLocalDelegatedMutablePropertyImpl
                        else -> symbols.kLocalDelegatedPropertyImpl
                    }
                } else {
                    when (receiverTypes.size) {
                        0 -> when {
                            isMutable -> symbols.kMutableProperty0Impl
                            else -> symbols.kProperty0Impl
                        }
                        1 -> when {
                            isMutable -> symbols.kMutableProperty1Impl
                            else -> symbols.kProperty1Impl
                        }
                        2 -> when {
                            isMutable -> symbols.kMutableProperty2Impl
                            else -> symbols.kProperty2Impl
                        }
                        else -> throw AssertionError("More than 2 receivers is not allowed")
                    }
                }

        return classSymbol.owner
    }

    override fun lower(irFile: IrFile) {
        // Somehow there is no reasonable common ancestor for IrProperty and IrLocalDelegatedProperty,
        // so index by IrDeclaration.
        val kProperties = mutableMapOf<IrDeclaration, IrField>()
        val generatedClasses = mutableListOf<IrClass>()

        fun kPropertyField(value: IrExpressionBody, id:Int) =
                context.irFactory.createField(
                        SYNTHETIC_OFFSET,
                        SYNTHETIC_OFFSET,
                        DECLARATION_ORIGIN_KPROPERTIES_FOR_DELEGATION,
                        "KPROPERTY${id}".synthesizedName,
                        DescriptorVisibilities.PRIVATE,
                        IrFieldSymbolImpl(),
                        value.expression.type,
                        isFinal = true,
                        isStatic = true,
                ).apply {
                    parent = irFile
                    annotations += buildSimpleAnnotation(context.irBuiltIns, startOffset, endOffset, context.ir.symbols.eagerInitialization.owner)
                    annotations += buildSimpleAnnotation(context.irBuiltIns, startOffset, endOffset, context.ir.symbols.sharedImmutable.owner)
                    initializer = value
                }

        irFile.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {

            override fun visitPropertyReference(expression: IrPropertyReference): IrExpression {
                expression.transformChildrenVoid(this)

                val startOffset = expression.startOffset
                val endOffset = expression.endOffset
                val irBuilder = context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol, startOffset, endOffset)
                irBuilder.run {
                    val receiversCount = listOf(expression.dispatchReceiver, expression.extensionReceiver).count { it != null }
                    return when (receiversCount) {
                        1 -> createKProperty(expression, this, irFile, generatedClasses) // Has receiver.

                        2 -> error("Callable reference to properties with two receivers is not allowed: ${expression.symbol.owner.name}")

                        else -> { // Cache KProperties with no arguments.
                            val field = kProperties.getOrPut(expression.symbol.owner) {
                                kPropertyField(
                                    irExprBody(createKProperty(expression, this, irFile, generatedClasses) as IrConstantValue),
                                    kProperties.size
                                )
                            }

                            irGetField(null, field)
                        }
                    }
                }
            }

            override fun visitLocalDelegatedPropertyReference(expression: IrLocalDelegatedPropertyReference): IrExpression {
                expression.transformChildrenVoid(this)

                val startOffset = expression.startOffset
                val endOffset = expression.endOffset
                val irBuilder = context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol, startOffset, endOffset)
                irBuilder.run {
                    val receiversCount = listOf(expression.dispatchReceiver, expression.extensionReceiver).count { it != null }
                    if (receiversCount == 2)
                        throw AssertionError("Callable reference to properties with two receivers is not allowed: ${expression}")
                    else { // Cache KProperties with no arguments.
                        // TODO: what about `receiversCount == 1` case?
                        val field = kProperties.getOrPut(expression.symbol.owner) {
                            kPropertyField(irExprBody(createLocalKProperty(
                                    expression.symbol.owner.name.asString(),
                                    expression.getter.owner.returnType,
                                    KTypeGenerator(this@PropertyDelegationLowering.context, irFile, expression),
                                    this
                            )), kProperties.size)
                        }

                        return irGetField(null, field)
                    }
                }
            }
        })
        irFile.declarations.addAll(0, kProperties.values)
        irFile.declarations.addAll(generatedClasses)
    }

    private fun createKProperty(
            expression: IrPropertyReference,
            irBuilder: IrBuilderWithScope,
            irFile: IrFile,
            generatedClasses: MutableList<IrClass>
    ): IrExpression {
        val startOffset = expression.startOffset
        val endOffset = expression.endOffset
        return irBuilder.irBlock(expression) {
            val receiverTypes = mutableListOf<IrType>()
            val dispatchReceiver = expression.dispatchReceiver.let {
                if (it == null)
                    null
                else
                    irTemporary(value = it, nameHint = "\$dispatchReceiver${tempIndex++}")
            }
            val extensionReceiver = expression.extensionReceiver.let {
                if (it == null)
                    null
                else
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
                val getterKFunctionType = this@PropertyDelegationLowering.context.ir.symbols.getKFunctionType(
                        returnType,
                        receiverTypes
                )
                IrFunctionReferenceImpl(
                        startOffset = startOffset,
                        endOffset = endOffset,
                        type = getterKFunctionType,
                        symbol = expression.getter!!,
                        typeArgumentsCount = getter.typeParameters.size,
                        valueArgumentsCount = getter.valueParameters.size,
                        reflectionTarget = expression.getter!!
                ).apply {
                    this.dispatchReceiver = dispatchReceiver?.let { irGet(it) }
                    this.extensionReceiver = extensionReceiver?.let { irGet(it) }
                    for (index in 0 until expression.typeArgumentsCount)
                        putTypeArgument(index, expression.getTypeArgument(index))
                }
            }

            val setterCallableReference = expression.setter?.owner?.let { setter ->
                if (!isKMutablePropertyType(expression.type)) null
                else {
                    val setterKFunctionType = this@PropertyDelegationLowering.context.ir.symbols.getKFunctionType(
                            context.irBuiltIns.unitType,
                            receiverTypes + returnType
                    )
                    IrFunctionReferenceImpl(
                            startOffset = startOffset,
                            endOffset = endOffset,
                            type = setterKFunctionType,
                            symbol = expression.setter!!,
                            typeArgumentsCount = setter.typeParameters.size,
                            valueArgumentsCount = setter.valueParameters.size,
                            reflectionTarget = expression.setter!!
                    ).apply {
                        this.dispatchReceiver = dispatchReceiver?.let { irGet(it) }
                        this.extensionReceiver = extensionReceiver?.let { irGet(it) }
                        for (index in 0 until expression.typeArgumentsCount)
                            putTypeArgument(index, expression.getTypeArgument(index))
                    }
                }
            }

            val clazz = getKPropertyImpl(
                    receiverTypes = receiverTypes,
                    isLocal = false,
                    isMutable = setterCallableReference != null)

            val name = irString(expression.symbol.owner.name.asString())

            val initializer = if (dispatchReceiver == null && extensionReceiver == null) {
                fun IrFunctionReference.convert() : IrConstantValue {
                    val builder = FunctionReferenceLowering.FunctionReferenceBuilder(
                            irFile,
                            irFile,
                            this,
                            generationState,
                            irBuilder,
                    )
                    val (newClass, newExpression) = builder.build()
                    generatedClasses.add(newClass)
                    return newExpression as IrConstantValue
                }
                return irConstantObject(clazz, buildMap {
                    put("name", irConstantPrimitive(name))
                    put("getter", getterCallableReference.convert())
                    if (setterCallableReference != null) {
                        put("setter", setterCallableReference.convert())
                    }
                })
            } else irCallWithSubstitutedType(clazz.constructors.single(), receiverTypes + listOf(returnType)).apply {
                putValueArgument(0, name)
                putValueArgument(1, getterCallableReference)
                if (setterCallableReference != null)
                    putValueArgument(2, setterCallableReference)
            }
            +initializer
        }
    }

    private fun createLocalKProperty(propertyName: String,
                                     propertyType: IrType,
                                     kTypeGenerator: KTypeGenerator,
                                     irBuilder: IrBuilderWithScope): IrConstantValue {
        val symbols = context.ir.symbols
        return irBuilder.run {
            irConstantObject(
                    symbols.kLocalDelegatedPropertyImpl.owner,
                    mapOf(
                            "name" to irConstantPrimitive(irString(propertyName)),
                            "returnType" to with(kTypeGenerator) { irKType(propertyType) }
                    )
            )
        }
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

    private companion object {
        private val DECLARATION_ORIGIN_KPROPERTIES_FOR_DELEGATION = IrDeclarationOriginImpl("KPROPERTIES_FOR_DELEGATION")
    }
}
