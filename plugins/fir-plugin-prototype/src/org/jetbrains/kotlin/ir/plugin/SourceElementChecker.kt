/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.plugin

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.java.JavaBinarySourceElement
import org.jetbrains.kotlin.fir.lazy.Fir2IrLazyClass
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.load.java.structure.impl.VirtualFileBoundJavaClass
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinarySourceElement
import org.jetbrains.kotlin.load.kotlin.VirtualFileKotlinClass
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

@OptIn(UnsafeDuringIrConstructionAPI::class)
class SourceElementChecker(val context: IrPluginContext) : IrElementVisitorVoid {
    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitFunction(declaration: IrFunction) {
        val type = declaration.returnType as? IrSimpleType ?: return
        val klass = type.classifier.owner as? IrClass ?: return
        if (klass.startOffset != UNDEFINED_OFFSET) {
            return
        }
        if (klass.origin is IrDeclarationOrigin.GeneratedByPlugin) {
            return
        }
        if (klass is Fir2IrLazyClass && klass.fir.origin == FirDeclarationOrigin.BuiltIns) {
            return
        }
        when (val sourceElement = klass.source) {
            is KotlinJvmBinarySourceElement -> {
                val binaryClass = sourceElement.binaryClass
                if (binaryClass !is VirtualFileKotlinClass) {
                    throw AssertionError("No virtual file found: ${binaryClass.classId}")
                }
            }
            is JavaBinarySourceElement -> {
                val javaClass = sourceElement.javaClass
                (javaClass as VirtualFileBoundJavaClass).virtualFile
                    ?: throw AssertionError("No virtual file found: ${javaClass.fqName}")
            }
            is DeserializedContainerSource -> {
                // Klib element is possible here in JS test
                return
            }
            SourceElement.NO_SOURCE -> {
                throw AssertionError("No source found: ${klass.fqNameForIrSerialization}")
            }
            else -> {
                throw AssertionError("Unknown source element: ${sourceElement.javaClass}")
            }
        }
    }
}
