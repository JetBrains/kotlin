/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("ScopeUtils")

package org.jetbrains.kotlin.idea.util

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ClassDescriptorWithResolutionScopes
import org.jetbrains.kotlin.idea.FrontendInternals
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.resolve.frontendService
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.FileScopeProvider
import org.jetbrains.kotlin.resolve.scopes.LexicalScope

fun PsiElement.getResolutionScope(bindingContext: BindingContext): LexicalScope? {
    for (parent in parentsWithSelf) {
        if (parent is KtElement) {
            val scope = bindingContext[BindingContext.LEXICAL_SCOPE, parent]
            if (scope != null) return scope
        }

        if (parent is KtClassBody) {
            val classDescriptor = bindingContext[BindingContext.CLASS, parent.getParent()] as? ClassDescriptorWithResolutionScopes
            if (classDescriptor != null) {
                return classDescriptor.scopeForMemberDeclarationResolution
            }
        }
        if (parent is KtFile) {
            break
        }
    }

    return null
}

fun PsiElement.getResolutionScope(
    bindingContext: BindingContext,
    resolutionFacade: ResolutionFacade/*TODO: get rid of this parameter*/
): LexicalScope = getResolutionScope(bindingContext) ?: when (containingFile) {
    is KtFile -> resolutionFacade.getFileResolutionScope(containingFile as KtFile)
    else -> error("Not in KtFile")
}

@OptIn(FrontendInternals::class)
fun ResolutionFacade.getFileResolutionScope(file: KtFile): LexicalScope {
    return frontendService<FileScopeProvider>().getFileResolutionScope(file)
}