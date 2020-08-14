/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PsiJavaPatterns
import com.intellij.util.ProcessingContext
import org.jetbrains.kotlin.idea.frontend.api.getAnalysisSessionFor
import org.jetbrains.kotlin.idea.frontend.api.symbols.*
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtNamedSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtPossibleExtensionSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.isExtension
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtLabelReferenceExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelector

class KotlinFirCompletionContributor : CompletionContributor() {
    init {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), KotlinHighLevelApiContributor)
    }
}

private object KotlinHighLevelApiContributor : CompletionProvider<CompletionParameters>() {
    private val lookupElementFactory = HighLevelApiLookupElementFactory()

    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
        if (shouldSuppressCompletion(parameters, result.prefixMatcher)) return

        val originalFile = parameters.originalFile as? KtFile ?: return

        val reference = (parameters.position.parent as? KtSimpleNameExpression)?.mainReference ?: return
        val nameExpression = reference.expression.takeIf { it !is KtLabelReferenceExpression } ?: return

        val possibleReceiver = nameExpression.getQualifiedExpressionForSelector()?.receiverExpression

        with(getAnalysisSessionFor(originalFile).createContextDependentCopy()) {
            val (implicitScopes, implicitReceivers) = originalFile.getScopeContextForPosition(nameExpression)

            val typeOfPossibleReceiver = possibleReceiver?.getKtType()
            val possibleReceiverScope = typeOfPossibleReceiver?.let { it.getTypeScope() }

            fun addToCompletion(symbol: KtSymbol) {
                if (symbol !is KtNamedSymbol) return
                result.addElement(lookupElementFactory.createLookupElement(symbol))
            }

            if (possibleReceiverScope != null) {
                val nonExtensionMembers = possibleReceiverScope
                    .getCallableSymbols()
                    .filterNot { it.isExtension }

                val extensionNonMembers = implicitScopes
                    .getCallableSymbols()
                    .filter { it.isExtension && it.canBeCalledWith(listOf(typeOfPossibleReceiver)) }

                nonExtensionMembers.forEach(::addToCompletion)
                extensionNonMembers.forEach(::addToCompletion)
            } else {
                val extensionNonMembers = implicitScopes
                    .getCallableSymbols()
                    .filter { !it.isExtension || it.canBeCalledWith(implicitReceivers) }

                extensionNonMembers.forEach(::addToCompletion)

                val availableClasses = implicitScopes.getClassClassLikeSymbols()
                availableClasses.forEach(::addToCompletion)
            }
        }
    }

    private val AFTER_NUMBER_LITERAL = PsiJavaPatterns.psiElement().afterLeafSkipping(
        PsiJavaPatterns.psiElement().withText(""),
        PsiJavaPatterns.psiElement().withElementType(PsiJavaPatterns.elementType().oneOf(KtTokens.FLOAT_LITERAL, KtTokens.INTEGER_LITERAL))
    )
    private val AFTER_INTEGER_LITERAL_AND_DOT = PsiJavaPatterns.psiElement().afterLeafSkipping(
        PsiJavaPatterns.psiElement().withText("."),
        PsiJavaPatterns.psiElement().withElementType(PsiJavaPatterns.elementType().oneOf(KtTokens.INTEGER_LITERAL))
    )

    private fun shouldSuppressCompletion(parameters: CompletionParameters, prefixMatcher: PrefixMatcher): Boolean {
        val position = parameters.position
        val invocationCount = parameters.invocationCount

        // no completion inside number literals
        if (AFTER_NUMBER_LITERAL.accepts(position)) return true

        // no completion auto-popup after integer and dot
        if (invocationCount == 0 && prefixMatcher.prefix.isEmpty() && AFTER_INTEGER_LITERAL_AND_DOT.accepts(position)) return true

        return false
    }

}

private fun KtCallableSymbol.canBeCalledWith(implicitReceivers: List<KtType>): Boolean {
    val requiredReceiverType = (this as? KtPossibleExtensionSymbol)?.receiverType
        ?: error("Extension receiver type should be present on $this")

    return implicitReceivers.any { it.isSubTypeOf(requiredReceiverType) }
}