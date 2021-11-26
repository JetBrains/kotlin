/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.plugin

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.fir.backend.IrPluginDeclarationOrigin
import org.jetbrains.kotlin.fir.plugin.generators.ExternalClassGenerator
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

class GeneratedDeclarationsIrBodyFiller : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        moduleFragment.acceptChildrenVoid(Transformer(pluginContext))
    }
}

private class Transformer(val context: IrPluginContext) : IrElementVisitorVoid {
    private val irFactory = context.irFactory
    private val irBuiltIns = context.irBuiltIns

    override fun visitElement(element: IrElement) {
        when (element) {
            is IrDeclaration,
            is IrFile,
            is IrModuleFragment -> element.acceptChildrenVoid(this)
            else -> {}
        }
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction) {
        val origin = declaration.origin
        if (origin !is IrPluginDeclarationOrigin || origin.pluginKey != ExternalClassGenerator.Key) return
        require(declaration.body == null)
        val constructedType = declaration.returnType as? IrSimpleType ?: return
        val constructedClassSymbol = constructedType.classifier
        val constructedClass = constructedClassSymbol.owner as? IrClass ?: return
        val constructor = constructedClass.primaryConstructor ?: return
        val constructorCall = IrConstructorCallImpl(
            -1,
            -1,
            constructedType,
            constructor.symbol,
            typeArgumentsCount = 0,
            constructorTypeArgumentsCount = 0,
            valueArgumentsCount = 0
        )
        val returnStatement = IrReturnImpl(-1, -1, irBuiltIns.nothingType, declaration.symbol, constructorCall)
        declaration.body = irFactory.createBlockBody(-1, -1, listOf(returnStatement))
    }

    override fun visitConstructor(declaration: IrConstructor) {
        val origin = declaration.origin
        if (origin !is IrPluginDeclarationOrigin || origin.pluginKey != ExternalClassGenerator.Key) return
        require(declaration.body == null)
        val type = declaration.returnType as? IrSimpleType ?: return

        val delegatingAnyCall = IrDelegatingConstructorCallImpl(
            -1,
            -1,
            irBuiltIns.anyType,
            irBuiltIns.anyClass.owner.primaryConstructor?.symbol ?: return,
            typeArgumentsCount = 0,
            valueArgumentsCount = 0
        )

        val initializerCall = IrInstanceInitializerCallImpl(
            -1,
            -1,
            (declaration.parent as? IrClass)?.symbol ?: return,
            type
        )

        declaration.body = irFactory.createBlockBody(-1, -1, listOf(delegatingAnyCall, initializerCall))
    }
}
