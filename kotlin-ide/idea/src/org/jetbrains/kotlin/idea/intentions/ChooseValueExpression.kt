/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.template.Expression
import com.intellij.codeInsight.template.ExpressionContext
import com.intellij.codeInsight.template.TextResult
import com.intellij.codeInsight.template.impl.TemplateManagerImpl
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil

//TODO: move it somewhere else and reuse
abstract class ChooseValueExpression<in T : Any>(
    lookupItems: Collection<T>,
    defaultItem: T,
    private val advertisementText: String? = null
) : Expression() {
    protected abstract fun getLookupString(element: T): String
    protected abstract fun getResult(element: T): String

    @Suppress("LeakingThis")
    private val defaultItemString = getLookupString(defaultItem)

    private val lookupItems: Array<LookupElement> = lookupItems.map { suggestion ->
        LookupElementBuilder.create(suggestion, getLookupString(suggestion)).withInsertHandler { context, item ->
            val topLevelEditor = InjectedLanguageUtil.getTopLevelEditor(context.editor)
            val templateState = TemplateManagerImpl.getTemplateState(topLevelEditor)
            if (templateState != null) {
                val range = templateState.currentVariableRange
                if (range != null) {
                    @Suppress("UNCHECKED_CAST")
                    topLevelEditor.document.replaceString(range.startOffset, range.endOffset, getResult(item.`object` as T))
                }
            }
        }
    }.toTypedArray()

    override fun calculateLookupItems(context: ExpressionContext) = if (lookupItems.size > 1) lookupItems else null

    override fun calculateQuickResult(context: ExpressionContext) = calculateResult(context)

    override fun calculateResult(context: ExpressionContext) = TextResult(defaultItemString)

    override fun getAdvertisingText() = advertisementText
}

class ChooseStringExpression(
    suggestions: Collection<String>,
    default: String = suggestions.first(),
    advertisementText: String? = null
) : ChooseValueExpression<String>(suggestions, default, advertisementText) {
    override fun getLookupString(element: String) = element
    override fun getResult(element: String) = element
}
