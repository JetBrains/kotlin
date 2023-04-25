/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.resolve.diagnostics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.util.getParameterForArgument
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyExternal
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable

object JsExternalArgumentCallChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        for (argument in resolvedCall.call.valueArguments) {
            val parameter = resolvedCall.getParameterForArgument(argument) ?: continue
            if (AnnotationsUtils.isJsExternalArgument(parameter)) {
                val argExpression = argument.getArgumentExpression() ?: continue
                val argumentType = context.trace.bindingContext.getType(argExpression) ?: continue
                val declaration = argumentType.makeNotNullable().constructor.declarationDescriptor as? ClassDescriptor ?: continue
                if (!declaration.isEffectivelyExternal()) {
                    context.trace.report(ErrorsJs.JS_EXTERNAL_ARGUMENT.on(argExpression, argumentType))
                }
            }
        }
    }
}
