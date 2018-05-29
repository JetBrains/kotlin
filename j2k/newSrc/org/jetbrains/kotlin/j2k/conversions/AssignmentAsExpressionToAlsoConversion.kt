/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.j2k.ConversionContext
import org.jetbrains.kotlin.j2k.tree.JKJavaAssignmentExpression
import org.jetbrains.kotlin.j2k.tree.JKTreeElement
import org.jetbrains.kotlin.j2k.tree.impl.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.analysisContext
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.resolve.ImportPath

class AssignmentAsExpressionToAlsoConversion(val context: ConversionContext) : RecursiveApplicableConversionBase() {

    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKJavaAssignmentExpression) return recurse(element)

        // TODO: Check if inside variable initializer
        //if (element.parent)

        val alsoElement = resolveFqName(ClassId.fromString("kotlin/also"), element) ?: return recurse(element)
        val alsoSymbol = context.symbolProvider.provideSymbol(alsoElement) as? JKMethodSymbol ?: return recurse(element)
        element.invalidate()

        return JKQualifiedExpressionImpl(
            element.rExpression,
            JKKtQualifierImpl.DOT,
            JKKtCallExpressionImpl(alsoSymbol, JKExpressionListImpl())
        )
    }

    private fun resolveFqName(classId: ClassId, contextElement: JKTreeElement): PsiElement? {
        val element = context.backAnnotator(contextElement) ?: return null
        val importDirective = KtPsiFactory(element).createImportDirective(ImportPath(classId.asSingleFqName(), false))
        importDirective.containingKtFile.analysisContext = element.containingFile
        return importDirective.getChildOfType<KtDotQualifiedExpression>()
            ?.selectorExpression
            ?.let {
                it.references.mapNotNull { it.resolve() }.first()
            }
    }

}