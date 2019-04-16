package org.jetbrains.kotlin.r4a

import org.jetbrains.kotlin.com.intellij.openapi.extensions.Extensions
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtAnnotatedExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.diagnostics.DiagnosticSuppressor

class R4aDiagnosticSuppressor : DiagnosticSuppressor {

    companion object {
        fun registerExtension(
            @Suppress("UNUSED_PARAMETER") project: Project,
            extension: DiagnosticSuppressor
        ) {
            Extensions.getRootArea().getExtensionPoint(DiagnosticSuppressor.EP_NAME)
                .registerExtension(extension)
        }
    }

    override fun isSuppressed(diagnostic: Diagnostic): Boolean {
        return isSuppressed(diagnostic, null)
    }

    override fun isSuppressed(diagnostic: Diagnostic, bindingContext: BindingContext?): Boolean {
        if (diagnostic.factory == Errors.NON_SOURCE_ANNOTATION_ON_INLINED_LAMBDA_EXPRESSION) {
            for (entry in (
                    diagnostic.psiElement.parent as KtAnnotatedExpression
                    ).annotationEntries) {
                if (bindingContext != null) {
                    val annotation = bindingContext.get(BindingContext.ANNOTATION, entry)
                    if (annotation != null && annotation.isComposableAnnotation) return true
                }
                // Best effort, maybe jetbrains can get rid of nullability.
                else if (entry.shortName?.identifier == "Composable") return true
            }
        }
        return false
    }
}
