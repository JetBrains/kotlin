/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeMetaInfo.renderConfigurations

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.kotlin.checkers.diagnostics.factories.DebugInfoDiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.rendering.*
import org.jetbrains.kotlin.idea.codeMetaInfo.models.DiagnosticCodeMetaInfo
import org.jetbrains.kotlin.idea.codeMetaInfo.models.HighlightingCodeMetaInfo
import org.jetbrains.kotlin.idea.codeMetaInfo.models.ICodeMetaInfo
import org.jetbrains.kotlin.idea.codeMetaInfo.models.LineMarkerCodeMetaInfo


abstract class AbstractCodeMetaInfoRenderConfiguration(var renderParams: Boolean = true) {

    abstract fun asString(codeMetaInfo: ICodeMetaInfo): String

    open fun getAdditionalTags(codeMetaInfo: ICodeMetaInfo) = ""

    protected fun sanitizeLineMarkerTooltip(originalText: String?): String {
        if (originalText == null) return "null"
        val noHtmlTags = StringUtil.removeHtmlTags(originalText)
        return sanitizeLineBreaks(noHtmlTags)
    }

    protected fun sanitizeLineBreaks(originalText: String): String {
        return StringUtil.replace(originalText, "\n", " ")
    }
}

open class DiagnosticCodeMetaInfoRenderConfiguration(
    val withNewInference: Boolean = true,
    val renderSeverity: Boolean = false
) : AbstractCodeMetaInfoRenderConfiguration() {

    override fun asString(codeMetaInfo: ICodeMetaInfo): String {
        if (codeMetaInfo !is DiagnosticCodeMetaInfo) return ""
        return (getTag(codeMetaInfo) + if (renderParams) "(\"${getParamsString(codeMetaInfo)}\")" else "")
            .replace(Regex("""\r?\n"""), "")
    }

    private fun getParamsString(codeMetaInfo: DiagnosticCodeMetaInfo): String {
        val params = mutableListOf<String>()
        val renderer = when (codeMetaInfo.diagnostic.factory) {
            is DebugInfoDiagnosticFactory1 -> DiagnosticWithParameters1Renderer(
                "{0}",
                Renderers.TO_STRING
            ) as DiagnosticRenderer<Diagnostic>
            else -> DefaultErrorMessages.getRendererForDiagnostic(codeMetaInfo.diagnostic)
        }
        if (renderer is AbstractDiagnosticWithParametersRenderer) {
            val renderParameters = renderer.renderParameters(codeMetaInfo.diagnostic)
            params.addAll(ContainerUtil.map(renderParameters) { it.toString() })
        }
        if (renderSeverity)
            params.add("severity='${codeMetaInfo.diagnostic.severity}'")

        params.add(getAdditionalTags(codeMetaInfo))

        return params.filter { it.isNotEmpty() }.joinToString("; ")
    }

    private fun getTag(codeMetaInfo: DiagnosticCodeMetaInfo): String {
        return codeMetaInfo.diagnostic.factory.name
    }
}

open class LineMarkerRenderConfiguration(val renderDescription: Boolean = true) : AbstractCodeMetaInfoRenderConfiguration() {
    override fun asString(codeMetaInfo: ICodeMetaInfo): String {
        if (codeMetaInfo !is LineMarkerCodeMetaInfo) return ""
        return getTag() + if (renderParams) "(\"${getParamsString(codeMetaInfo)}\")" else ""
    }

    private fun getTag(): String {
        return "LINE_MARKER"
    }

    private fun getParamsString(lineMarkerCodeMetaInfo: LineMarkerCodeMetaInfo): String {
        val params = mutableListOf<String>()

        if (renderDescription)
            params.add("descr='${sanitizeLineMarkerTooltip(lineMarkerCodeMetaInfo.lineMarker.lineMarkerTooltip)}'")

        params.add(getAdditionalTags(lineMarkerCodeMetaInfo))

        return params.filter { it.isNotEmpty() }.joinToString("; ")
    }
}

open class HighlightingRenderConfiguration(
    val renderDescription: Boolean = true,
    val renderTextAttributesKey: Boolean = true,
    val renderSeverity: Boolean = true
) : AbstractCodeMetaInfoRenderConfiguration() {

    override fun asString(codeMetaInfo: ICodeMetaInfo): String {
        if (codeMetaInfo !is HighlightingCodeMetaInfo) return ""
        return getTag() + if (renderParams) "(${getParamsString(codeMetaInfo)})" else ""
    }

    private fun getTag(): String {
        return "HIGHLIGHTING"
    }

    private fun getParamsString(highlightingCodeMetaInfo: HighlightingCodeMetaInfo): String {
        val params = mutableListOf<String>()

        if (renderSeverity)
            params.add("severity='${getSeverity(highlightingCodeMetaInfo.highlightingInfo)}'")
        if (renderDescription)
            params.add("descr='${sanitizeLineBreaks(highlightingCodeMetaInfo.highlightingInfo.description)}'")
        if (renderTextAttributesKey)
            params.add("textAttributesKey='${highlightingCodeMetaInfo.highlightingInfo.forcedTextAttributesKey}'")

        params.add(getAdditionalTags(highlightingCodeMetaInfo))

        return params.filter { it.isNotEmpty() }.joinToString("; ")
    }

    private fun getSeverity(highlightingInfo: HighlightInfo): String {
        return if (highlightingInfo.severity == HighlightSeverity.INFORMATION) "info" else highlightingInfo.severity.toString()
            .toLowerCase()
    }
}