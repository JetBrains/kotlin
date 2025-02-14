/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.renderCompilerError
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationBase
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.visitors.IrTransformer
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

/**
 * This pass runs after inlining and performs the following additional transformations over some operations:
 *     - Convert expanded typeOf calls to IrConstantValue nodes as performance optimization
 *     - Convert immutableBlobOf() arguments to special IrConst.
 *     - Convert `obj::class` and `Class::class` to calls.
 */
internal class PostInlineLowering(val context: Context) : BodyLoweringPass {

    private val symbols get() = context.symbols

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        val irFile = container.file
        val classesToTransformToConstants = listOf(
                symbols.kTypeImpl,
                symbols.kTypeProjectionList,
                symbols.kTypeParameterImpl,
                symbols.kTypeImplForTypeParametersWithRecursiveBounds
        )
        irBody.transformChildren(object : IrTransformer<IrBuilderWithScope>() {
            override fun visitDeclaration(declaration: IrDeclarationBase, data: IrBuilderWithScope) =
                    super.visitDeclaration(declaration,
                            data = (declaration as? IrSymbolOwner)?.let { context.createIrBuilder(it.symbol, it.startOffset, it.endOffset) }
                                    ?: data
                    )

            override fun visitClassReference(expression: IrClassReference, data: IrBuilderWithScope): IrExpression {
                expression.transformChildren(this, data)

                val irClassSymbol = expression.symbol as? IrClassSymbol
                return if (irClassSymbol == null) {
                    data.at(expression).irCall(symbols.throwNullPointerException.owner)
                } else {
                    data.toNativeConstantReflectionBuilder(symbols).at(expression).irKClass(irClassSymbol)
                }
            }

            override fun visitConstructorCall(expression: IrConstructorCall, data: IrBuilderWithScope): IrElement {
                super.visitConstructorCall(expression, data)
                val clazz = expression.symbol.owner.constructedClass
                if (clazz.symbol in classesToTransformToConstants) {
                    fun IrElement.isConvertibleToConst() : Boolean = this is IrConstantValue || this is IrConst || (this is IrVararg  && elements.all { it.isConvertibleToConst() })
                    fun IrElement.convertToConst() : IrConstantValue = when (this) {
                        is IrConstantValue -> this
                        is IrConst -> data.irConstantPrimitive(this)
                        is IrVararg -> data.irConstantArray(type, elements.map { e -> e.convertToConst() })
                        else -> shouldNotBeCalled()
                    }
                    if (expression.arguments.all { it!!.isConvertibleToConst() }) {
                        return data.at(expression).irConstantObject(expression.symbol,
                                expression.arguments.map { it!!.convertToConst() },
                                expression.typeArguments.map { it!! }
                        )
                    }
                }
                return expression
            }

            override fun visitGetClass(expression: IrGetClass, data: IrBuilderWithScope): IrExpression {
                expression.transformChildren(this, data)

                return data.at(expression).run {
                    irCallWithSubstitutedType(symbols.kClassImplConstructor, listOf(expression.argument.type)).apply {
                        val typeInfo = irCall(symbols.getObjectTypeInfo).apply {
                            arguments[0] = expression.argument
                        }

                        arguments[0] = typeInfo
                    }
                }
            }

            override fun visitCall(expression: IrCall, data: IrBuilderWithScope): IrExpression {
                expression.transformChildren(this, data)

                // Function inlining is changing function symbol at callsite
                // and unbound symbol replacement is happening later.
                // So we compare descriptors for now.
                if (expression.symbol == symbols.immutableBlobOf) {
                    // Convert arguments of the binary blob to special IrConst structure, so that
                    // vararg lowering will not affect it.
                    val args = expression.arguments[0] as? IrVararg
                            ?: error("varargs shall not be lowered yet")
                    val builder = StringBuilder()
                    args.elements.forEach {
                        require(it is IrConst) { renderCompilerError(irFile, it, "expected const") }
                        val value = it.value
                        require(value is Short && value >= 0 && value <= 0xff) {
                            renderCompilerError(irFile, it, "incorrect value for binary data: $value")
                        }
                        // Luckily, all values in range 0x00 .. 0xff represent valid UTF-16 symbols,
                        // block 0 (Basic Latin) and block 1 (Latin-1 Supplement) in
                        // Basic Multilingual Plane, so we could just append data "as is".
                        builder.append(value.toInt().toChar())
                    }
                    return data.irCall(context.symbols.immutableBlobOfImpl).apply {
                        arguments[0] = data.irString(builder.toString())
                    }
                }

                return expression
            }

        }, data = context.createIrBuilder((container as IrSymbolOwner).symbol, irBody.startOffset, irBody.endOffset))
    }
}