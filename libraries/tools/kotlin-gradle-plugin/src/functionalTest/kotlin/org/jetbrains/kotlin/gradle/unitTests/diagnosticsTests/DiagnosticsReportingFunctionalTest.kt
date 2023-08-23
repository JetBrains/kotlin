/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.diagnosticsTests

import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.internal.logging.slf4j.OutputEventListenerBackedLoggerContext
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.diagnostics.*
import org.jetbrains.kotlin.gradle.plugin.diagnostics.ToolingDiagnostic.Severity
import org.jetbrains.kotlin.gradle.plugin.diagnostics.ToolingDiagnostic.Severity.*
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.util.applyKotlinJvmPlugin
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.checkDiagnostics
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.slf4j.LoggerFactory
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DiagnosticsReportingFunctionalTest {
    private val gradleOutput = StringBuilder()

    @Before
    fun setUpGradleOutputListening() {
        (LoggerFactory.getILoggerFactory() as OutputEventListenerBackedLoggerContext).setOutputEventListener {
            gradleOutput.appendLine(it)
        }
    }

    @After
    fun tearDownGradleOutputListening() {
        (LoggerFactory.getILoggerFactory() as OutputEventListenerBackedLoggerContext).reset()
        gradleOutput.clear()
    }

    @Test
    fun testNonDuplicatedReporting() {
        buildProjectWithMockedCheckers {
            applyKotlinJvmPlugin()
            evaluate()

            assertEquals(2, diagnostics.size, "Expected MockCheckers to be launched and report one diagnostic each")
            reportTestDiagnostic()
            assertEquals(3, diagnostics.size, "Expected non-deduplicated diagnostic to be reported and stored")
            reportTestDiagnostic()
            assertEquals(4, diagnostics.size, "Expected non-deduplicated diagnostic to be reported and stored")
            checkDiagnostics("nonDuplicatedReporting")
        }
    }

    @Test
    fun testOncePerBuildReporting() {
        val root = buildProjectWithMockedCheckers()

        root.applyKotlinJvmPlugin()
        root.evaluate()
        assertEquals(2, root.diagnostics.size, "Expected 2 MockCheckers to report one diagnostic each")

        fun ProjectInternal.assertConsequentReportsOfOnePerBuildDiagnosticAreDeduplicated() {
            assertEquals(
                1, diagnostics.size,
                "Expected to have only one diagnostic reported in subproject; " +
                        "second is expected to be deduplicated because it is reported in root"
            )
        }

        buildProjectWithMockedCheckers("subproject-a", root) {
            applyKotlinJvmPlugin()
            evaluate()

            assertConsequentReportsOfOnePerBuildDiagnosticAreDeduplicated()
            reportOnePerBuildTestDiagnostic()
            assertConsequentReportsOfOnePerBuildDiagnosticAreDeduplicated()
        }

        buildProjectWithMockedCheckers("subproject-b", root) {
            applyKotlinJvmPlugin()
            evaluate()
            assertConsequentReportsOfOnePerBuildDiagnosticAreDeduplicated()
            reportOnePerBuildTestDiagnostic()
            assertConsequentReportsOfOnePerBuildDiagnosticAreDeduplicated()
        }

        root.checkDiagnostics("oncePerBuildReporting")
    }

    // Known quirk: deduplicated diagnostics use internalId as a default key of deduplication,
    // meaning that subsequent reported diagnostics with the same ID will be dropped even if
    // they have different message/severity
    @Test
    fun testOncePerBuildWithDifferentSeverities() {
        val root = buildProject()

        root.applyKotlinJvmPlugin()
        root.evaluate()

        buildProject(
            {
                withName("subproject")
                withParent(root)
            }
        ).run {
            applyKotlinJvmPlugin()
            evaluate()
            reportOnePerBuildTestDiagnostic()
            reportOnePerBuildTestDiagnostic(severity = ERROR) // NB: will be lost!
        }

        root.checkDiagnostics("deduplicationWithDifferentSeverities", compactRendering = false)
    }

    @Test
    fun testSuppressedWarningsAreNotStoredOrRendered() {
        buildProject().run {
            applyKotlinJvmPlugin()
            extraProperties.set(PropertiesProvider.PropertyNames.KOTLIN_SUPPRESS_GRADLE_PLUGIN_WARNINGS, testDiagnosticId)
            reportTestDiagnostic()
            assertEquals(0, diagnostics.size, "Suppressed diagnostics shouldn't be stored")
            evaluate()
            assertEquals(emptyList(), diagnostics, "Expected no diagnostic reported after project evaluation")
            checkDiagnostics("suppressedWarnings")
        }
    }

    @Test
    fun testSuppressedErrorsAreNotStoredOrRendered() {
        buildProject().run {
            applyKotlinJvmPlugin()
            extraProperties.set(PropertiesProvider.PropertyNames.KOTLIN_SUPPRESS_GRADLE_PLUGIN_ERRORS, testDiagnosticId)
            reportTestDiagnostic(severity = ERROR)
            assertEquals(0, diagnostics.size, "Suppressed diagnostics shouldn't be stored")

            evaluate()
            assertEquals(
                KotlinToolingDiagnostics.InternalKotlinGradlePluginPropertiesUsed.id,
                diagnostics.singleOrNull()?.factoryId,
                "Expected the diagnostic about using internal properties to be reported"
            )
            checkDiagnostics("suppressedErrors")
        }
    }

    @Test
    fun testSuppressForWarningsDoesntWorkForErrors() {
        buildProject().run {
            applyKotlinJvmPlugin()
            extraProperties.set(PropertiesProvider.PropertyNames.KOTLIN_SUPPRESS_GRADLE_PLUGIN_WARNINGS, testDiagnosticId)
            reportTestDiagnostic(severity = ERROR)
            assertEquals(
                1, diagnostics.size,
                "Expected that suppress for warnings doesn't work for ERROR, therefore the diagnostic should be stored"
            )
            evaluate()
            checkDiagnostics("suppressForWarningsDoesntWorkForErrors")
        }
    }

    @Test
    fun testLocationsAttaching() {
        buildProject().run {
            applyKotlinJvmPlugin()
            reportTestDiagnostic()
            evaluate()

            val diagnostics = project!!.kotlinToolingDiagnosticsCollector.getDiagnosticsForProject(rootProject)

            val projectDiagnostic = diagnostics.single()
            assertNotNull(projectDiagnostic, "Project diagnostic hasn't been reported")
            assertEquals(project!!.toLocation(), projectDiagnostic.location)
        }
    }

    @Test
    fun testDefaultDiagnosticHandling() {
        buildProject().run {
            applyKotlinJvmPlugin()
            assertTrue(gradleOutput.isEmpty(), "Expect to have nothing written to output after plugin application")

            reportTestDiagnostic()
            assertEquals(1, diagnostics.size, "Expected reported diagnostic to be stored right away")
            assertTrue(gradleOutput.isEmpty(), "Expected the diagnostic to be reported later")

            evaluate()
            assertTrue(gradleOutput.contains(testDiagnosticMessage), "Expected the diagnostic to be rendered after project evaluation")
            assertEquals(1, diagnostics.size, "Expected reported diagnostic to be still stored after rendering")
        }
    }

    @Test
    fun testFatalsHandling() {
        buildProject().run {
            applyKotlinJvmPlugin()
            assertTrue(gradleOutput.isEmpty(), "Expect to have nothing written to output after plugin application")

            var thrownException: Exception? = null
            try {
                reportTestDiagnostic(severity = FATAL)
            } catch (e: Exception) {
                thrownException = e
            }
            assertNotNull(thrownException, "Expected reporting a FATAL diagnostic to throw an exception immediately")
            assertEquals(1, diagnostics.size, "Expected FATAL diagnostic to be stored")
            // We can't assert how Gradle renders an exception because we caught it

            evaluate()
            assertEquals(
                0, testDiagnosticMessage.countOccurrencesIn(gradleOutput.toString()),
                "Expected FATALs rendering to not be rendered after project evaluation"
            )
        }
    }

    @Test
    fun testTransparentModeHandling() {
        buildProject().run {
            applyKotlinJvmPlugin()
            assertTrue(gradleOutput.isEmpty(), "Expected to have nothing written to output after plugin application")

            evaluate()

            assertTrue(gradleOutput.isEmpty(), "Expected to have nothing written to output after project evaluation")
            assertEquals(0, diagnostics.size, "Expected to have no diagnostics reported after project evaluation")

            // Usually this happens automatically in projectsEvaluated, but it doesn't fire in those minimal tests, so we're doing
            // it manually
            kotlinToolingDiagnosticsCollector.switchToTransparentMode()
            reportTestDiagnostic()

            assertEquals(
                1, testDiagnosticMessage.countOccurrencesIn(gradleOutput.toString()),
                "Expected diagnostic message to be printed immediately when in transparent mode"
            )
            assertEquals(1, diagnostics.size, "Expected the diagnostic to be stored even if the transparent mode is enabled")
        }
    }
}

