/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import java.io.File
import java.net.URI
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal object ProblemsApiTestUtils {
    fun extractProblemReportUrl(output: String): String? {
        val urlRegex = """file:///[^\s]+problems-report\.html""".toRegex()
        return urlRegex.find(output)?.value
    }

    fun readProblemReportContent(urlString: String): String {
        val uri = URI(urlString)
        val file = File(uri)

        return if (file.exists()) {
            file.readText()
        } else {
            "File not found: $urlString"
        }
    }

    fun parseProblemReportFromScript(
        scriptContent: String,
        gradleVersion: GradleVersion,
    ): ProblemReport<*> {
        val jsonRegex = """// begin-report-data\s*(\{.*\})\s*// end-report-data""".toRegex(RegexOption.DOT_MATCHES_ALL)
        val matchResult = jsonRegex.find(scriptContent)
            ?: throw IllegalArgumentException("Could not extract JSON from script")

        val json = Json {
            isLenient = true
            ignoreUnknownKeys = true
        }

        return if (gradleVersion < GradleVersion.version(TestVersions.Gradle.G_9_4)) {
            json.decodeFromString<ProblemReport<ProblemsApiDiagnosticG811>>(matchResult.groupValues[1])
        } else {
            json.decodeFromString<ProblemReport<ProblemsApiDiagnosticG94>>(matchResult.groupValues[1])
        }
    }
}

internal fun BuildResult.assertProblemsReportContainsDiagnostic(
    expectedProblemId: String,
    expectedMessageSubstring: String,
    gradleVersion: GradleVersion,
) {
    val reportUrl = ProblemsApiTestUtils.extractProblemReportUrl(output)
    assertNotNull(reportUrl, "Problems API HTML report URL not found in build output")

    val reportContent = ProblemsApiTestUtils.readProblemReportContent(reportUrl)
    val report = ProblemsApiTestUtils.parseProblemReportFromScript(reportContent, gradleVersion)

    val matchingDiagnostics = report.diagnostics.filter { diagnostic ->
        diagnostic.problemId.any { it.name == expectedProblemId } &&
                (diagnostic.problemDetailsActual.contains(expectedMessageSubstring) ||
                        diagnostic.contextualLabel.contains(expectedMessageSubstring))
    }

    assertTrue(
        matchingDiagnostics.isNotEmpty(),
        "Expected Problems API HTML report to contain a '$expectedProblemId' diagnostic " +
                "with message containing '$expectedMessageSubstring', but none found.\n" +
                "Report diagnostics: ${report.diagnostics.map { d -> "${d.problemId.map { it.name }} -> details: ${d.problemDetailsActual}}" }}"
    )
}

@Serializable
internal data class ProblemReport<PD : ProblemDiagnostic>(
    val diagnostics: List<PD>,
    val problemsReport: ProblemsReportSummary,
)

internal interface ProblemDiagnostic {
    val locations: List<ProblemsApiLocation>
    val problem: List<TextWrapper>
    val severity: String
    val problemDetailsActual: String
    val contextualLabel: String
    val problemId: List<ProblemIdentifier>
    val solutionsActual: List<String>
}

@Serializable
internal data class ProblemsApiDiagnosticG811(
    override val locations: List<ProblemsApiLocation> = emptyList(),
    override val problem: List<TextWrapper> = emptyList(),
    override val severity: String = "",
    private val problemDetails: List<TextWrapper> = emptyList(),
    override val contextualLabel: String = "",
    override val problemId: List<ProblemIdentifier> = emptyList(),
    private val solutions: List<List<TextWrapper>> = emptyList(),
) : ProblemDiagnostic {
    override val problemDetailsActual: String
        get() = problemDetails.firstOrNull()?.text ?: ""

    override val solutionsActual: List<String>
        get() = solutions.flatten().map { it.text }

}

@Serializable
internal data class ProblemsApiDiagnosticG94(
    override val locations: List<ProblemsApiLocation> = emptyList(),
    override val problem: List<TextWrapper> = emptyList(),
    override val severity: String = "",
    private val problemDetails: String = "",
    override val contextualLabel: String = "",
    override val problemId: List<ProblemIdentifier> = emptyList(),
    private val solutions: List<String> = emptyList(),
) : ProblemDiagnostic {
    override val problemDetailsActual: String
        get() = problemDetails

    override val solutionsActual: List<String>
        get() = solutions

}

@Serializable
internal data class TextWrapper(val text: String)

@Serializable
internal data class ProblemsApiLocation(val pluginId: String = "")

@Serializable
internal data class ProblemIdentifier(
    val name: String,
    val displayName: String,
)

@Serializable
internal data class ProblemsReportSummary(
    val totalProblemCount: Int,
    val buildName: String,
    val requestedTasks: String,
    val documentationLink: String,
    // Missing since Gradle 9.4
    val documentationLinkCaption: String? = null,
    val summaries: List<String> = emptyList(),
)
