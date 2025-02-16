/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.plugin.sandbox.ir

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.toIrConst
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.FqName
import java.nio.ByteBuffer

const val pluginId = "org.jetbrains.kotlin.plugin.sandbox"

class MetadataExtensionEmitter(val context: IrPluginContext) : IrVisitorVoid() {
    companion object {
        private val markerAnnotationFqName = FqName("org.jetbrains.kotlin.plugin.sandbox.EmitMetadata")
    }

    override fun visitElement(element: IrElement) {
        when (element) {
            is IrFile,
            is IrModuleFragment -> element.acceptChildrenVoid(this)
            else -> {}
        }
    }

    override fun visitClass(declaration: IrClass) {
        declaration.getAnnotation(markerAnnotationFqName)?.let { annotation ->
            emitMetadata(declaration, annotation)
        }
        declaration.acceptChildrenVoid(this)
    }

    private fun emitMetadata(irClass: IrClass, annotation: IrConstructorCall) {
        val value = (annotation.arguments[0] as IrConst).value as Int

        context.metadataDeclarationRegistrar.addCustomMetadataExtension(
            irClass,
            pluginId,
            value.toByteArray()
        )
    }
}

class MetadataExtensionExtractor(val context: IrPluginContext) : IrVisitorVoid() {
    companion object {
        private val markerAnnotationFqName = FqName("org.jetbrains.kotlin.plugin.sandbox.GenerateBodyUsingEmittedMetadata")
    }


    override fun visitElement(element: IrElement) {
        when (element) {
            is IrFile,
            is IrModuleFragment -> element.acceptChildrenVoid(this)
            else -> {}
        }
    }

    override fun visitFunction(declaration: IrFunction) {
        if (!declaration.hasAnnotation(markerAnnotationFqName)) return
        val parameterClass = declaration.parameters.firstOrNull { it.kind == IrParameterKind.Regular }?.type?.classOrNull?.owner ?: return
        val valueFromMetadata = context.metadataDeclarationRegistrar.getCustomMetadataExtension(
            parameterClass,
            pluginId
        )?.toInt() ?: return
        val irFactory = context.irFactory
        val irBuiltIns = context.irBuiltIns
        val returnExpression = IrReturnImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            type = irBuiltIns.nothingType,
            returnTargetSymbol = declaration.symbol,
            value = valueFromMetadata.toIrConst(irBuiltIns.intType)
        )
        declaration.body = irFactory.createExpressionBody(returnExpression)
    }
}

private fun Int.toByteArray(): ByteArray {
    return ByteBuffer.allocate(Int.SIZE_BYTES).putInt(this).array()
}

private fun ByteArray.toInt(): Int {
    return ByteBuffer.wrap(this).int
}
