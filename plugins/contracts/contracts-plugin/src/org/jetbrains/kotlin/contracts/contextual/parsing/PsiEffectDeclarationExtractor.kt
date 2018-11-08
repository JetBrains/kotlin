/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.contextual.parsing

import org.jetbrains.kotlin.contracts.contextual.model.CleanerDeclaration
import org.jetbrains.kotlin.contracts.contextual.model.ProviderDeclaration
import org.jetbrains.kotlin.contracts.contextual.model.VerifierDeclaration
import org.jetbrains.kotlin.contracts.parsing.PsiContractVariableParserDispatcher
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

abstract class PsiEffectDeclarationExtractor(val context: BindingContext, val dispatcher: PsiContractVariableParserDispatcher) {
    abstract fun extractDeclarations(declaration: KtExpression, dslFunctionName: Name): ContextDeclarations

    internal fun extractDeclarationsOrNull(declaration: KtExpression, dslFunctionName: Name): ContextDeclarations? {
        val declarations = extractDeclarations(declaration, dslFunctionName)
        return if (declarations.isEmpty()) null else declarations
    }

    protected fun CallableDescriptor.extractConstructorName() =
        (this as? ClassConstructorDescriptor)?.constructedClass?.name?.asString()

    protected fun KtExpression.getResolverCallAndResultingDescriptor(): Pair<ResolvedCall<*>, CallableDescriptor>? {
        val resolvedCall = getResolvedCall(this@PsiEffectDeclarationExtractor.context) ?: return null
        val descriptor = resolvedCall.resultingDescriptor
        return resolvedCall to descriptor
    }

    protected fun ResolvedCall<*>.firstArgumentAsExpressionOrNull(): KtExpression? =
        this.valueArgumentsByIndex?.firstOrNull()?.safeAs<ExpressionValueArgument>()?.valueArgument?.getArgumentExpression()

    protected fun ResolvedCall<*>.argumentAsExpressionOrNull(index: Int): KtExpression? =
        this.valueArgumentsByIndex?.getOrNull(index)?.safeAs<ExpressionValueArgument>()?.valueArgument?.getArgumentExpression()
}

data class ContextDeclarations(
    val provider: ProviderDeclaration? = null,
    val verifier: VerifierDeclaration? = null,
    val cleaner: CleanerDeclaration? = null
) {
    internal fun isEmpty() = provider == null && verifier == null && cleaner == null
}