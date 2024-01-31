/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.konan.descriptors.synthesizedName
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

internal class InitializersLowering(val context: CommonBackendContext) : ClassLoweringPass {
    companion object {
        val STATEMENT_ORIGIN_ANONYMOUS_INITIALIZER = IrStatementOriginImpl("ANONYMOUS_INITIALIZER")
        val DECLARATION_ORIGIN_ANONYMOUS_INITIALIZER = IrDeclarationOriginImpl("ANONYMOUS_INITIALIZER")
    }

    override fun lower(irClass: IrClass) {
        if (irClass.isInterface) return
        InitializersTransformer(irClass).lowerInitializers()
    }

    private inner class InitializersTransformer(val irClass: IrClass) {
        val initializers = mutableListOf<IrStatement>()
        val constInitializers = mutableListOf<IrStatement>()

        fun lowerInitializers() {
            collectAndRemoveInitializers()
            val initializeMethodSymbol = createInitializerMethod()
            lowerConstructors(initializeMethodSymbol)
        }

        private fun collectAndRemoveInitializers() {
            // Do with one traversal in order to preserve initializers order.
            irClass.transformChildrenVoid(object : IrElementTransformerVoid() {
                override fun visitClass(declaration: IrClass): IrStatement {
                    // Skip nested.
                    return declaration
                }

                override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer): IrStatement {
                    initializers.add(IrBlockImpl(declaration.startOffset, declaration.endOffset,
                            context.irBuiltIns.unitType, STATEMENT_ORIGIN_ANONYMOUS_INITIALIZER, declaration.body.statements))
                    return declaration
                }

                override fun visitField(declaration: IrField): IrStatement {
                    if (declaration.isStatic) return declaration
                    val initializer = declaration.initializer ?: return declaration
                    val startOffset = initializer.startOffset
                    val endOffset = initializer.endOffset
                    val initExpression = initializer.expression
                    val isConst = declaration.correspondingPropertySymbol?.owner?.isConst == true
                    if (isConst)
                        require(initExpression is IrConst<*>) { "Const val can only be initialized with a const: ${declaration.render()}" }
                    (if (isConst) constInitializers else initializers).add(
                            IrBlockImpl(startOffset, endOffset,
                                    context.irBuiltIns.unitType,
                                    IrStatementOrigin.INITIALIZE_FIELD,
                                    listOf(
                                            IrSetFieldImpl(startOffset, endOffset, declaration.symbol,
                                                    IrGetValueImpl(
                                                            startOffset, endOffset,
                                                            irClass.thisReceiver!!.type, irClass.thisReceiver!!.symbol
                                                    ),
                                                    initExpression,
                                                    context.irBuiltIns.unitType,
                                                    IrStatementOrigin.INITIALIZE_FIELD)))
                    )

                    // We shall keep initializer for constants for compile-time instantiation.
                    // We suppose that if the property is const, then its initializer is IrConst.
                    // If this requirement isn't satisfied, then PropertyAccessorInlineLowering can fail.
                    declaration.initializer = if (isConst) context.irFactory.createExpressionBody(initExpression.shallowCopy()) else null

                    return declaration
                }
            })

            irClass.declarations.transformFlat {
                if (it !is IrAnonymousInitializer)
                    null
                else listOf()
            }
        }

        private fun createInitializerMethod(): IrSimpleFunctionSymbol? {
            if (irClass.declarations.any { it is IrConstructor && it.isPrimary })
                return null // Place initializers in the primary constructor.

            val allInitializers = constInitializers + initializers
            if (allInitializers.isEmpty())
                return null
            val startOffset = irClass.startOffset
            val endOffset = irClass.endOffset
            val initializeFun =
                context.irFactory.createSimpleFunction(
                        startOffset,
                        endOffset,
                        DECLARATION_ORIGIN_ANONYMOUS_INITIALIZER,
                        "INITIALIZER".synthesizedName,
                        DescriptorVisibilities.PRIVATE,
                        isInline = false,
                        isExpect = false,
                        context.irBuiltIns.unitType,
                        Modality.FINAL,
                        IrSimpleFunctionSymbolImpl(),
                        isTailrec = false,
                        isSuspend = false,
                        isOperator = false,
                        isInfix = false,
                ).apply {
                    parent = irClass
                    irClass.declarations.add(this)

                    createDispatchReceiverParameter()

                    body = context.irFactory.createBlockBody(startOffset, endOffset, allInitializers)
                }

            for (initializer in allInitializers) {
                initializer.transformChildrenVoid(object : IrElementTransformerVoid() {
                    override fun visitGetValue(expression: IrGetValue): IrExpression {
                        if (expression.symbol == irClass.thisReceiver!!.symbol) {
                            return IrGetValueImpl(
                                    expression.startOffset,
                                    expression.endOffset,
                                    initializeFun.dispatchReceiverParameter!!.type,
                                    initializeFun.dispatchReceiverParameter!!.symbol
                            )
                        }
                        return expression
                    }
                })
                initializer.setDeclarationsParent(initializeFun)
            }

            return initializeFun.symbol
        }

        private fun lowerConstructors(initializeMethodSymbol: IrSimpleFunctionSymbol?) {
            irClass.transformChildrenVoid(object : IrElementTransformerVoid() {

                override fun visitClass(declaration: IrClass): IrStatement {
                    // Skip nested.
                    return declaration
                }

                override fun visitConstructor(declaration: IrConstructor): IrStatement {
                    val body = declaration.body ?: return declaration
                    val blockBody = body as? IrBlockBody
                            ?: throw AssertionError("Unexpected constructor body: ${declaration.body}")

                    blockBody.statements.transformFlat {
                        when {
                            it is IrInstanceInitializerCall -> {
                                if (initializeMethodSymbol == null) {
                                    val allInitializers = constInitializers + initializers
                                    require(declaration.isPrimary || allInitializers.isEmpty())
                                    for (initializer in allInitializers)
                                        initializer.setDeclarationsParent(declaration)
                                    allInitializers
                                } else {
                                    val startOffset = it.startOffset
                                    val endOffset = it.endOffset
                                    listOf(IrCallImpl(startOffset, endOffset,
                                            context.irBuiltIns.unitType, initializeMethodSymbol,
                                            initializeMethodSymbol.owner.typeParameters.size,
                                            initializeMethodSymbol.owner.valueParameters.size
                                    ).apply {
                                        dispatchReceiver = IrGetValueImpl(
                                                startOffset, endOffset,
                                                irClass.thisReceiver!!.type, irClass.thisReceiver!!.symbol
                                        )
                                    })
                                }
                            }

                            /**
                             * IR for kotlin.Any is:
                             * BLOCK_BODY
                             *   DELEGATING_CONSTRUCTOR_CALL 'constructor Any()'
                             *   INSTANCE_INITIALIZER_CALL classDescriptor='Any'
                             *
                             *   to avoid possible recursion we manually reject body generation for Any.
                             */
                            it is IrDelegatingConstructorCall
                                    && irClass.symbol == context.irBuiltIns.anyClass
                                    && it.symbol == declaration.symbol -> {
                                listOf()
                            }

                            else -> null
                        }
                    }

                    return declaration
                }
            })
        }
    }
}
