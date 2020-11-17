/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.repl.messages

import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticUtils
import org.w3c.dom.ls.DOMImplementationLS
import javax.xml.parsers.DocumentBuilderFactory

class IdeDiagnosticMessageHolder : DiagnosticMessageHolder {
    private val diagnostics = arrayListOf<Pair<Diagnostic, String>>()

    override fun report(diagnostic: Diagnostic, file: PsiFile?, render: String) {
        diagnostics.add(Pair(diagnostic, render))
    }

    override fun renderMessage(): String {
        val docFactory = DocumentBuilderFactory.newInstance()
        val docBuilder = docFactory.newDocumentBuilder()
        val errorReport = docBuilder.newDocument()

        val rootElement = errorReport.createElement("report")
        errorReport.appendChild(rootElement)

        for ((diagnostic, message) in diagnostics) {
            val errorRange = DiagnosticUtils.firstRange(diagnostic.textRanges)

            val reportEntry = errorReport.createElement("reportEntry")
            reportEntry.setAttribute("severity", diagnostic.severity.toString())
            reportEntry.setAttribute("rangeStart", errorRange.startOffset.toString())
            reportEntry.setAttribute("rangeEnd", errorRange.endOffset.toString())
            reportEntry.appendChild(errorReport.createTextNode(StringUtil.escapeXmlEntities(message)))

            rootElement.appendChild(reportEntry)
        }

        val domImplementation = errorReport.implementation as DOMImplementationLS
        val lsSerializer = domImplementation.createLSSerializer()
        return lsSerializer.writeToString(errorReport)
    }
}
