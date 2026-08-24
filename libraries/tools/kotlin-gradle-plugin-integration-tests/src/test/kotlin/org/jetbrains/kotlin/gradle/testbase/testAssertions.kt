/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import com.intellij.openapi.util.JDOMUtil
import org.gradle.internal.impldep.com.google.common.hash.HashFunction
import org.gradle.internal.impldep.com.google.common.hash.Hashing
import org.gradle.util.GradleVersion
import org.jdom.Content
import org.jdom.Element
import org.jdom.Text
import org.jetbrains.kotlin.test.util.trimTrailingWhitespaces
import java.nio.file.Files
import java.nio.file.Path
import java.util.Base64
import kotlin.io.path.absolutePathString
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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

fun GradleProject.assertNoTestResultsProduced(
    taskName: String,
    subprojectName: String? = null,
) {
    val testResultsDir = testResultsAndReportsDirs(taskName, subprojectName).first
    if (Files.exists(testResultsDir)) {
        val xmlFiles = testResultsDir.allFilesWithExtension("xml")
        assertTrue(
            xmlFiles.isEmpty(),
            "Expected no test result XML files in '$testResultsDir', but found: ${xmlFiles.joinToString()}"
        )
    }
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

fun GradleProject.readTestCases(
    taskName: String,
    subprojectName: String? = null,
): List<TestCaseResult> {
    val simpleTaskName = taskName.substringAfterLast(':')
    val testReportDir = testResultsAndReportsDirs(taskName, subprojectName).first

    if (!Files.exists(testReportDir)) {
        return emptyList()
    }

    val xmlFiles = testReportDir.allFilesWithExtension("xml")
    return xmlFiles.flatMap { xmlFile ->
        val root = JDOMUtil.load(xmlFile)
        val testCases = when (root.name) {
            "testcase" -> listOf(root)
            "testsuites" -> root.getChildren("testcase") + root.getChildren("testsuite").flatMap { it.getChildren("testcase") }
            else -> root.getChildren("testcase")
        }
        testCases.map { testCaseElement ->
            val rawClassName = testCaseElement.getAttributeValue("classname")
                ?: testCaseElement.getAttributeValue("className")
                ?: ""
            val className = rawClassName.removePrefix("$simpleTaskName.")
            val name = testCaseElement.getAttributeValue("name") ?: ""
            val failureElement = testCaseElement.getChild("failure") ?: testCaseElement.getChild("error")
            val failure = failureElement?.let {
                TestFailureInfo(
                    message = it.getAttributeValue("message"),
                    type = it.getAttributeValue("type"),
                    stackTrace = it.textTrim.ifEmpty { null } ?: it.text.ifEmpty { null }
                )
            }
            TestCaseResult(
                className = className,
                name = name,
                failure = failure
            )
        }
    }
}

fun GradleProject.assertExecutedTestCases(
    taskName: String,
    vararg expectedIds: String,
    subprojectName: String? = null,
) {
    val actualIds = readTestCases(taskName, subprojectName).map { it.id }.toSortedSet()
    val expected = expectedIds.toSortedSet()
    assertEquals(expected, actualIds)
}

private fun GradleProject.testResultsAndReportsDirs(
    taskName: String,
    subprojectName: String? = null,
): Pair<Path, Path> {
    val cleanTaskName = taskName.removePrefix(":")
    val subproject: String? = when {
        subprojectName != null -> subprojectName
        cleanTaskName.contains(':') -> cleanTaskName.substringBeforeLast(':').replace(':', '/')
        else -> null
    }
    val simpleTaskName: String = cleanTaskName.substringAfterLast(':')
    val buildDirLocation = if (subproject != null) {
        projectPath.resolve(subproject)
    } else {
        projectPath
    }
    val testResultsDir = buildDirLocation.resolve("build/test-results/$simpleTaskName")
    val testReportsDir = buildDirLocation.resolve("build/reports/tests/$simpleTaskName")
    return testResultsDir to testReportsDir
}

fun GradleProject.testClassHtmlReport(
    taskName: String,
    className: String,
    gradleVersion: GradleVersion,
    subprojectName: String? = null,
    targetName: String? = null,
): Path {
    val testReportsDir = testResultsAndReportsDirs(taskName, subprojectName).second
    val simpleTaskName = taskName.removePrefix(":").substringAfterLast(':')

    val reportRelativePath = if (gradleVersion < GradleVersion.version(TestVersions.Gradle.G_9_3)) {
        "classes/$className.html"
    } else {
        val prefix = if (targetName == "jvm") "" else "$simpleTaskName."
        val dirName = if (gradleVersion < GradleVersion.version(TestVersions.Gradle.G_9_4) ||
            gradleVersion >= GradleVersion.version(TestVersions.Gradle.G_9_6)
        ) {
            "$prefix$className"
        } else {
            "$prefix$className".hashTestPathSegment()
        }
        "$dirName/index.html"
    }

    return testReportsDir.resolve(reportRelativePath)
}

// Adopted from Gradle
// platforms/software/testing-base/src/main/java/org/gradle/api/internal/tasks/testing/report/generic/GenericHtmlTestReportGenerator.java
// Caused by https://github.com/gradle/gradle/pull/37052
private fun String.hashTestPathSegment(): String {
    val hashBytes = TEST_PATH_HASHER.hashUnencodedChars(this).asBytes()
    return Base64.getUrlEncoder().withoutPadding().encodeToString(hashBytes)
}

private val TEST_PATH_HASHER: HashFunction = Hashing.farmHashFingerprint64()

data class TestCaseResult(
    val className: String,
    val name: String,
    val failure: TestFailureInfo? = null,
) {
    val id: String = "$className#${name.substringBefore('[')}"
}

data class TestFailureInfo(
    val message: String? = null,
    val type: String? = null,
    val stackTrace: String? = null,
)
