/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.common

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

abstract class IrDefaultElementVisitor: IrElementVisitorVoid {
    final override fun visitElement(element: IrElement) {
        when (element) {
            is IrDeclaration,
            is IrFile,
            is IrModuleFragment -> element.acceptChildrenVoid(this)
            else -> {}
        }
    }
}

abstract class AbstractFunctionBodyGenerator(protected val context: IrPluginContext) : IrDefaultElementVisitor() {
    abstract fun interestedIn(key: GeneratedDeclarationKey): Boolean
    abstract fun generateBodyForFunction(function: IrSimpleFunction, key: GeneratedDeclarationKey): IrBody?

    final override fun visitSimpleFunction(declaration: IrSimpleFunction) {
        val origin = declaration.origin
        if (origin !is IrDeclarationOrigin.GeneratedByPlugin || !interestedIn(origin.pluginKey)) return
        require(declaration.body == null)
        declaration.body = generateBodyForFunction(declaration, origin.pluginKey)
    }
}
