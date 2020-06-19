/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.highlighter

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.Annotation
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.util.TextRange
import com.intellij.xml.util.XmlStringUtil
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages

object Diagnostic2Annotation {
    fun getHtmlMessage(diagnostic: Diagnostic, renderMessage: (Diagnostic) -> String): String {
        var message = renderMessage(diagnostic)
        if (ApplicationManager.getApplication().isInternal || ApplicationManager.getApplication().isUnitTestMode) {
            val factoryName = diagnostic.factory.name
            message = if (message.startsWith("<html>")) {
                "<html>[$factoryName] ${message.substring("<html>".length)}"
            } else {
                "[$factoryName] $message"
            }
        }
        if (!message.startsWith("<html>")) {
            message = "<html><body>${XmlStringUtil.escapeString(message)}</body></html>"
        }
        return message
    }

    fun getDefaultMessage(diagnostic: Diagnostic): String {
        val message = DefaultErrorMessages.render(diagnostic)
        if (ApplicationManager.getApplication().isInternal || ApplicationManager.getApplication().isUnitTestMode) {
            return "[${diagnostic.factory.name}] $message"
        }
        return message
    }

    fun createAnnotation(
        diagnostic: Diagnostic,
        range: TextRange,
        holder: AnnotationHolder,
        nonDefaultMessage: String?,
        textAttributes: TextAttributesKey?,
        highlightType: ProblemHighlightType?,
        renderMessage: (Diagnostic) -> String
    ): Annotation {
        val defaultMessage = nonDefaultMessage ?: getDefaultMessage(diagnostic)

        val annotation = when (diagnostic.severity) {
            Severity.ERROR -> holder.createErrorAnnotation(range, defaultMessage)
            Severity.WARNING -> {
                if (highlightType == ProblemHighlightType.WEAK_WARNING) {
                    holder.createWeakWarningAnnotation(range, defaultMessage)
                } else {
                    holder.createWarningAnnotation(range, defaultMessage)
                }
            }
            Severity.INFO -> holder.createInfoAnnotation(range, defaultMessage)
        }

        annotation.tooltip = getHtmlMessage(diagnostic, renderMessage)

        if (highlightType != null) {
            annotation.highlightType = highlightType
        }

        if (textAttributes != null) {
            annotation.textAttributes = textAttributes
        }

        return annotation
    }
}