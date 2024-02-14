/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.passes

import org.jetbrains.kotlin.analysis.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.visitors.SirVisitorVoid
import org.jetbrains.sir.passes.builder.KotlinSource


public class SirDiscoverDeclarationsPass : SirModulePass {
    private class TypePatcher(
        private val declarationLookup: Map<String, SirNamedDeclaration>
    ) : SirVisitorVoid() {

        override fun visitElement(element: SirElement) {
            element.acceptChildren(this)
        }

        override fun visitFunction(function: SirFunction) {
            val typesToPatch = listOf(function.returnType) + function.parameters.map { it.type }
            typesToPatch.forEach { patchType(it) }
        }

        override fun visitVariable(variable: SirVariable) {
            patchType(variable.type)
        }

        private fun patchType(type: SirType) {
            val nominalType = type as? SirNominalType
                ?: return
            val typeToPatch = nominalType.declRef as? UnknownDeclaration
                ?: return

            nominalType.declRef = DiscoveredDeclaration(typeToPatch.findNamedDeclaration())
        }

        // currently, I assume that SirBuilder received all the sources and that that sources are full
        // For the use case when we should build SIR from single PSI(IDE usage) - we need to implement lookup
        // through Analysis Api for missing declarations.
        private fun UnknownDeclaration.findNamedDeclaration(): SirNamedDeclaration {
            val origin = (origin as? KotlinSource)
                ?: throw IllegalArgumentException("received not a KotlinSource during SirDiscoverDeclarationsPass")
            val symbol = origin.symbol as? KtNamedClassOrObjectSymbol
                ?: throw IllegalArgumentException("received not a ClassORObjectSymbol during type patching")

            return declarationLookup[symbol.name.asString()]
                ?: throw IllegalStateException("SirDeclaration for ${symbol.name.asString()} is not found.")
        }
    }

    public override fun run(element: SirModule, data: Nothing?): SirModule {
        val declarations = element
            .declarations.filterIsInstance<SirNamedDeclaration>()
            .associateBy { it.name }

        element.accept(TypePatcher(declarations))

        return element
    }
}
