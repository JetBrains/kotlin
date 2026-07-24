/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import com.intellij.openapi.util.JDOMUtil
import org.jdom.Content
import org.jdom.Element
import org.jdom.Text
import org.jetbrains.kotlin.test.util.trimTrailingWhitespaces
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.test.assertEquals

/**
 * @param stripBrowserVersionInfoFromTestCaseNames Some test executor implementations include browser version info in test case names,
 * which can make test results difficult to compare. Example:
 *
 * ```xml
 * <testcase name="test[wasmJs, browser, ChromeHeadless150.0.0.0, MacOS10.15.7]" classname="PrintTest" time="..." />
 * ```
 *
 * If [stripBrowserVersionInfoFromTestCaseNames] is true, this function will strip the browser version info from test case names as
 * follows:
 *
 * ```xml
 * <testcase name="test[wasmJs, browser]" classname="PrintTest" time="..." />
 * ```
 *
 * In cases where browser info is more predictable, leave this parameter as false.
 */
fun GradleProject.assertTestResults(
    expectedTestReport: Path,
    vararg testReportNames: String,
    subprojectName: String? = null,
    stripBrowserVersionInfoFromTestCaseNames: Boolean = false,
    attributeValidators: Map<String, (String) -> Unit> = emptyMap(),
    cleanupStdOut: (String) -> String = { it },
) {
    val buildDirLocation = if (subprojectName != null) { projectPath.resolve(subprojectName) } else projectPath
    val testReportDirs = testReportNames.map { buildDirLocation.resolve("build/test-results/$it") }

    assertDirectoriesExist(*testReportDirs.toTypedArray())

    val actualTestResults = readValidateAndCleanupTestResults(
        testReportDirs,
        projectPath,
        stripBrowserVersionInfoFromTestCaseNames,
        attributeValidators,
        cleanupStdOut
    )
    val expectedTestResults = prettyPrintXml(expectedTestReport.readText())

    assertEquals(expectedTestResults, actualTestResults)
}

internal fun readValidateAndCleanupTestResults(
    testReportDirs: List<Path>,
    projectPath: Path,
    stripBrowserVersionInfoFromTestCaseNames: Boolean,
    attributeValidators: Map<String, (String) -> Unit> = emptyMap(),
    cleanupStdOut: (String) -> String = { it },
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

    val doc = JDOMUtil.load(xmlString.reader())
    val skipAttrs = setOf("timestamp", "hostname", "time", "message")
    val skipContentsOf = setOf("failure")

    fun cleanup(e: Element) {
        if (e.name in skipContentsOf) e.text = "..."

        val browserTestRegex = "\\[(.*(, )?)browser,.*]".toRegex();
        e.attributes.forEach {
            attributeValidators[it.name]?.let { validator ->
                validator(it.value)
            }
            if (it.name in skipAttrs) {
                it.value = "..."
            } else if (stripBrowserVersionInfoFromTestCaseNames &&
                it.name == "name" &&
                e.name == "testcase" &&
                it.value.contains(browserTestRegex)
            ) {
                it.value = it.value.replace(browserTestRegex, "[$1browser]")
            }
        }
        if (e.name == "system-out") {
            val content = e.content.map {
                if (it.cType == Content.CType.CDATA || it.cType == Content.CType.Text) {
                    (it as Text).text = cleanupStdOut(it.value)
                }
                it
            }
            e.setContent(content)
        }

        e.children.forEach {
            cleanup(it)
        }
    }

    cleanup(doc)
    return JDOMUtil.write(doc)
}

internal fun prettyPrintXml(uglyXml: String): String =
    JDOMUtil.write(JDOMUtil.load(uglyXml.reader()))