private fun buildProjectWithMockedCheckers(
    name: String? = null,
    parent: ProjectInternal? = null,
    block: ProjectInternal.() -> Unit = { },
): ProjectInternal {
    val project = buildProject(
        {
            if (name != null) withName(name)
            if (parent != null) withParent(parent)
        }
    )

    project.allprojects {
        project.extensions.extraProperties.set(
            KOTLIN_GRADLE_PROJECT_CHECKERS_OVERRIDE,
            listOf(MockChecker, MockPerBuildChecker)
        )
    }

    project.block()
    return project
}

private val testDiagnosticId = "TEST_DIAGNOSTIC"
private val testDiagnosticMessage = "This is a test diagnostic\n\nIt has multiple lines of text"
private fun Project.reportTestDiagnostic(severity: Severity = WARNING) {
    val TEST_DIAGNOSTIC = ToolingDiagnostic(testDiagnosticId, testDiagnosticMessage, severity)
    kotlinToolingDiagnosticsCollector.report(
        project,
        TEST_DIAGNOSTIC
    )
}

private fun Project.reportOnePerBuildTestDiagnostic(severity: Severity = WARNING) {
    kotlinToolingDiagnosticsCollector.reportOncePerGradleBuild(
        project,
        ToolingDiagnostic(
            "TEST_DIAGNOSTIC_ONE_PER_BUILD",
            "This is a test diagnostics that should be reported once per build\n\nIt has multiple lines of text",
            severity
        )
    )
}

internal object MockChecker : KotlinGradleProjectChecker {
    override suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
        project.reportTestDiagnostic()
    }
}

internal object MockPerBuildChecker : KotlinGradleProjectChecker {
    override suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
        project.reportOnePerBuildTestDiagnostic()
    }
}

private val ProjectInternal.diagnostics: Collection<ToolingDiagnostic>
    get() = project!!.kotlinToolingDiagnosticsCollector.getDiagnosticsForProject(project!!)

private fun String.countOccurrencesIn(other: String) = other.split(this).size - 1
