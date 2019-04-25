package org.jetbrains.kotlin.r4a

import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtAnnotatedExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTraceContext
import org.jetbrains.kotlin.resolve.TemporaryBindingTrace
import org.jetbrains.kotlin.resolve.calls.callUtil.getCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.context.TemporaryTraceAndCache
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
        if(diagnostic.factory == Errors.NAMED_ARGUMENTS_NOT_ALLOWED) {
            val functionCall = diagnostic.psiElement.parent.parent.parent.parent as KtExpression
            if(bindingContext != null) {
                val call = (diagnostic.psiElement.parent.parent.parent.parent as KtCallExpression).getCall(bindingContext).getResolvedCall(bindingContext)
                val temporaryTrace = TemporaryBindingTrace.create(BindingTraceContext.createTraceableBindingTrace(), "trace to resolve ktx call", functionCall)
                if(call != null) return ComposableAnnotationChecker.get(diagnostic.psiElement.project).shouldInvokeAsTag(temporaryTrace, call)
                return false;
            }
        }
        return false
    }
}
