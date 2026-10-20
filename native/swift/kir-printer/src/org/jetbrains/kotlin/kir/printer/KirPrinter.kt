/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kir.printer

import org.jetbrains.kotlin.kir.KirFunctionBody
import org.jetbrains.kotlin.kir.KirModule
import org.jetbrains.kotlin.kir.printer.impl.KirAsKotlinSourcesPrinter
import org.jetbrains.kotlin.kir.printer.impl.renderCDeclarations
import org.jetbrains.kotlin.kir.printer.impl.renderSwiftBridges

/**
 * Prints KIR as Kotlin sources.
 */
public class KirPrinter(
    private val emptyBodyStub: KirFunctionBody = defaultEmptyBodyStub,
) {
    /**
     * Prints the Kotlin sources of [module], including the Kotlin declarations of its bridges.
     */
    public fun print(module: KirModule): String = KirAsKotlinSourcesPrinter.print(
        module, emptyBodyStub
    )

    /**
     * Prints the Swift part of the bridges of [module]: `@_cdecl` functions that implement the bridges.
     * The module the bridges call into must be imported by the file they are printed to.
     */
    public fun printSwiftBridges(module: KirModule): String = renderSwiftBridges(module)

    /**
     * Prints the C part of the bridges of [module]: includes and declarations of the bridge functions.
     */
    public fun printCDeclarations(module: KirModule): String = renderCDeclarations(module)
}

private val defaultEmptyBodyStub: KirFunctionBody = KirFunctionBody(
    listOf("TODO(\"Body is absent\")")
)
