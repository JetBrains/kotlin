/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.resolve.diagnostics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall

object JsExternalEnumChecker : CallChecker {
    private val forbiddenEnumApi = setOf("values", "valueOf", "name", "ordinal", "hashCode", "compareTo")

    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        val callee = resolvedCall.resultingDescriptor
        if (!AnnotationsUtils.isNativeObject(callee)) return

        (callee.containingDeclaration as? ClassDescriptor)?.let {
            if (it.kind == ClassKind.ENUM_CLASS && callee.name.asString() in forbiddenEnumApi) {
                context.trace.report(ErrorsJs.WRONG_OPERATION_WITH_EXTERNAL_ENUM.on(reportOn))
            }
        }
    }
}