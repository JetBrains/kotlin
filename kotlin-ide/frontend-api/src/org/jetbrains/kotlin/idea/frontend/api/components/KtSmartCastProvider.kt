/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.components

import org.jetbrains.kotlin.idea.frontend.api.ImplicitReceiverSmartCast
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.psi.KtExpression

abstract class KtSmartCastProvider : KtAnalysisSessionComponent() {
    abstract fun getSmartCastedToType(expression: KtExpression): KtType?
    abstract fun getImplicitReceiverSmartCast(expression: KtExpression): Collection<ImplicitReceiverSmartCast>
}

interface KtSmartCastProviderMixIn : KtAnalysisSessionMixIn {
    fun KtExpression.getSmartCast(): KtType? =
        analysisSession.smartCastProvider.getSmartCastedToType(this)

    fun KtExpression.getImplicitReceiverSmartCast(): Collection<ImplicitReceiverSmartCast> =
        analysisSession.smartCastProvider.getImplicitReceiverSmartCast(this)
}