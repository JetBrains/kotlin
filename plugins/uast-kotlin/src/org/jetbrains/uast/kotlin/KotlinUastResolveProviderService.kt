/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.kotlin

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.constants.UnsignedErrorValueTypeConstant
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression

interface KotlinUastResolveProviderService : BaseKotlinUastResolveProviderService {
    fun getBindingContext(element: KtElement): BindingContext
    fun getTypeMapper(element: KtElement): KotlinTypeMapper?
    fun getLanguageVersionSettings(element: KtElement): LanguageVersionSettings
    fun getReferenceVariants(ktElement: KtElement, nameHint: String): Sequence<DeclarationDescriptor>

    override val baseKotlinConverter: BaseKotlinConverter
        get() = KotlinConverter

    override fun convertParent(uElement: UElement): UElement? {
        return convertParentImpl(uElement)
    }

    override fun resolveToDeclaration(ktExpression: KtExpression): PsiElement? {
        return resolveToDeclarationImpl(ktExpression)
    }

    override fun resolveToType(ktTypeReference: KtTypeReference, source: UElement): PsiType? {
        return ktTypeReference.toPsiType(source)
    }

    override fun getExpressionType(uExpression: UExpression): PsiType? {
        val ktElement = uExpression.sourcePsi as? KtExpression ?: return null
        val ktType = ktElement.analyze()[BindingContext.EXPRESSION_TYPE_INFO, ktElement]?.type ?: return null
        return ktType.toPsiType(uExpression, ktElement, boxed = false)
    }

    override fun evaluate(uExpression: UExpression): Any? {
        val ktElement = uExpression.sourcePsi as? KtExpression ?: return null
        val compileTimeConst = ktElement.analyze()[BindingContext.COMPILE_TIME_VALUE, ktElement]
        if (compileTimeConst is UnsignedErrorValueTypeConstant) return null
        return compileTimeConst?.getValue(TypeUtils.NO_EXPECTED_TYPE)
    }
}
