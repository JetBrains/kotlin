/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.printer

import org.jetbrains.kotlin.sir.*

object SirPrinter : SirVisitor<StringBuilder, Boolean> {

    fun print(element: SirElement): String = buildString {
        element.accept(this@SirPrinter, this)
    }

    override fun acceptModule(module: SirModule, data: StringBuilder) = with(data) {
        module.declarations.forEach {
            val wasWritten = it.accept(this@SirPrinter, this)
            if (module.declarations.last() != it && wasWritten) {
                appendLine()
                appendLine()
            }
        }
        true
    }

    override fun acceptSwiftFunction(function: SirFunction, data: StringBuilder) = with(data) {
        append("func ")
        append(function.name)
        append("(")
        ParameterPrinter.acceptSwiftFunction(function, this)
        append(")")
        append(" -> ")
        ParameterPrinter.acceptType(function.returnType, this)
        append(" { fatalError() }")
        true
    }

    override fun acceptType(type: SirType, data: StringBuilder) = false // we do not support new types currently

    override fun acceptParameter(param: SirParameter, data: StringBuilder) = false // we do not support top level properties currently
    override fun acceptForeignFunction(function: SirForeignFunction, data: StringBuilder) = false // we do not write Foreign nodes
}

private object ParameterPrinter : SirVisitor<StringBuilder, Boolean> {
    override fun acceptParameter(param: SirParameter, data: StringBuilder) = with(data) {
        append(param.name.prependIndent())
        append(": ")
        param.type.accept(this@ParameterPrinter, this)
        true
    }

    override fun acceptType(type: SirType, data: StringBuilder) = with(data) {
        append(type.name)
        true
    }

    override fun acceptSwiftFunction(function: SirFunction, data: StringBuilder) = with(data) {
        if (function.arguments.isNotEmpty()) {
            appendLine()
        }
        function.arguments.forEach {
            it.accept(this@ParameterPrinter, this)
            if (function.arguments.last() != it) {
                append(",")
            }
            appendLine()
        }
        true
    }

    override fun acceptModule(module: SirModule, data: StringBuilder) = false
    override fun acceptForeignFunction(function: SirForeignFunction, data: StringBuilder) = false
}