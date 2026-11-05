/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kir.printer.impl

import org.jetbrains.kotlin.kir.*
import org.jetbrains.kotlin.utils.IndentingPrinter
import org.jetbrains.kotlin.utils.SmartPrinter
import org.jetbrains.kotlin.utils.withIndent

internal class KirAsKotlinSourcesPrinter private constructor(
    private val printer: SmartPrinter,
    private val emptyBodyStub: KirFunctionBody,
) : IndentingPrinter by printer {
    companion object {
        fun print(
            module: KirModule,
            emptyBodyStub: KirFunctionBody,
        ): String {
            val printer = KirAsKotlinSourcesPrinter(
                SmartPrinter(StringBuilder()),
                emptyBodyStub = emptyBodyStub,
            )
            val declarationsString = with(printer) {
                module.printChildren()
                toString().trimIndent()
            }
            return declarationsString
        }
    }

    private fun KirDeclarationContainer.printChildren() = this.declarations.forEach {
        it.print()
    }

    private fun KirDeclaration.print() {
        when (this) {
            is KirCallable -> printDeclaration()
        }
    }

    private fun KirDeclaration.printVisibility() = print("${this.visibility.value} ")

    private fun KirCallable.printDeclaration() {
        printVisibility()
        printName()
        print("()")
        println(" {")
        withIndent {
            body.print()
        }
        println("}")
    }

    private fun KirCallable.printName() = print(
        when (this) {
            is KirFunction -> "fun $name"
        }
    )

    private fun KirFunctionBody?.print() = (this ?: emptyBodyStub)
        .statements
        .flatMap { it.split("\n") }
        .forEach {
            println(it)
        }
}
