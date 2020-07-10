/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext
import org.jetbrains.kotlin.idea.frontend.api.getAnalysisSessionFor
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtNamedSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtPossibleExtensionSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.isExtension
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.idea.references.mainReference
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
    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
        val originalFile = parameters.originalFile as? KtFile ?: return

        val reference = (parameters.position.parent as? KtSimpleNameExpression)?.mainReference ?: return
        val nameExpression = reference.expression.takeIf { it !is KtLabelReferenceExpression } ?: return

        val possibleReceiver = nameExpression.getQualifiedExpressionForSelector()?.receiverExpression

        val originalSession = getAnalysisSessionFor(originalFile)
        val sessionForCompletion = originalSession.analyzeInContext()
        val scopeProvider = sessionForCompletion.scopeProvider

        val (implicitScopes, implicitReceivers) = scopeProvider.getScopeContextForPosition(originalFile, nameExpression)

        val typeOfPossibleReceiver = possibleReceiver?.let { sessionForCompletion.getKtExpressionType(it) }
        val possibleReceiverScope = typeOfPossibleReceiver?.let { sessionForCompletion.scopeProvider.getScopeForType(it) }

        fun addToCompletion(symbol: KtCallableSymbol) {
            if (symbol !is KtNamedSymbol) return
            result.addElement(LookupElementBuilder.create(symbol.name.asString()))
        }

        if (possibleReceiverScope != null) {
            val nonExtensionMembers = possibleReceiverScope
                .getCallableSymbols()
                .filterNot { it.isExtension }
                .toList()

            val extensionNonMembers = implicitScopes
                .getCallableSymbols()
                .filter { it.isExtension && it.canBeCalledWith(listOf(typeOfPossibleReceiver)) }
                .toList()

            nonExtensionMembers.forEach(::addToCompletion)
            extensionNonMembers.forEach(::addToCompletion)
        } else {
            val extensionNonMembers = implicitScopes
                .getCallableSymbols()
                .filter { !it.isExtension || it.canBeCalledWith(implicitReceivers) }

            extensionNonMembers.forEach(::addToCompletion)
        }

        return
    }
}

private fun KtCallableSymbol.canBeCalledWith(implicitReceivers: List<KtType>): Boolean {
    val requiredReceiverType = (this as? KtPossibleExtensionSymbol)?.receiverType
        ?: error("Extension receiver type should be present on ${this}")

    return implicitReceivers.any { it.isSubTypeOf(requiredReceiverType) }
}