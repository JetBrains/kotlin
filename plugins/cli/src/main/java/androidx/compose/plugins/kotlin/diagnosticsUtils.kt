/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.plugins.kotlin

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory0
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory2
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory3
import org.jetbrains.kotlin.diagnostics.reportFromPlugin
import androidx.compose.plugins.kotlin.analysis.ComposeDefaultErrorMessages
import org.jetbrains.kotlin.types.expressions.ExpressionTypingContext

fun <E : PsiElement> DiagnosticFactory0<E>.report(
    context: ExpressionTypingContext,
    elements: Collection<E>
) {
    elements.forEach {
        context.trace.reportFromPlugin(
            on(it),
            ComposeDefaultErrorMessages
        )
    }
}

fun <E : PsiElement, T1> DiagnosticFactory1<E, T1>.report(
    context: ExpressionTypingContext,
    elements: Collection<E>,
    value1: T1
) {
    elements.forEach {
        context.trace.reportFromPlugin(
            on(it, value1),
            ComposeDefaultErrorMessages
        )
    }
}

fun <E : PsiElement, T1, T2> DiagnosticFactory2<E, T1, T2>.report(
    context: ExpressionTypingContext,
    elements: Collection<E>,
    value1: T1,
    value2: T2
) {
    elements.forEach {
        context.trace.reportFromPlugin(
            on(it, value1, value2),
            ComposeDefaultErrorMessages
        )
    }
}

fun <E : PsiElement, T1, T2, T3> DiagnosticFactory3<E, T1, T2, T3>.report(
    context: ExpressionTypingContext,
    elements: Collection<E>,
    value1: T1,
    value2: T2,
    value3: T3
) {
    elements.forEach {
        context.trace.reportFromPlugin(
            on(it, value1, value2, value3),
            ComposeDefaultErrorMessages
        )
    }
}