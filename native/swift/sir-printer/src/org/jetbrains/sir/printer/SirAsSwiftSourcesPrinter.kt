/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.printer

import org.jetbrains.kotlin.sir.*

object SirAsSwiftSourcesPrinter : SirVisitor<StringBuilder, Boolean> {

    fun print(element: SirElement): String = buildString {
        element.accept(this@SirAsSwiftSourcesPrinter, this)
    }

    override fun visitModule(module: SirModule, data: StringBuilder) = with(data) {
        module.declarations.forEach {
            val wasWritten = it.accept(this@SirAsSwiftSourcesPrinter, this)
            if (module.declarations.last() != it && wasWritten) {
                appendLine()
                appendLine()
            }
        }
        true
    }

    override fun visitSwiftFunction(function: SirFunction, data: StringBuilder) = with(data) {
        append("public func ")
        append(function.name)
        append("(")
        ParameterPrinter.visitSwiftFunction(function, this)
        append(")")
        append(" -> ")
        ParameterPrinter.visitType(function.returnType, this)
        append(" { fatalError() }")
        true
    }

    override fun visitType(type: SirType, data: StringBuilder) = false // we do not support new types currently

    override fun visitParameter(param: SirParameter, data: StringBuilder) = false // we do not support top level properties currently
    override fun visitForeignFunction(function: SirForeignFunction, data: StringBuilder) = false // we do not write Foreign nodes
}

private object ParameterPrinter : SirVisitor<StringBuilder, Boolean> {
    override fun visitParameter(param: SirParameter, data: StringBuilder) = with(data) {
        append(param.name.prependIndent())
        append(": ")
        param.type.accept(this@ParameterPrinter, this)
        true
    }

    override fun visitType(type: SirType, data: StringBuilder) = with(data) {
        append(type.name)
        true
    }

    override fun visitSwiftFunction(function: SirFunction, data: StringBuilder) = with(data) {
        if (function.parameters.isNotEmpty()) {
            appendLine()
        }
        function.parameters.forEach {
            it.accept(this@ParameterPrinter, this)
            if (function.parameters.last() != it) {
                append(",")
            }
            appendLine()
        }
        true
    }

    override fun visitModule(module: SirModule, data: StringBuilder) = false
    override fun visitForeignFunction(function: SirForeignFunction, data: StringBuilder) = false
}