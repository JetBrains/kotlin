/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.smart

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.builtins.isFunctionOrSuspendFunctionType
import org.jetbrains.kotlin.idea.completion.LambdaSignatureTemplates
import org.jetbrains.kotlin.idea.completion.suppressAutoInsertion
import org.jetbrains.kotlin.idea.core.ExpectedInfo
import org.jetbrains.kotlin.idea.core.fuzzyType
import org.jetbrains.kotlin.resolve.calls.util.getValueParametersCountFromFunctionType
import org.jetbrains.kotlin.types.KotlinType
import java.util.*

object LambdaItems {
    fun collect(functionExpectedInfos: Collection<ExpectedInfo>): Collection<LookupElement> {
        val list = ArrayList<LookupElement>()
        addToCollection(list, functionExpectedInfos)
        return list
    }

    fun addToCollection(collection: MutableCollection<LookupElement>, expectedInfos: Collection<ExpectedInfo>) {
        val functionExpectedInfos = expectedInfos.filter { it.fuzzyType?.type?.isFunctionOrSuspendFunctionType == true }
        if (functionExpectedInfos.isEmpty()) return

        val functionTypes = functionExpectedInfos.mapNotNull { it.fuzzyType?.type }.toSet()

        val singleType = if (functionTypes.size == 1) functionTypes.single() else null
        val singleSignatureLength = singleType?.let(::getValueParametersCountFromFunctionType)
        val offerNoParametersLambda = singleSignatureLength == 0 || singleSignatureLength == 1
        if (offerNoParametersLambda) {
            val lookupElement = LookupElementBuilder.create(LambdaSignatureTemplates.DEFAULT_LAMBDA_PRESENTATION)
                .withInsertHandler(ArtificialElementInsertHandler("{ ", " }", false))
                .suppressAutoInsertion()
                .assignSmartCompletionPriority(SmartCompletionItemPriority.LAMBDA_NO_PARAMS)
                .addTailAndNameSimilarity(functionExpectedInfos)
            collection.add(lookupElement)
        }

        if (singleSignatureLength != 0) {
            for (functionType in functionTypes) {
                if (LambdaSignatureTemplates.explicitParameterTypesRequired(functionTypes, functionType)) {
                    collection.add(
                        createLookupElement(
                            functionType,
                            functionExpectedInfos,
                            LambdaSignatureTemplates.SignaturePresentation.NAMES_OR_TYPES,
                            explicitParameterTypes = true
                        )
                    )

                } else {
                    collection.add(
                        createLookupElement(
                            functionType,
                            functionExpectedInfos,
                            LambdaSignatureTemplates.SignaturePresentation.NAMES_AND_TYPES,
                            explicitParameterTypes = true
                        )
                    )
                    collection.add(
                        createLookupElement(
                            functionType,
                            functionExpectedInfos,
                            LambdaSignatureTemplates.SignaturePresentation.NAMES,
                            explicitParameterTypes = false
                        )
                    )
                }
            }
        }
    }

    private fun createLookupElement(
        functionType: KotlinType,
        functionExpectedInfos: List<ExpectedInfo>,
        signaturePresentation: LambdaSignatureTemplates.SignaturePresentation,
        explicitParameterTypes: Boolean
    ): LookupElement {
        val lookupString = LambdaSignatureTemplates.lambdaPresentation(functionType, signaturePresentation)
        return LookupElementBuilder.create(lookupString)
            .withInsertHandler { context, _ ->
                val offset = context.startOffset
                val placeholder = "{}"
                context.document.replaceString(offset, context.tailOffset, placeholder)
                val placeholderRange = TextRange(offset, offset + placeholder.length)
                LambdaSignatureTemplates.insertTemplate(
                    context,
                    placeholderRange,
                    functionType,
                    explicitParameterTypes,
                    signatureOnly = false
                )
            }
            .suppressAutoInsertion()
            .assignSmartCompletionPriority(SmartCompletionItemPriority.LAMBDA)
            .addTailAndNameSimilarity(functionExpectedInfos.filter { it.fuzzyType?.type == functionType })
    }
}
