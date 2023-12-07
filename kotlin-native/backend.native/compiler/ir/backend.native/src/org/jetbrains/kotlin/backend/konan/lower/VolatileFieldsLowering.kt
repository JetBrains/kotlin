/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.ir.addDispatchReceiver
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.NativeMapping
import org.jetbrains.kotlin.backend.konan.ir.buildSimpleAnnotation
import org.jetbrains.kotlin.backend.konan.llvm.IntrinsicType
import org.jetbrains.kotlin.backend.konan.llvm.tryGetIntrinsicType
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrReturnableBlockSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.irCall
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.capitalizeDecapitalize.*
import org.jetbrains.kotlin.ir.util.*

val IR_DECLARATION_ORIGIN_VOLATILE = IrDeclarationOriginImpl("VOLATILE")

internal class VolatileFieldsLowering(val context: Context) : FileLoweringPass {
    private val symbols = context.ir.symbols
    private val irBuiltins = context.irBuiltIns
    private fun IrBuilderWithScope.irByteToBool(expression: IrExpression) = irCall(symbols.areEqualByValue[PrimitiveBinaryType.BYTE]!!).apply {
        putValueArgument(0, expression)
        putValueArgument(1, irByte(1))
    }
    private fun IrBuilderWithScope.irBoolToByte(expression: IrExpression) = irWhen(irBuiltins.byteType, listOf(
        irBranch(expression, irByte(1)),
        irElseBranch(irByte(0))
    ))
    private val convertedBooleanFields = mutableSetOf<IrFieldSymbol>()
    private fun IrField.requiresBooleanConversion() = (type == irBuiltins.booleanType && hasAnnotation(KonanFqNames.volatile)) || symbol in convertedBooleanFields

    private fun buildIntrinsicFunction(irField: IrField, intrinsicType: IntrinsicType, builder: IrSimpleFunction.() -> Unit) = context.irFactory.buildFun {
        isExternal = true
        origin = IR_DECLARATION_ORIGIN_VOLATILE
        name = Name.special("<${intrinsicType.name.decapitalizeSmart()}-${irField.name}>")
        startOffset = irField.startOffset
        endOffset = irField.endOffset
    }.apply {
        val property = irField.correspondingPropertySymbol?.owner
        val scope = property?.parent
        require(scope != null)
        require(scope is IrClass || scope is IrFile)
        parent = scope
        if (scope is IrClass) {
            addDispatchReceiver {
                startOffset = irField.startOffset
                endOffset = irField.endOffset
                type = scope.defaultType
            }
        }
        builder()
        annotations += buildSimpleAnnotation(context.irBuiltIns,
                SYNTHETIC_OFFSET, SYNTHETIC_OFFSET,
                context.ir.symbols.typedIntrinsic.owner, intrinsicType.name)
    }

    private fun buildCasFunction(irField: IrField, intrinsicType: IntrinsicType, functionReturnType: IrType) =
            buildIntrinsicFunction(irField, intrinsicType ) {
                returnType = functionReturnType
                addValueParameter {
                    startOffset = irField.startOffset
                    endOffset = irField.endOffset
                    name = Name.identifier("expectedValue")
                    type = irField.type
                }
                addValueParameter {
                    startOffset = irField.startOffset
                    endOffset = irField.endOffset
                    name = Name.identifier("newValue")
                    type = irField.type
                }
            }

    private fun buildAtomicRWMFunction(irField: IrField, intrinsicType: IntrinsicType) =
            buildIntrinsicFunction(irField, intrinsicType) {
                returnType = irField.type
                addValueParameter {
                    startOffset = irField.startOffset
                    endOffset = irField.endOffset
                    name = Name.identifier("value")
                    type = irField.type
                }
            }


    private inline fun atomicFunction(irField: IrField, type: NativeMapping.AtomicFunctionType, builder: () -> IrSimpleFunction): IrSimpleFunction {
        val key = NativeMapping.AtomicFunctionKey(irField, type)
        return context.mapping.volatileFieldToAtomicFunction.getOrPut(key) {
            builder().also {
                context.mapping.functionToVolatileField[it] = irField
            }
        }
    }

    private fun compareAndSetFunction(irField: IrField) = atomicFunction(irField, NativeMapping.AtomicFunctionType.COMPARE_AND_SET) {
        this.buildCasFunction(irField, IntrinsicType.COMPARE_AND_SET, this.context.irBuiltIns.booleanType)
    }
    private fun compareAndExchangeFunction(irField: IrField) = atomicFunction(irField, NativeMapping.AtomicFunctionType.COMPARE_AND_EXCHANGE) {
        this.buildCasFunction(irField, IntrinsicType.COMPARE_AND_EXCHANGE, irField.type)
    }
    private fun getAndSetFunction(irField: IrField) = atomicFunction(irField, NativeMapping.AtomicFunctionType.GET_AND_SET) {
        this.buildAtomicRWMFunction(irField, IntrinsicType.GET_AND_SET)
    }
    private fun getAndAddFunction(irField: IrField) = atomicFunction(irField, NativeMapping.AtomicFunctionType.GET_AND_ADD) {
        this.buildAtomicRWMFunction(irField, IntrinsicType.GET_AND_ADD)
    }

