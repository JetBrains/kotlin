/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.konan.diagnostics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.descriptors.konan.*

class NativeReifiedForwardDeclarationChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        val typeArgumentList = resolvedCall.call.typeArgumentList?.arguments
        for ((typeParam, typeArg) in resolvedCall.typeArguments) {
            if (!typeParam.isReified) continue

            val typeArgDescriptor = typeArg.constructor.declarationDescriptor
            if (typeArgDescriptor is ClassDescriptor && typeArgDescriptor.getForwardDeclarationKindOrNull() != null) {
                val typeArgumentPsi = if (typeArgumentList != null) {
                    typeArgumentList[typeParam.index].typeReference
                } else {
                    resolvedCall.call.calleeExpression
                }

                context.trace.report(ErrorsNative.FORWARD_DECLARATION_AS_REIFIED_TYPE_ARGUMENT.on(typeArgumentPsi!!, typeArg))
            }
        }
    }
}