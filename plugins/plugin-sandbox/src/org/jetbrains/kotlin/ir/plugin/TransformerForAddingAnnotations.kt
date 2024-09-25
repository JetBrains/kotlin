/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.plugin

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.fromSymbolOwner
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isAnnotationClass
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class TransformerForAddingAnnotations(val context: IrPluginContext) : IrElementVisitorVoid {
    companion object {
        private val markerAnnotationFqName = FqName("org.jetbrains.kotlin.fir.plugin.AddAnnotations")
        private val annotationToAddId = ClassId(FqName("org.jetbrains.kotlin.fir.plugin"), Name.identifier("AnnotationToAdd"))
        private val annotationToAddFqName = annotationToAddId.asSingleFqName()
        private const val prefixNameForClass = "VerySpecificName"
    }

    private val annotationsAdder = AnnotationsAdder()

    override fun visitElement(element: IrElement) {
        when (element) {
            is IrFile,
            is IrModuleFragment -> element.acceptChildrenVoid(this)
            else -> {}
        }
    }

    override fun visitClass(declaration: IrClass) {
        if (declaration.hasAnnotation(markerAnnotationFqName) || declaration.name.asString().startsWith(prefixNameForClass)) {
            declaration.acceptVoid(annotationsAdder)
        }
    }

    private inner class AnnotationsAdder : IrElementVisitorVoid {
        val annotationClass = context.referenceClass(annotationToAddId)?.takeIf { it.owner.isAnnotationClass }

        override fun visitElement(element: IrElement, data: Nothing?) {}

        override fun visitDeclaration(declaration: IrDeclarationBase) {
            addAnnotation(declaration)
            declaration.acceptChildrenVoid(this)
        }

        override fun visitFunction(declaration: IrFunction) {
            if (declaration.isFakeOverride) return
            visitDeclaration(declaration)
        }

        private fun addAnnotation(declaration: IrDeclarationBase) {
            if (declaration.hasAnnotation(annotationToAddFqName)) return
            val annotationClass = annotationClass ?: return
            val annotationConstructor = annotationClass.owner.constructors.first()
            val annotationCall = IrConstructorCallImpl.fromSymbolOwner(
                type = annotationClass.defaultType,
                constructorSymbol = annotationConstructor.symbol
            ).also {
                it.putValueArgument(
                    0,
                    IrConstImpl.boolean(UNDEFINED_OFFSET, UNDEFINED_OFFSET, context.irBuiltIns.booleanType, true)
                )
                it.putValueArgument(
                    1,
                    IrConstImpl.byte(UNDEFINED_OFFSET, UNDEFINED_OFFSET, context.irBuiltIns.byteType, 1)
                )
                it.putValueArgument(
                    2,
                    IrConstImpl.char(UNDEFINED_OFFSET, UNDEFINED_OFFSET, context.irBuiltIns.charType, 'c')
                )
                it.putValueArgument(
                    3,
                    IrConstImpl.double(UNDEFINED_OFFSET, UNDEFINED_OFFSET, context.irBuiltIns.doubleType, 4.2)
                )
                it.putValueArgument(
                    4,
                    IrConstImpl.float(UNDEFINED_OFFSET, UNDEFINED_OFFSET, context.irBuiltIns.floatType, 2.4f)
                )
                it.putValueArgument(
                    5,
                    IrConstImpl.int(UNDEFINED_OFFSET, UNDEFINED_OFFSET, context.irBuiltIns.intType, 42)
                )
                it.putValueArgument(
                    6,
                    IrConstImpl.long(UNDEFINED_OFFSET, UNDEFINED_OFFSET, context.irBuiltIns.longType, 24L)
                )
                it.putValueArgument(
                    7,
                    IrConstImpl.short(UNDEFINED_OFFSET, UNDEFINED_OFFSET, context.irBuiltIns.shortType, 7)
                )
                it.putValueArgument(
                    8,
                    IrConstImpl.string(UNDEFINED_OFFSET, UNDEFINED_OFFSET, context.irBuiltIns.stringType, "OK")
                )
            }
            context.metadataDeclarationRegistrar.addMetadataVisibleAnnotationsToElement(declaration, annotationCall)
        }
    }
}
