package org.jetbrains.kotlin.r4a

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory0
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory2
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory3
import org.jetbrains.kotlin.diagnostics.reportFromPlugin
import org.jetbrains.kotlin.r4a.analysis.R4ADefaultErrorMessages
import org.jetbrains.kotlin.types.expressions.ExpressionTypingContext

fun <E : PsiElement> DiagnosticFactory0<E>.report(
    context: ExpressionTypingContext,
    elements: Collection<E>
) {
    elements.forEach {
        context.trace.reportFromPlugin(
            on(it),
            R4ADefaultErrorMessages
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
            R4ADefaultErrorMessages
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
            R4ADefaultErrorMessages
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
            R4ADefaultErrorMessages
        )
    }
}