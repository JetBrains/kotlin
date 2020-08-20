/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.resolve

import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.util.Condition
import com.intellij.openapi.util.Conditions
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.util.ThrowableRunnable
import com.intellij.util.io.exists
import org.jetbrains.kotlin.checkers.API_VERSION_DIRECTIVE
import org.jetbrains.kotlin.checkers.diagnostics.factories.DebugInfoDiagnosticFactory0
import org.jetbrains.kotlin.checkers.diagnostics.factories.SyntaxErrorDiagnosticFactory
import org.jetbrains.kotlin.checkers.utils.CheckerTestUtil
import org.jetbrains.kotlin.checkers.utils.DiagnosticsRenderingConfiguration
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.idea.multiplatform.setupMppProjectFromTextFile
import org.jetbrains.kotlin.idea.project.KotlinMultiplatformAnalysisModeComponent
import org.jetbrains.kotlin.idea.resolve.frontendService
import org.jetbrains.kotlin.idea.stubs.AbstractMultiModuleTest
import org.jetbrains.kotlin.idea.test.IDEA_TEST_DATA_DIR
import org.jetbrains.kotlin.idea.test.allKotlinFiles
import org.jetbrains.kotlin.idea.test.runAll
import org.jetbrains.kotlin.idea.util.sourceRoots
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.Directives
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.Assert
import java.io.File
import java.nio.file.Paths
import java.util.regex.Pattern

abstract class AbstractMultiModuleIdeResolveTest : AbstractMultiModuleTest() {
    fun doTest(testDataPath: String) {
        val testRoot = File(testDataPath)

        val dependenciesTxt = File(testDataPath, "dependencies.txt")
        require(dependenciesTxt.exists()) {
            "${dependenciesTxt.absolutePath} does not exist. dependencies.txt is required"
        }

        // This will implicitly copy all source files to temporary directory, clearing them from diagnostic markup in process
        setupMppProjectFromTextFile(testRoot)

        project.allKotlinFiles()

        for (module in ModuleManager.getInstance(project).modules) {
            for (sourceRoot in module.sourceRoots) {
                VfsUtilCore.processFilesRecursively(sourceRoot) { file ->
                    if (file.isDirectory) return@processFilesRecursively true

                    val tempSourceKtFile = PsiManager.getInstance(project).findFile(file) as KtFile
                    checkFile(tempSourceKtFile, tempSourceKtFile.findCorrespondingFileInTestDir(sourceRoot, testRoot))
                    true
                }
            }
        }
    }

    private fun KtFile.findCorrespondingFileInTestDir(containingRoot: VirtualFile, testDir: File): File {
        val tempRootPath = Paths.get(containingRoot.path)
        val tempProjectDirPath = tempRootPath.parent
        val tempSourcePath = Paths.get(this.virtualFilePath)

        val relativeToProjectRootPath = tempProjectDirPath.relativize(tempSourcePath)

        val testSourcesProjectDirPath = testDir.toPath()
        val testSourcePath = testSourcesProjectDirPath.resolve(relativeToProjectRootPath)

        require(testSourcePath.exists()) {
            "Can't find file in testdata for copied file $this: checked at path ${testSourcePath.toAbsolutePath()}"
        }

        return testSourcePath.toFile()
    }

    protected open fun checkFile(file: KtFile, expectedFile: File) {
        val resolutionFacade = file.getResolutionFacade()
        val (bindingContext, moduleDescriptor) = resolutionFacade.analyzeWithAllCompilerChecks(listOf(file))

        val directives = KotlinTestUtils.parseDirectives(file.text)
        val diagnosticsFilter = parseDiagnosticFilterDirective(directives, allowUnderscoreUsage = false)

        val actualDiagnostics = CheckerTestUtil.getDiagnosticsIncludingSyntaxErrors(
            bindingContext,
            file,
            markDynamicCalls = false,
            dynamicCallDescriptors = mutableListOf(),
            configuration = DiagnosticsRenderingConfiguration(
                platform = null, // we don't need to attach platform-description string to diagnostic here
                withNewInference = false,
                languageVersionSettings = resolutionFacade.frontendService(),
            ),
            dataFlowValueFactory = resolutionFacade.frontendService(),
            moduleDescriptor = moduleDescriptor as ModuleDescriptorImpl
        ).filter { diagnosticsFilter.value(it.diagnostic) }

        val actualTextWithDiagnostics = CheckerTestUtil.addDiagnosticMarkersToText(
            file,
            actualDiagnostics,
            diagnosticToExpectedDiagnostic = emptyMap(),
            getFileText = { it.text },
            uncheckedDiagnostics = emptyList(),
            withNewInferenceDirective = false,
            renderDiagnosticMessages = true
        ).toString()

        KotlinTestUtils.assertEqualsToFile(expectedFile, actualTextWithDiagnostics)
    }

