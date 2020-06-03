/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.highlighter.visitors

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.psi.util.PsiUtilCore
import org.jetbrains.kotlin.idea.fir.highlighter.textAttributesForClass
import org.jetbrains.kotlin.idea.fir.highlighter.textAttributesForKtParameterDeclaration
import org.jetbrains.kotlin.idea.fir.highlighter.textAttributesForKtPropertyDeclaration
import org.jetbrains.kotlin.idea.highlighter.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.idea.highlighter.KotlinHighlightingColors as Colors

internal class DeclarationHighlightingVisitor(holder: AnnotationHolder) : HighlightingVisitor(holder) {
    override fun visitTypeAlias(typeAlias: KtTypeAlias) {
        highlightNamedDeclaration(typeAlias, Colors.TYPE_ALIAS)
        super.visitTypeAlias(typeAlias)
    }

    override fun visitObjectDeclaration(declaration: KtObjectDeclaration) {
        highlightNamedDeclaration(declaration, Colors.OBJECT)
        super.visitObjectDeclaration(declaration)
    }

    override fun visitClass(klass: KtClass) {
        highlightNamedDeclaration(klass, textAttributesForClass(klass))
        super.visitClass(klass)
    }

    override fun visitProperty(property: KtProperty) {
        textAttributesForKtPropertyDeclaration(property)?.let { attributes ->
            highlightNamedDeclaration(property, attributes)
        }
        highlightMutability(property)
        super.visitProperty(property)
    }

    override fun visitParameter(parameter: KtParameter) {
        textAttributesForKtParameterDeclaration(parameter)?.let { attributes ->
            highlightNamedDeclaration(parameter, attributes)
        }
        highlightMutability(parameter)
        super.visitParameter(parameter)
    }


    private fun <D> highlightMutability(declaration: D) where D : KtValVarKeywordOwner, D : KtNamedDeclaration {
        if (PsiUtilCore.getElementType(declaration.valOrVarKeyword) == KtTokens.VAR_KEYWORD) {
            highlightNamedDeclaration(declaration, Colors.MUTABLE_VARIABLE)
        }
    }
}

class DeclarationHighlightingExtension : BeforeResolveHighlightingExtension {
    override fun createVisitor(holder: AnnotationHolder): HighlightingVisitor =
        DeclarationHighlightingVisitor(holder)
}