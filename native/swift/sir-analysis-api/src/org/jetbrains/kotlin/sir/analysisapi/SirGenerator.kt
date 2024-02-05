/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.analysisapi

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.isPublic
import org.jetbrains.kotlin.sir.SirDeclaration
import org.jetbrains.kotlin.sir.SirForeignFunction
import org.jetbrains.kotlin.sir.analysisapi.transformers.toForeignFunction
import org.jetbrains.kotlin.sir.analysisapi.transformers.toForeignVariable

/**
 * A root interface for classes that produce Swift IR elements.
 */
public interface SirFactory {
    public fun build(fromFile: KtFile): List<SirDeclaration>
}

public class SirGenerator : SirFactory {
    override fun build(fromFile: KtFile): List<SirDeclaration> = analyze(fromFile) {
        val res = mutableListOf<SirDeclaration>()
        fromFile.accept(Visitor(res))
        return res.toList()
    }

    private class Visitor(val res: MutableList<SirDeclaration>) : KtTreeVisitorVoid() {
        override fun visitNamedFunction(function: KtNamedFunction) {
            super.visitNamedFunction(function)
            function.process(KtNamedFunction::toForeignFunction)
        }

        override fun visitProperty(property: KtProperty) {
            super.visitProperty(property)
            property.process(KtProperty::toForeignVariable)
        }

        private inline fun <T : KtDeclaration> T.process(converter: T.() -> SirDeclaration) {
            this.takeIf { it.isPublic }
                ?.let(converter)
                ?.let { res.add(it) }
        }
    }
}
