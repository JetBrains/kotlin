/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.ir.buildSimpleAnnotation
import org.jetbrains.kotlin.backend.konan.llvm.IntrinsicType
import org.jetbrains.kotlin.backend.konan.llvm.tryGetIntrinsicType
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.declarations.buildReceiverParameter
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrReturnableBlockSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.capitalizeDecapitalize.*
import org.jetbrains.kotlin.utils.addToStdlib.getOrSetIfNull
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

val IR_DECLARATION_ORIGIN_VOLATILE = IrDeclarationOriginImpl("VOLATILE")

enum class AtomicFunctionType {
    COMPARE_AND_EXCHANGE, COMPARE_AND_SET, GET_AND_SET, GET_AND_ADD,
    ATOMIC_GET_ARRAY_ELEMENT, ATOMIC_SET_ARRAY_ELEMENT, COMPARE_AND_EXCHANGE_ARRAY_ELEMENT, COMPARE_AND_SET_ARRAY_ELEMENT, GET_AND_SET_ARRAY_ELEMENT, GET_AND_ADD_ARRAY_ELEMENT;
}

private var IrField.atomicFunction: MutableMap<AtomicFunctionType, IrSimpleFunction>? by irAttribute(copyByDefault = false)
internal var IrSimpleFunction.volatileField: IrField? by irAttribute(copyByDefault = false)

internal class VolatileFieldsLowering(val context: Context) : FileLoweringPass {
    private val symbols = context.symbols
    private val irBuiltins = context.irBuiltIns
    private fun IrBuilderWithScope.irByteToBool(expression: IrExpression) = irCall(symbols.areEqualByValue[PrimitiveBinaryType.BYTE]!!).apply {
        arguments[0] = expression
        arguments[1] = irByte(1)
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
            parameters += buildReceiverParameter {
                type = scope.defaultType
                startOffset = irField.startOffset
                endOffset = irField.endOffset
            }
        }
        builder()
        annotations += buildSimpleAnnotation(context.irBuiltIns,
                SYNTHETIC_OFFSET, SYNTHETIC_OFFSET,
                context.symbols.typedIntrinsic.owner, intrinsicType.name)
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


    private inline fun atomicFunction(irField: IrField, type: AtomicFunctionType, builder: () -> IrSimpleFunction): IrSimpleFunction {
        val atomicFunctions = irField::atomicFunction.getOrSetIfNull { mutableMapOf() }
        return atomicFunctions.getOrPut(type) {
            builder().also {
                it.volatileField = irField
            }
        }
    }

    private fun compareAndSetFunction(irField: IrField) = atomicFunction(irField, AtomicFunctionType.COMPARE_AND_SET) {
        this.buildCasFunction(irField, IntrinsicType.COMPARE_AND_SET, this.context.irBuiltIns.booleanType)
    }
    private fun compareAndExchangeFunction(irField: IrField) = atomicFunction(irField, AtomicFunctionType.COMPARE_AND_EXCHANGE) {
        this.buildCasFunction(irField, IntrinsicType.COMPARE_AND_EXCHANGE, irField.type)
    }
    private fun getAndSetFunction(irField: IrField) = atomicFunction(irField, AtomicFunctionType.GET_AND_SET) {
        this.buildAtomicRWMFunction(irField, IntrinsicType.GET_AND_SET)
    }
    private fun getAndAddFunction(irField: IrField) = atomicFunction(irField, AtomicFunctionType.GET_AND_ADD) {
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


            private fun unsupported(message: String) = builder.irCall(context.symbols.throwIllegalArgumentExceptionWithMessage).apply {
                arguments[0] = builder.irString(message)
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

            private tailrec fun getConstPropertyReference(expression: IrExpression?, expectedReturn: IrReturnableBlockSymbol?) : IrRichPropertyReference? {
                return when {
                    expression == null -> null
                    expectedReturn == null && expression is IrRichPropertyReference -> expression
                    expectedReturn == null && expression is IrReturnableBlock -> getConstPropertyReference(expression.singleExpressionOrNull, expression.symbol)
                    expression is IrReturn && expression.returnTargetSymbol == expectedReturn -> getConstPropertyReference(expression.value, null)
                    expression is IrBlock -> getConstPropertyReference(expression.singleExpressionOrNull, expectedReturn)
                    expression is IrTypeOperatorCall && expression.operator == IrTypeOperator.IMPLICIT_CAST -> getConstPropertyReference(expression.argument, expectedReturn)
                    else -> null
                }
            }

            override fun visitCall(expression: IrCall): IrExpression {
                expression.transformChildrenVoid(this)
                val intrinsicType = tryGetIntrinsicType(expression).takeIf {
                    it in intrinsicMap || it == IntrinsicType.ATOMIC_GET_FIELD || it == IntrinsicType.ATOMIC_SET_FIELD
                } ?: return expression
                builder.at(expression)
                val extensionReceiver = expression.arguments[expression.symbol.owner.parameters.indexOfFirst { it.kind == IrParameterKind.ExtensionReceiver }]
                val reference = getConstPropertyReference(extensionReceiver, null)
                        ?: return unsupported("Only compile-time known IrProperties supported for $intrinsicType")
                val property = (reference.reflectionTargetSymbol as? IrPropertySymbol)?.owner
                val backingField = property?.backingField
                if (backingField?.hasAnnotation(KonanFqNames.volatile) != true) {
                    return unsupported("Only volatile properties are supported for $intrinsicType")
                }
                val function = when(intrinsicType) {
                    IntrinsicType.ATOMIC_GET_FIELD -> property.getter ?: error("Getter is not defined for the property: ${property.render()}")
                    IntrinsicType.ATOMIC_SET_FIELD -> property.setter ?: error("Setter is not defined for the property: ${property.render()}")
                    else -> intrinsicMap[intrinsicType]!!(backingField)
                }
                return builder.irCall(function).apply {
                    dispatchReceiver = reference.boundValues.singleOrNull()
                    val replacementParams = function.parameters.filter { it.kind == IrParameterKind.Regular }
                    val originalParams = expression.symbol.owner.parameters.filter { it.kind == IrParameterKind.Regular }
                    for ((from, to) in originalParams.zip(replacementParams)) {
                        arguments[to] = expression.arguments[from]
                    }
                }.let {
                    if (intrinsicType == IntrinsicType.ATOMIC_GET_FIELD || intrinsicType == IntrinsicType.ATOMIC_SET_FIELD) {
                        return it
                    }
                    if (backingField.requiresBooleanConversion()) {
                        for (param in function.parameters.filter { it.kind == IrParameterKind.Regular }) {
                            it.arguments[param] = builder.irBoolToByte(it.arguments[param]!!)
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