    companion object {
        private const val DIAGNOSTICS_DIRECTIVE = "DIAGNOSTICS"
        private val DIAGNOSTICS_PATTERN: Pattern = Pattern.compile("([+\\-!])(\\w+)\\s*")
        private val DIAGNOSTICS_TO_INCLUDE_ANYWAY: Set<DiagnosticFactory<*>> = setOf(
                Errors.UNRESOLVED_REFERENCE,
                Errors.UNRESOLVED_REFERENCE_WRONG_RECEIVER,
                SyntaxErrorDiagnosticFactory.INSTANCE,
                DebugInfoDiagnosticFactory0.ELEMENT_WITH_ERROR_TYPE,
                DebugInfoDiagnosticFactory0.MISSING_UNRESOLVED,
                DebugInfoDiagnosticFactory0.UNRESOLVED_WITH_TARGET
        )

        fun parseDiagnosticFilterDirective(
                directiveMap: Directives,
                allowUnderscoreUsage: Boolean
        ): Condition<Diagnostic> {
            val directives = directiveMap[DIAGNOSTICS_DIRECTIVE]
            val initialCondition =
                    if (allowUnderscoreUsage)
                        Condition<Diagnostic> { it.factory.name != "UNDERSCORE_USAGE_WITHOUT_BACKTICKS" }
                    else
                        Conditions.alwaysTrue()

            if (directives == null) {
                // If "!API_VERSION" is present, disable the NEWER_VERSION_IN_SINCE_KOTLIN diagnostic.
                // Otherwise it would be reported in any non-trivial test on the @SinceKotlin value.
                if (API_VERSION_DIRECTIVE in directiveMap) {
                    return Conditions.and(initialCondition, Condition { diagnostic ->
                        diagnostic.factory !== Errors.NEWER_VERSION_IN_SINCE_KOTLIN
                    })
                }
                return initialCondition
            }

            var condition = initialCondition
            val matcher = DIAGNOSTICS_PATTERN.matcher(directives)
            if (!matcher.find()) {
                Assert.fail(
                        "Wrong syntax in the '// !$DIAGNOSTICS_DIRECTIVE: ...' directive:\n" +
                        "found: '$directives'\n" +
                        "Must be '([+-!]DIAGNOSTIC_FACTORY_NAME|ERROR|WARNING|INFO)+'\n" +
                        "where '+' means 'include'\n" +
                        "      '-' means 'exclude'\n" +
                        "      '!' means 'exclude everything but this'\n" +
                        "directives are applied in the order of appearance, i.e. !FOO +BAR means include only FOO and BAR"
                )
            }

            var first = true
            do {
                val operation = matcher.group(1)
                val name = matcher.group(2)

                val newCondition: Condition<Diagnostic> =
                        if (name in setOf("ERROR", "WARNING", "INFO")) {
                            Condition { diagnostic -> diagnostic.severity == Severity.valueOf(name) }
                        } else {
                            Condition { diagnostic -> name == diagnostic.factory.name }
                        }

                when (operation) {
                    "!" -> {
                        if (!first) {
                            Assert.fail(
                                    "'$operation$name' appears in a position rather than the first one, " +
                                    "which effectively cancels all the previous filters in this directive"
                            )
                        }
                        condition = newCondition
                    }
                    "+" -> condition = Conditions.or(condition, newCondition)
                    "-" -> condition = Conditions.and(condition, Conditions.not(newCondition))
                }
                first = false
            } while (matcher.find())

            // We always include UNRESOLVED_REFERENCE and SYNTAX_ERROR because they are too likely to indicate erroneous test data
            return Conditions.or(
                    condition,
                    Condition { diagnostic -> diagnostic.factory in DIAGNOSTICS_TO_INCLUDE_ANYWAY }
            )
        }
    }
}

abstract class AbstractMultiplatformAnalysisTest : AbstractMultiModuleIdeResolveTest() {
    override fun getTestDataDirectory() = IDEA_TEST_DATA_DIR.resolve("multiplatform")

    override fun setUp() {
        super.setUp()
        KotlinMultiplatformAnalysisModeComponent.setMode(project, KotlinMultiplatformAnalysisModeComponent.Mode.COMPOSITE)
    }

    override fun tearDown() {
        runAll(
            ThrowableRunnable {
                KotlinMultiplatformAnalysisModeComponent.setMode(project, KotlinMultiplatformAnalysisModeComponent.Mode.SEPARATE)
            },
            ThrowableRunnable { super.tearDown() }
        )
    }
}