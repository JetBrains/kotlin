/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.js.resolve.diagnostics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils
import org.jetbrains.kotlin.psi.KtClassLiteralExpression
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.calls.checkers.RttiExpressionChecker
import org.jetbrains.kotlin.resolve.calls.checkers.RttiExpressionInformation
import org.jetbrains.kotlin.resolve.calls.checkers.RttiOperation
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.expressions.ClassLiteralChecker

class JsNativeRttiChecker : RttiExpressionChecker, ClassLiteralChecker {
    override fun check(rttiInformation: RttiExpressionInformation, reportOn: PsiElement, trace: BindingTrace) {
        val sourceType = rttiInformation.sourceType
        val targetType = rttiInformation.targetType
        val targetDescriptor = targetType?.constructor?.declarationDescriptor
        if (sourceType != null && targetDescriptor != null && AnnotationsUtils.isNativeInterface(targetDescriptor)) {
            when (rttiInformation.operation) {
                RttiOperation.IS,
                RttiOperation.NOT_IS -> trace.report(ErrorsJs.CANNOT_CHECK_FOR_NATIVE_INTERFACE.on(reportOn, targetType))

                RttiOperation.AS,
                RttiOperation.SAFE_AS -> trace.report(ErrorsJs.UNCHECKED_CAST_TO_NATIVE_INTERFACE.on(reportOn, sourceType, targetType))
            }
        }
    }

    override fun check(expression: KtClassLiteralExpression, type: KotlinType, context: ResolutionContext<*>) {
        val descriptor = type.constructor.declarationDescriptor as? ClassDescriptor ?: return
        if (AnnotationsUtils.isNativeInterface(descriptor)) {
            context.trace.report(ErrorsJs.NATIVE_INTERFACE_AS_CLASS_LITERAL.on(expression))
        }
    }
}
