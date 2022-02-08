/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.jdom.CDATA
import org.jdom.Content
import org.jdom.Element
import org.jdom.input.SAXBuilder
import org.jdom.output.Format
import org.jdom.output.XMLOutputter
import org.jetbrains.kotlin.test.util.trimTrailingWhitespaces
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.streams.asSequence
import kotlin.streams.toList
import kotlin.test.assertEquals

fun GradleProject.assertTestResults(expectedTestReport: Path, vararg testReportNames: String) {
    val testReportDirs = testReportNames.map { projectPath.resolve("build/test-results/$it") }

    assertDirectoriesExist(*testReportDirs.toTypedArray())

    val actualTestResults = readAndCleanupTestResults(testReportDirs, projectPath)
    val expectedTestResults = prettyPrintXml(expectedTestReport.readText())

    assertEquals(expectedTestResults, actualTestResults)
}

internal fun readAndCleanupTestResults(
    testReportDirs: List<Path>,
    projectPath: Path,
    cleanupStdOut: (String) -> String = { it }
): String {
    val files = testReportDirs
        .flatMap {
            it.allFilesWithExtension("xml")
        }
        .sortedBy {
            // let containing test suite be first
            it.name.replace(".xml", ".A.xml")
        }

    val xmlString = buildString {
        appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        appendLine("<results>")
        files.forEach { file ->
            appendLine(
                file.readText()
                    .trimTrailingWhitespaces()
                    .replace(projectPath.absolutePathString(), "/\$PROJECT_DIR$")
                    .replace(projectPath.name, "\$PROJECT_NAME$")
                    .replace("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n", "")
            )
        }
        appendLine("</results>")
    }

    val doc = SAXBuilder().build(xmlString.reader())
    val skipAttrs = setOf("timestamp", "hostname", "time", "message")
    val skipContentsOf = setOf("failure")

    fun cleanup(e: Element) {
        if (e.name in skipContentsOf) e.text = "..."
        e.attributes.forEach {
            if (it.name in skipAttrs) {
                it.value = "..."
            }
        }
        if (e.name == "system-out") {
            val content = e.content.map {
                if (it.cType == Content.CType.CDATA) {
                    (it as CDATA).text = cleanupStdOut(it.value)
                }
                it
            }
            e.setContent(content)
        }

        e.children.forEach {
            cleanup(it)
        }
    }

    cleanup(doc.rootElement)
    return XMLOutputter(Format.getPrettyFormat()).outputString(doc)
}

internal fun prettyPrintXml(uglyXml: String): String =
    XMLOutputter(Format.getPrettyFormat()).outputString(SAXBuilder().build(uglyXml.reader()))