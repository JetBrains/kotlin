/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.compiler.plugins.kotlin.k1

import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtAnnotatedExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.util.getCall
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.resolve.diagnostics.DiagnosticSuppressor

open class ComposeDiagnosticSuppressor : DiagnosticSuppressor {
    override fun isSuppressed(diagnostic: Diagnostic): Boolean {
        return isSuppressed(diagnostic, null)
    }

    override fun isSuppressed(diagnostic: Diagnostic, bindingContext: BindingContext?): Boolean {
        if (diagnostic.factory == Errors.NON_SOURCE_ANNOTATION_ON_INLINED_LAMBDA_EXPRESSION) {
            for (
                entry in (
                    diagnostic.psiElement.parent as KtAnnotatedExpression
                    ).annotationEntries
            ) {
                if (bindingContext != null) {
                    val annotation = bindingContext.get(BindingContext.ANNOTATION, entry)
                    if (annotation != null && annotation.isComposableAnnotation) return true
                }
                // Best effort, maybe jetbrains can get rid of nullability.
                else if (entry.shortName?.identifier == "Composable") return true
            }
        }
        if (diagnostic.factory == Errors.NAMED_ARGUMENTS_NOT_ALLOWED) {
            if (bindingContext != null) {
                val call = (diagnostic.psiElement.parent.parent.parent.parent as KtCallExpression)
                    .getCall(bindingContext).getResolvedCall(bindingContext)
                if (call != null) {
                    return call.isComposableInvocation()
                }
            }
        }
        return false
    }
}
