/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import com.intellij.openapi.paths.GlobalPathReferenceProvider
import com.intellij.openapi.paths.WebReference
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiReference
import org.jetbrains.kotlin.idea.kdoc.KDocReferenceDescriptorsImpl
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.parents

class KotlinReferenceContributor : KotlinReferenceProviderContributor {
    override fun registerReferenceProviders(registrar: KotlinPsiReferenceRegistrar) {
        with(registrar) {
            registerProvider(factory = ::KtSimpleNameReferenceDescriptorsImpl)

            registerMultiProvider<KtNameReferenceExpression> { nameReferenceExpression ->
                if (nameReferenceExpression.getReferencedNameElementType() != KtTokens.IDENTIFIER) return@registerMultiProvider emptyArray()
                if (nameReferenceExpression.parents.any { it is KtImportDirective || it is KtPackageDirective || it is KtUserType }) {
                    return@registerMultiProvider emptyArray()
                }

                when (nameReferenceExpression.readWriteAccess(useResolveForReadWrite = false)) {
                    ReferenceAccess.READ ->
                        arrayOf(SyntheticPropertyAccessorReferenceDescriptorImpl(nameReferenceExpression, getter = true))
                    ReferenceAccess.WRITE ->
                        arrayOf(SyntheticPropertyAccessorReferenceDescriptorImpl(nameReferenceExpression, getter = false))
                    ReferenceAccess.READ_WRITE ->
                        arrayOf(
                            SyntheticPropertyAccessorReferenceDescriptorImpl(nameReferenceExpression, getter = true),
                            SyntheticPropertyAccessorReferenceDescriptorImpl(nameReferenceExpression, getter = false)
                        )
                }
            }

            registerProvider(factory = ::KtConstructorDelegationReferenceDescriptorsImpl)

            registerProvider(factory = ::KtInvokeFunctionReferenceDescriptorsImpl)

            registerProvider(factory = ::KtArrayAccessReferenceDescriptorsImpl)

            registerProvider(factory = ::KtCollectionLiteralReferenceDescriptorsImpl)

            registerProvider(factory = ::KtForLoopInReferenceDescriptorsImpl)

            registerProvider(factory = ::KtPropertyDelegationMethodsReferenceDescriptorsImpl)

            registerProvider(factory = ::KtDestructuringDeclarationReferenceDescriptorsImpl)

            registerProvider(factory = ::KDocReferenceDescriptorsImpl)

            registerProvider(KotlinDefaultAnnotationMethodImplicitReferenceProvider)

            registerMultiProvider<KtStringTemplateEntry> { stringTemplateEntry ->
                val texts = stringTemplateEntry.text.split(Regex("\\s"))
                val results = mutableListOf<PsiReference>()
                var startIndex = 0
                texts.forEach { text ->
                    if (GlobalPathReferenceProvider.isWebReferenceUrl(text)) {
                        results.add(WebReference(stringTemplateEntry, TextRange(startIndex, startIndex + text.length), text))
                    }
                    startIndex += text.length + 1
                }
                results.toTypedArray()
            }
        }
    }
}
