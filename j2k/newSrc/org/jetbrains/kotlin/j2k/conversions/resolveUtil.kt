/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPolyVariantReference
import org.jetbrains.kotlin.j2k.ConversionContext
import org.jetbrains.kotlin.j2k.tree.JKTreeElement
import org.jetbrains.kotlin.j2k.tree.impl.psi
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.analysisContext
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.resolve.ImportPath


internal fun resolveFqName(classId: ClassId, contextElement: JKTreeElement, context: ConversionContext): PsiElement? {
    val element = contextElement.psi ?: return null
    return resolveFqName(classId, element)
}

fun resolveFqName(classId: ClassId, element: PsiElement): PsiElement? {
    return constructImportDirectiveWithContext(classId, element)
        .getChildOfType<KtDotQualifiedExpression>()
        ?.selectorExpression
        ?.let {
            it.references.mapNotNull { it.resolve() }.firstOrNull()
        }
}

private fun constructImportDirectiveWithContext(classId: ClassId, element: PsiElement): KtImportDirective {
    val importDirective = KtPsiFactory(element).createImportDirective(ImportPath(classId.asSingleFqName(), false))
    importDirective.containingKtFile.analysisContext = element.containingFile
    return importDirective
}

internal fun multiResolveFqName(classId: ClassId, elementForContext: PsiElement): List<PsiElement> {
    return constructImportDirectiveWithContext(classId, elementForContext)
        .getChildOfType<KtDotQualifiedExpression>()
        ?.selectorExpression
        ?.let {
            it.references.filterIsInstance<PsiPolyVariantReference>().flatMap { it.multiResolve(false).mapNotNull { it.element } }
        }.orEmpty()
}