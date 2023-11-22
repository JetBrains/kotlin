/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.printer

import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.visitors.SirVisitor

object SirAsSwiftSourcesPrinter : SirVisitor<Boolean, StringBuilder>() {

    override fun visitElement(element: SirElement, data: StringBuilder): Boolean {
        return false
    }

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

    override fun visitFunction(function: SirFunction, data: StringBuilder) = with(data) {
        append("public func ")
        append(function.name)
        append("(")
        function.accept(ParameterPrinter, data)
        append(")")
        append(" -> ")
        ParameterPrinter.visitType(function.returnType, this)
        appendLine(" {")
        appendLine("  ${printBody(function.body)}")
        appendLine("}")
        true
    }
}

private fun printBody(body: SirFunctionBody?): String {
    if (body == null) return "fatalError()"
    return "return ${body.generateCallSite()}"
}

private object ParameterPrinter : SirVisitor<Boolean, StringBuilder>() {
    fun visitParameter(param: SirParameter, data: StringBuilder) = with(data) {
        append(param.argumentName?.prependIndent())
        append(": ")
        visitType(param.type, data)
        true
    }

    fun visitType(type: SirType, data: StringBuilder) = with(data) {
        require(type is SirNominalType)
        append(type.declaration.name)
        true
    }

    override fun visitElement(element: SirElement, data: StringBuilder): Boolean {
        return false
    }

    override fun visitFunction(function: SirFunction, data: StringBuilder) = with(data) {
        if (function.parameters.isNotEmpty()) {
            appendLine()
        }
        function.parameters.forEach {
            visitParameter(it, data)
            if (function.parameters.last() != it) {
                append(",")
            }
            appendLine()
        }
        true
    }
}