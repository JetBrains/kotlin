/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.parameterInfo

import com.intellij.codeInsight.hints.InlayInfo
import org.jetbrains.kotlin.backend.common.descriptors.isSuspend
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.psiUtil.startOffset

fun provideSuspendingCallHint(callExpression: KtCallExpression): InlayInfo? {
    val resolvedCall = callExpression.resolveToCall() ?: return null
    if (resolvedCall.candidateDescriptor.isSuspend) {
        return InlayInfo(TYPE_INFO_PREFIX + "#", callExpression.calleeExpression?.startOffset ?: callExpression.startOffset)
    }
    return null
}