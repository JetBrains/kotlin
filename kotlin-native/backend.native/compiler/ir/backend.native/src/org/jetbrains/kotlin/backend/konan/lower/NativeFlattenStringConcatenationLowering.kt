/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrStringConcatenationImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.hasShape
import org.jetbrains.kotlin.ir.util.isTopLevelInPackage
import org.jetbrains.kotlin.ir.util.isUnsigned
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.util.OperatorNameConventions
import kotlin.math.max
import kotlin.math.min

/** Index-driven Native fork of `FlattenStringConcatenationLowering`. */
internal class NativeFlattenStringConcatenationLowering(
        val generationState: NativeGenerationState,
) : FileLoweringPass {
    private val context: CommonBackendContext = generationState.context

    override fun lower(irFile: IrFile) {
        val index = generationState.fileLowerState.irElementIndex

        index.forEach<IrStringConcatenation> { concat ->
            val replacement = IrStringConcatenationImpl(
                    concat.startOffset, concat.endOffset, concat.type,
                    collectStringConcatenationArguments(concat)
            ).tryToFold()
            index.spliceIfNeeded(concat, replacement, reindexNewSubtree = true, reindexOldSubtree = false)
        }

        index.forEach<IrCall> { call ->
            if (!call.isStringPlusCall && !call.isSpecialToStringCall) return@forEach
            val replacement = IrStringConcatenationImpl(
                    call.startOffset, call.endOffset, call.type,
                    collectStringConcatenationArguments(call)
            ).tryToFold()
            index.spliceIfNeeded(call, replacement, reindexNewSubtree = true, reindexOldSubtree = false)
        }
    }

    private fun IrStringConcatenation.tryToFold(): IrExpression {
        val folded = mutableListOf<IrExpression>()
        for (next in this.arguments) {
            val last = folded.lastOrNull()
            when {
                next !is IrConst -> folded += next
                last !is IrConst -> folded += IrConstImpl.string(
                        next.startOffset, next.endOffset, context.irBuiltIns.stringType, constToString(next)
                )
                else -> folded[folded.size - 1] = IrConstImpl.string(
                        // Inlined strings may have `last.startOffset > next.endOffset`
                        min(last.startOffset, next.startOffset), max(last.endOffset, next.endOffset),
                        context.irBuiltIns.stringType,
                        constToString(last) + constToString(next)
                )
            }
        }
        return folded.singleOrNull() as? IrConst
                ?: IrStringConcatenationImpl(this.startOffset, this.endOffset, this.type, folded)
    }

    private fun constToString(const: IrConst): String {
        return normalizeUnsignedValue(const).toString()
    }

    private fun normalizeUnsignedValue(const: IrConst): Any? {
        if (const.type.isUnsigned()) {
            when (val kind = const.kind) {
                is IrConstKind.Byte ->
                    return (const.value as Byte).toUByte()
                is IrConstKind.Short ->
                    return (const.value as Short).toUShort()
                is IrConstKind.Int ->
                    return (const.value as Int).toUInt()
                is IrConstKind.Long ->
                    return (const.value as Long).toULong()
                else -> {}
            }
        }
        return const.value
    }

    companion object {
        // Same parent-name set as the common lowering.
        private val PARENT_NAMES = setOf(
                StandardNames.BUILT_INS_PACKAGE_FQ_NAME,
                StandardNames.FqNames.string.toSafe()
        )

        val IrCall.isStringPlusCall: Boolean
            get() {
                val function = symbol.owner
                return (function.hasShape(dispatchReceiver = true, regularParameters = 1)
                        || function.hasShape(extensionReceiver = true, regularParameters = 1))
                        && function.parameters[0].type.isStringClassType()
                        && function.returnType.isStringClassType()
                        && function.name == OperatorNameConventions.PLUS
                        && function.fqNameWhenAvailable?.parent() in PARENT_NAMES
            }

        val IrSimpleFunction.isToString: Boolean
            get() = name == OperatorNameConventions.TO_STRING
                    && hasShape(dispatchReceiver = true)
                    && returnType.isString()
                    && (parameters[0].type.isAny() || overriddenSymbols.isNotEmpty())

        private val IrSimpleFunction.isNullableToString: Boolean
            get() = isTopLevelInPackage(OperatorNameConventions.TO_STRING.asString(), StandardNames.BUILT_INS_PACKAGE_FQ_NAME)
                    && returnType.isString()
                    && hasShape(extensionReceiver = true)
                    && parameters[0].type.isNullableAny()

        val IrCall.isToStringCall: Boolean
            get() {
                if (superQualifierSymbol != null)
                    return false

                val function = symbol.owner
                return function.isToString || function.isNullableToString
            }

        val IrCall.isSpecialToStringCall: Boolean
            get() = isToStringCall && dispatchReceiver?.type?.isPrimitiveType() != false

        private fun isStringConcatenationExpression(expression: IrExpression): Boolean =
                (expression is IrStringConcatenation) || (expression is IrCall) && expression.isStringPlusCall

        private fun collectStringConcatenationArguments(expression: IrExpression): List<IrExpression> {
            val arguments = mutableListOf<IrExpression>()
            expression.acceptChildrenVoid(object : IrVisitorVoid() {

                override fun visitElement(element: IrElement) {
                    element.acceptChildrenVoid(this)
                }

                override fun visitCall(expression: IrCall) {
                    if (isStringConcatenationExpression(expression) || expression.isToStringCall) {
                        expression.acceptChildrenVoid(this)
                    } else {
                        arguments.add(expression)
                    }
                }

                override fun visitStringConcatenation(expression: IrStringConcatenation) {
                    expression.acceptChildrenVoid(this)
                }

                override fun visitExpression(expression: IrExpression) {
                    arguments.add(expression)
                }
            })

            return arguments
        }
    }
}
