/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.Project
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.ProcessingContext
import org.jetbrains.kotlin.idea.frontend.api.getAnalysisSessionFor
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtNamedSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtPossibleExtensionSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.isExtension
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.stubindex.KotlinTopLevelFunctionByPackageIndex
import org.jetbrains.kotlin.idea.stubindex.KotlinTopLevelPropertyByPackageIndex
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtCallableDeclaration
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
    require(this is KtPossibleExtensionSymbol && this.isExtension) { "This function should be called only on extensions!" }

    val requiredReceiverType = receiverType ?: error("Receiver type should be present")
    return implicitReceivers.any { it.isSubTypeOf(requiredReceiverType) }
}

class PackageIndexHelper(private val project: Project) {
    //todo use more concrete scope
    private val searchScope = GlobalSearchScope.allScope(project)

    private val functionByPackageIndex = KotlinTopLevelFunctionByPackageIndex.getInstance()
    private val propertyByPackageIndex = KotlinTopLevelPropertyByPackageIndex.getInstance()

    fun getPackageTopLevelNames(packageFqName: FqName): Set<Name> {
        return getTopLevelCallables(packageFqName).mapTo(mutableSetOf()) { it.nameAsSafeName }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun getTopLevelCallables(packageFqName: FqName): List<KtCallableDeclaration> = buildList {
        addAll(functionByPackageIndex.get(packageFqName.asString(), project, searchScope))
        addAll(propertyByPackageIndex.get(packageFqName.asString(), project, searchScope))
    }
}
