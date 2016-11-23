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
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall

class JsReifiedNativeChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        val typeArgumentList = resolvedCall.call.typeArgumentList?.arguments
        for ((typeParam, typeArg) in resolvedCall.typeArguments) {
            if (!typeParam.isReified) continue

            val typeArgDescriptor = typeArg.constructor.declarationDescriptor
            if (typeArgDescriptor != null && AnnotationsUtils.isNativeInterface(typeArgDescriptor)) {
                val typeArgumentPsi = if (typeArgumentList != null) {
                    typeArgumentList[typeParam.index].typeReference
                }
                else {
                    resolvedCall.call.callElement
                }

                context.trace.report(ErrorsJs.NATIVE_INTERFACE_AS_REIFIED_TYPE_ARGUMENT.on(typeArgumentPsi!!, typeArg))
            }
        }
    }
}