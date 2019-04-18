/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.nullabilityAnalysis

import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.isNullExpression
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValue
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactoryImpl
import org.jetbrains.kotlin.resolve.jvm.checkers.mustNotBeNull
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.util.javaslang.getOrNull

internal inline fun KtExpression.deepestReceiver(): KtExpression =
    generateSequence(this) {
        if (it is KtQualifiedExpression) it.receiverExpression else null
    }.last()

internal inline fun KtExpression.isNullable(): Boolean =
    getType(analyze())?.isNullable() != false

internal inline fun KtExpression.getForcedNullability(): Nullability? {
    val bindingContext = analyze()
    val type = this.getType(bindingContext) ?: return null
    if (!type.isNullable()) return Nullability.NOT_NULL

    //TODO better way of getting DataFlowValueFactoryImpl
    val dataFlowValue = DataFlowValueFactoryImpl(LanguageVersionSettingsImpl.DEFAULT)
        .createDataFlowValue(
            this,
            type,
            bindingContext,
            getResolutionFacade().moduleDescriptor
        )
    val dataFlowInfo = analyze()[BindingContext.EXPRESSION_TYPE_INFO, this]?.dataFlowInfo ?: return null
    return when {
        dataFlowInfo.completeNullabilityInfo.get(dataFlowValue)?.getOrNull() ==
                org.jetbrains.kotlin.resolve.calls.smartcasts.Nullability.NOT_NULL -> Nullability.NOT_NULL
        type.isExternallyAnnotatedNotNull(dataFlowInfo, dataFlowValue) -> Nullability.NOT_NULL
        else -> null
    }
}


private fun KotlinType.isExternallyAnnotatedNotNull(dataFlowInfo: DataFlowInfo, dataFlowValue: DataFlowValue): Boolean=
    mustNotBeNull()?.isFromJava == true && dataFlowInfo.getStableNullability(dataFlowValue).canBeNull()

private fun IElementType.isEqualsToken() =
    this == KtTokens.EQEQ
            || this == KtTokens.EXCLEQ
            || this == KtTokens.EQEQEQ
            || this == KtTokens.EXCLEQEQEQ

fun KtBinaryExpression.isComaprationWithNull() =
    operationToken.isEqualsToken() &&
            (left.isNullExpression() || right.isNullExpression())

fun KtExpression.isLiteral() =
    this is KtStringTemplateExpression
            || this is KtLiteralStringTemplateEntry
            || this is KtConstantExpression
            || isNullExpression()