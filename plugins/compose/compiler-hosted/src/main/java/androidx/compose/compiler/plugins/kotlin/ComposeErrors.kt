/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.compiler.plugins.kotlin

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory0
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory2
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.types.KotlinType

object ComposeErrors {

    // error goes on the composable call in a non-composable function
    @JvmField
    val COMPOSABLE_INVOCATION =
        DiagnosticFactory0.create<PsiElement>(
            Severity.ERROR
        )

    // error goes on the non-composable function with composable calls
    @JvmField
    val COMPOSABLE_EXPECTED =
        DiagnosticFactory0.create<PsiElement>(
            Severity.ERROR
        )

    @JvmField
    val COMPOSABLE_FUNCTION_REFERENCE =
        DiagnosticFactory0.create<KtCallableReferenceExpression>(
            Severity.ERROR
        )

    @JvmField
    val COMPOSABLE_PROPERTY_BACKING_FIELD =
        DiagnosticFactory0.create<PsiElement>(
            Severity.ERROR
        )

    @JvmField
    val COMPOSABLE_VAR =
        DiagnosticFactory0.create<PsiElement>(
            Severity.ERROR
        )

    @JvmField
    val COMPOSABLE_SUSPEND_FUN =
        DiagnosticFactory0.create<PsiElement>(
            Severity.ERROR
        )

    @JvmField
    val CAPTURED_COMPOSABLE_INVOCATION =
        DiagnosticFactory2.create<PsiElement, DeclarationDescriptor, DeclarationDescriptor>(
            Severity.ERROR
        )

    @JvmField
    val ILLEGAL_ASSIGN_TO_UNIONTYPE =
        DiagnosticFactory2.create<KtExpression, Collection<KotlinType>, Collection<KotlinType>>(
            Severity.ERROR
        )

    @JvmField
    val ILLEGAL_TRY_CATCH_AROUND_COMPOSABLE =
        DiagnosticFactory0.create<PsiElement>(
            Severity.ERROR
        )

    init {
        Errors.Initializer.initializeFactoryNamesAndDefaultErrorMessages(
            ComposeErrors::class.java,
            ComposeErrorMessages()
        )
    }
}