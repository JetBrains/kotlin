/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize.ide

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.parcelize.ParcelizeAnnotationChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall

class IdeParcelizeAnnotationChecker : ParcelizeAnnotationChecker() {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        if (!ParcelizeAvailability.isAvailable(resolvedCall.call.callElement)) {
            return
        }

        super.check(resolvedCall, reportOn, context)
    }
}