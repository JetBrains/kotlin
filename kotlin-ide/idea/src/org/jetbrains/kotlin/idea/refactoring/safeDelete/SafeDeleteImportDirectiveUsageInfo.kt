/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.safeDelete

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMember
import com.intellij.refactoring.safeDelete.usageInfo.SafeDeleteReferenceSimpleDeleteUsageInfo
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.caches.resolve.util.getJavaMemberDescriptor
import org.jetbrains.kotlin.idea.core.targetDescriptors
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class SafeDeleteImportDirectiveUsageInfo(
    importDirective: KtImportDirective, declaration: PsiElement
) : SafeDeleteReferenceSimpleDeleteUsageInfo(importDirective, declaration, importDirective.isSafeToDelete(declaration))

private fun KtImportDirective.isSafeToDelete(element: PsiElement): Boolean {
    val referencedDescriptor = targetDescriptors().singleOrNull() ?: return false
    val unwrappedElement = element.unwrapped
    val declarationDescriptor = when (unwrappedElement) {
        is KtDeclaration -> unwrappedElement.resolveToDescriptorIfAny(BodyResolveMode.FULL)
        is PsiMember -> unwrappedElement.getJavaMemberDescriptor()
        else -> return false
    }
    return referencedDescriptor == declarationDescriptor
}
