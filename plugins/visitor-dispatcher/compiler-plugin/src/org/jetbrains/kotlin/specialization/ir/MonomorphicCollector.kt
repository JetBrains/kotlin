/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.specialization.ir

import org.jetbrains.kotlin.common.IrDefaultElementVisitor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.specialization.common.isMonomorphic

class MonomorphicFunctionCollector: IrDefaultElementVisitor() {
    val functions = mutableListOf<IrSimpleFunction>()

    override fun visitSimpleFunction(declaration: IrSimpleFunction, data: Nothing?) {
        if (declaration.isMonomorphic()) {
            functions.add(declaration)
        }
        super.visitSimpleFunction(declaration, data)
    }
}

class MonomorphicCallsCollector: IrElementVisitorVoid {
    val calls = mutableListOf<IrCall>()

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitCall(expression: IrCall) {
        if (expression.symbol.owner.isMonomorphic()) {
            calls.add(expression)
        }
        super.visitCall(expression)
    }
}
