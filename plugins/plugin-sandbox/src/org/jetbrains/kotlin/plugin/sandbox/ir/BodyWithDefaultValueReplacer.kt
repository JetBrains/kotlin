/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.plugin.sandbox.ir

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.util.deepCopyWithoutPatchingParents
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.Name

class BodyWithDefaultValueReplacer : IrVisitorVoid() {
    companion object {
        val NAME = Name.identifier("replaceMeWithDefault")
    }

    override fun visitFile(declaration: IrFile) {
        declaration.acceptChildrenVoid(this)
    }

    override fun visitClass(declaration: IrClass) {
        declaration.acceptChildrenVoid(this)
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction) {
        if (declaration.name != NAME) return
        val defaultValue = declaration.parameters.first { it.kind == IrParameterKind.Regular }.defaultValue ?: return
        val bodyReplacer = object : IrVisitorVoid() {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitReturn(expression: IrReturn) {
                expression.value = defaultValue.expression.deepCopyWithoutPatchingParents()
            }
        }
        declaration.body?.acceptChildrenVoid(bodyReplacer)
    }
}
