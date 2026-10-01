/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kir.printer.impl

import org.jetbrains.kotlin.kir.KirFunction
import org.jetbrains.kotlin.kir.KirModule
import org.jetbrains.kotlin.utils.IndentingPrinter
import org.jetbrains.kotlin.utils.SmartPrinter
import org.jetbrains.kotlin.utils.withIndent

internal class KirAsKotlinSourcesPrinter private constructor(
    private val printer: SmartPrinter,
) : IndentingPrinter by printer {
    companion object {
        fun print(module: KirModule): String {
            val printer = SmartPrinter(StringBuilder())
            KirAsKotlinSourcesPrinter(printer).printModule(module)
            return printer.toString().trimIndent()
        }
    }

    private fun printModule(module: KirModule) {
        module.functions.forEach { it.print() }
    }

    private fun KirFunction.print() {
        println("fun $name() {")
        withIndent {
            println("\"stub\"")
        }
        println("}")
    }
}