    private fun IrField.isInteger() = type == context.irBuiltIns.intType ||
            type == context.irBuiltIns.longType ||
            type == context.irBuiltIns.shortType ||
            type == context.irBuiltIns.byteType

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(object : IrBuildingTransformer(context) {
            override fun visitClass(declaration: IrClass): IrStatement {
                declaration.transformChildrenVoid()
                processDeclarationList(declaration.declarations)
                return declaration
            }

            override fun visitFile(declaration: IrFile): IrFile {
                declaration.transformChildrenVoid()
                processDeclarationList(declaration.declarations)
                return declaration
            }

            private fun processDeclarationList(declarations: MutableList<IrDeclaration>) {
                declarations.transformFlat {
                    when {
                        it !is IrProperty -> null
                        it.backingField?.hasAnnotation(KonanFqNames.volatile) != true -> null
                        else -> {
                            val field = it.backingField!!
                            if (field.type.binaryTypeIsReference() && context.memoryModel != MemoryModel.EXPERIMENTAL) {
                                it.annotations = it.annotations.filterNot { it.symbol.owner.parentAsClass.hasEqualFqName(KonanFqNames.volatile) }
                                null
                            } else {
                                listOfNotNull(it,
                                        compareAndSetFunction(field),
                                        compareAndExchangeFunction(field),
                                        getAndSetFunction(field),
                                        if (field.isInteger()) getAndAddFunction(field) else null
                                )
                            }
                        }
                    }
                }
            }

            override fun visitField(declaration: IrField): IrStatement {
                if (declaration.type == irBuiltins.booleanType && declaration.hasAnnotation(KonanFqNames.volatile)) {
                    convertedBooleanFields.add(declaration.symbol)
                    declaration.type = irBuiltins.byteType
                    declaration.initializer?.let {
                        it.expression = context.createIrBuilder(declaration.symbol).at(it.expression).irBoolToByte(it.expression)
                    }
                }
                return super.visitField(declaration)
            }


            private fun unsupported(message: String) = builder.irCall(context.ir.symbols.throwIllegalArgumentExceptionWithMessage).apply {
                putValueArgument(0, builder.irString(message))
            }

            override fun visitGetField(expression: IrGetField): IrExpression {
                super.visitGetField(expression)
                return if (expression.symbol.owner.requiresBooleanConversion()) {
                    builder.at(expression).irByteToBool(expression.apply { type = irBuiltins.byteType })
                } else {
                    expression
                }
            }

            override fun visitSetField(expression: IrSetField): IrExpression {
                super.visitSetField(expression)
                return if (expression.symbol.owner.requiresBooleanConversion()) {
                    expression.apply { value = builder.at(value).irBoolToByte(value) }
                } else {
                    expression
                }
            }

            private val intrinsicMap = mapOf(
                    IntrinsicType.COMPARE_AND_SET_FIELD to ::compareAndSetFunction,
                    IntrinsicType.COMPARE_AND_EXCHANGE_FIELD to ::compareAndExchangeFunction,
                    IntrinsicType.GET_AND_SET_FIELD to ::getAndSetFunction,
                    IntrinsicType.GET_AND_ADD_FIELD to ::getAndAddFunction,
            )

            private val IrBlock.singleExpressionOrNull get() = statements.singleOrNull() as? IrExpression

            private tailrec fun getConstPropertyReference(expression: IrExpression?, expectedReturn: IrReturnableBlockSymbol?) : IrPropertyReference? {
                return when {
                    expression == null -> null
                    expectedReturn == null && expression is IrPropertyReference -> expression
                    expectedReturn == null && expression is IrReturnableBlock -> getConstPropertyReference(expression.singleExpressionOrNull, expression.symbol)
                    expression is IrReturn && expression.returnTargetSymbol == expectedReturn -> getConstPropertyReference(expression.value, null)
                    expression is IrBlock -> getConstPropertyReference(expression.singleExpressionOrNull, expectedReturn)
                    else -> null
                }
            }

            override fun visitCall(expression: IrCall): IrExpression {
                expression.transformChildrenVoid(this)
                val intrinsicType = tryGetIntrinsicType(expression).takeIf {
                    it in intrinsicMap || it == IntrinsicType.ATOMIC_GET_FIELD || it == IntrinsicType.ATOMIC_SET_FIELD
                } ?: return expression
                builder.at(expression)
                val reference = getConstPropertyReference(expression.extensionReceiver, null)
                        ?: return unsupported("Only compile-time known IrProperties supported for $intrinsicType")
                val property = reference.symbol.owner
                val backingField = property.backingField
                if (backingField?.type?.binaryTypeIsReference() == true && context.memoryModel != MemoryModel.EXPERIMENTAL) {
                    return unsupported("Only primitives are supported for $intrinsicType with legacy memory model")
                }
                if (backingField?.hasAnnotation(KonanFqNames.volatile) != true) {
                    return unsupported("Only volatile properties are supported for $intrinsicType")
                }
                val function = when(intrinsicType) {
                    IntrinsicType.ATOMIC_GET_FIELD -> property.getter ?: error("Getter is not defined for the property: ${property.render()}")
                    IntrinsicType.ATOMIC_SET_FIELD -> property.setter ?: error("Setter is not defined for the property: ${property.render()}")
                    else -> intrinsicMap[intrinsicType]!!(backingField)
                }
                return builder.irCall(function).apply {
                    dispatchReceiver = reference.dispatchReceiver
                    for (index in 0 until expression.valueArgumentsCount) {
                        putValueArgument(index, expression.getValueArgument(index))
                    }
                }.let {
                    if (intrinsicType == IntrinsicType.ATOMIC_GET_FIELD || intrinsicType == IntrinsicType.ATOMIC_SET_FIELD) {
                        return it
                    }
                    if (backingField.requiresBooleanConversion()) {
                        for (arg in 0 until it.valueArgumentsCount) {
                            it.putValueArgument(arg, builder.irBoolToByte(it.getValueArgument(arg)!!))
                        }
                        if (intrinsicType == IntrinsicType.COMPARE_AND_EXCHANGE_FIELD || intrinsicType == IntrinsicType.GET_AND_SET_FIELD) {
                            builder.irByteToBool(it)
                        } else {
                            it
                        }
                    } else {
                        it
                    }
                }
            }
        })
    }
}