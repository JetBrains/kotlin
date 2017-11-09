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
import org.jetbrains.kotlin.js.resolve.diagnostics.JsExternalChecker.DEFINED_EXTERNALLY_PROPERTY_NAMES
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe

object JsDefinedExternallyCallChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        if (resolvedCall.resultingDescriptor.fqNameUnsafe !in DEFINED_EXTERNALLY_PROPERTY_NAMES) return

        val ownerDescriptor = context.scope.ownerDescriptor
        if (!AnnotationsUtils.isNativeObject(ownerDescriptor) && !AnnotationsUtils.isPredefinedObject(ownerDescriptor)) {
            context.trace.report(ErrorsJs.CALL_TO_DEFINED_EXTERNALLY_FROM_NON_EXTERNAL_DECLARATION.on(reportOn))
        }
    }
}
