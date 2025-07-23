/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.plugin.sandbox.ir

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * If some function is annotated with `@CallSpecifiedFunction(functionFqName)` then this transformer
 * inserts the call of this function in the first statement of the body of annotated function.
 */
class IrTransformerForICTesting(private val context: IrPluginContext) : IrVisitorVoid() {
    companion object {
        private val ANNOTATION_FQ_NAME = FqName.fromSegments("org.jetbrains.kotlin.plugin.sandbox.CallSpecifiedFunction".split("."))
    }

    override fun visitElement(element: IrElement) {
        when (element) {
            is IrDeclaration,
            is IrFile,
            is IrModuleFragment
                -> element.acceptChildrenVoid(this)
            else -> {}
        }
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction) {
        val annotationArgument = declaration.getAnnotation(ANNOTATION_FQ_NAME)?.arguments?.firstOrNull() ?: return
        val argumentString = (annotationArgument as? IrConst)?.value as? String ?: return
        val callableId = run {
            val packageFqName = FqName.fromSegments(argumentString.substringBeforeLast('.').split("."))
            val functionName = Name.identifier(argumentString.substringAfterLast('.'))
            CallableId(packageFqName, functionName)
        }
        val functionToCall = context.referenceFunctions(callableId).singleOrNull()?.owner ?: return
        val body = declaration.body as? IrBlockBody ?: return
        val functionCall = IrCallImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            type = functionToCall.returnType,
            symbol = functionToCall.symbol,
            origin = null,
            superQualifierSymbol = null
        )
        body.statements.add(0, functionCall)
    }
}
