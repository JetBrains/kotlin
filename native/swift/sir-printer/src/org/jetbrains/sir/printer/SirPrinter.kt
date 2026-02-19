/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.printer

import org.jetbrains.kotlin.sir.SirBridge
import org.jetbrains.kotlin.sir.SirBridged
import org.jetbrains.kotlin.sir.SirDeclarationContainer
import org.jetbrains.kotlin.sir.SirFunctionBody
import org.jetbrains.kotlin.sir.SirModule
import org.jetbrains.kotlin.sir.SirSubscript
import org.jetbrains.kotlin.sir.SirVariable
import org.jetbrains.kotlin.sir.util.Comparators
import org.jetbrains.kotlin.sir.util.accessors
import org.jetbrains.sir.printer.impl.CBridgePrinter
import org.jetbrains.sir.printer.impl.KotlinBridgePrinter
import org.jetbrains.sir.printer.impl.SirAsSwiftSourcesPrinter

public class SirPrinter(
    private val stableDeclarationsOrder: Boolean = true,
    private val renderDocComments: Boolean = false,
    private val renderDeclarationOrigins: Boolean = false,
    private val emptyBodyStub: SirFunctionBody = fatalErrorBodyStub
) {
    public inner class Printout(private val module: SirModule) {
        public val swiftSource: Sequence<String> by lazy {
            val result = SirAsSwiftSourcesPrinter.print(
                module,
                stableDeclarationsOrder = this@SirPrinter.stableDeclarationsOrder,
                renderDocComments = this@SirPrinter.renderDocComments,
                renderDeclarationOrigins = this@SirPrinter.renderDeclarationOrigins,
                emptyBodyStub = this@SirPrinter.emptyBodyStub,
            )
            listOf(result).asSequence()
        }

        public val cSource: Sequence<String> by lazy {
            val printer = CBridgePrinter()
            bridges.forEach(printer::add)
            printer.print()
        }

        public val kotlinSource: Sequence<String> by lazy {
            val printer = KotlinBridgePrinter()
            bridges.forEach(printer::add)
            printer.print()
        }

        public val hasBridges: Boolean get() = bridges.isNotEmpty()

        private val bridges: List<SirBridge> by lazy {
            collectBridges(module).let {
                if (this@SirPrinter.stableDeclarationsOrder) {
                    it.sortedWith(Comparators.stableBridgeComparator)
                } else {
                    // FIXME: turns out, some tests rely on bridge order to be always stable
                    it.sortedWith(Comparators.stableBridgeComparator)
                }
            }
        }
    }

    public fun print(sirModule: SirModule): Printout = Printout(sirModule)
}

private val fatalErrorBodyStub: SirFunctionBody = SirFunctionBody(
    listOf("fatalError()")
)

private fun collectBridges(container: SirDeclarationContainer): List<SirBridge> = buildList {
    addAll(container.declarations.filterIsInstance<SirBridged>().flatMap { it.bridges })

    addAll(
        container.declarations
            .filterIsInstance<SirVariable>()
            .flatMap { it.accessors.flatMap { it.bridges } }
    )
    addAll(
        container.declarations
            .filterIsInstance<SirSubscript>()
            .flatMap { it.accessors.flatMap { it.bridges } }
    )
    addAll(
        container.declarations
            .filterIsInstance<SirDeclarationContainer>()
            .flatMap { collectBridges(it) }
    )
}.distinct()