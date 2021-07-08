/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.ir

import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.createDispatchReceiverParameter
import org.jetbrains.kotlin.backend.konan.KonanBackendContext
import org.jetbrains.kotlin.backend.konan.KonanFqNames
import org.jetbrains.kotlin.backend.konan.descriptors.getAnnotationStringValue
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.addMember
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.isAnnotationWithEqualFqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.OperatorNameConventions

// Generate additional functions for array set and get operators without bounds checking.
internal class FunctionsWithoutBoundCheckGenerator(val context: KonanBackendContext) {
    private val symbols = context.ir.symbols

    private fun generateFunction(baseFunction: IrSimpleFunction, functionName: Name) =
            context.irFactory.createFunction(
                    baseFunction.startOffset, baseFunction.endOffset,
                    IrDeclarationOrigin.GENERATED_SETTER_GETTER,
                    IrSimpleFunctionSymbolImpl(),
                    functionName,
                    DescriptorVisibilities.PUBLIC,
                    Modality.FINAL,
                    baseFunction.returnType,
                    isInline = baseFunction.isInline,
                    isExternal = baseFunction.isExternal,
                    isTailrec = baseFunction.isTailrec,
                    isSuspend = baseFunction.isSuspend,
                    isExpect = baseFunction.isExpect,
                    isFakeOverride = baseFunction.isFakeOverride,
                    isOperator = baseFunction.isOperator,
                    isInfix = baseFunction.isInfix
            ).also { function ->
                function.parent = baseFunction.parent
                function.createDispatchReceiverParameter()
                function.valueParameters = baseFunction.valueParameters.map { it.copyTo(function) }
                // Copy annotations.
                val setWithoutBEAnnotations = baseFunction.annotations.map { annotation ->
                    annotation.deepCopyWithSymbols().also { copy ->
                        if (copy.isAnnotationWithEqualFqName(KonanFqNames.gcUnsafeCall)) {
                            val value = "${annotation.getAnnotationStringValue("callee")}_without_BoundCheck"
                            copy.putValueArgument(0,
                                    IrConstImpl.string(UNDEFINED_OFFSET, UNDEFINED_OFFSET, context.irBuiltIns.stringType, value))
                        }
                    }
                }
                function.annotations = setWithoutBEAnnotations
            }


    fun generate() {
        val arraysClasses = symbols.primitiveTypesToPrimitiveArrays.values + symbols.array
        arraysClasses.forEach { classSymbol ->
            val setFunction = classSymbol.owner.functions.single { it.name == OperatorNameConventions.SET }
            classSymbol.owner.addMember(generateFunction(setFunction, KonanNameConventions.setWithoutBoundCheck))

            val getFunction = classSymbol.owner.functions.single { it.descriptor.name == OperatorNameConventions.GET }
            classSymbol.owner.addMember(generateFunction(getFunction, KonanNameConventions.getWithoutBoundCheck))
        }
    }
}