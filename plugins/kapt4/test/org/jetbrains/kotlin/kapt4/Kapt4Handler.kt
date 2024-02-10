/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt4

import com.intellij.openapi.util.text.StringUtil
import com.sun.tools.javac.comp.CompileStates
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.util.JCDiagnostic
import com.sun.tools.javac.util.List
import com.sun.tools.javac.util.Log
import org.jetbrains.kotlin.kapt3.base.KaptContext
import org.jetbrains.kotlin.kapt3.base.javac.KaptJavaLogBase
import org.jetbrains.kotlin.kapt3.base.parseJavaFiles
import org.jetbrains.kotlin.kapt3.javac.KaptJavaFileObject
import org.jetbrains.kotlin.kapt3.test.KaptTestDirectives
import org.jetbrains.kotlin.kapt3.test.KaptTestDirectives.EXPECTED_ERROR
import org.jetbrains.kotlin.kapt3.test.handlers.ClassFileToSourceKaptStubHandler
import org.jetbrains.kotlin.kapt3.test.handlers.removeMetadataAnnotationContents
import org.jetbrains.kotlin.kapt3.test.messageCollectorProvider
import org.jetbrains.kotlin.test.Assertions
import org.jetbrains.kotlin.test.model.AnalysisHandler
import org.jetbrains.kotlin.test.model.TestArtifactKind
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.sourceFileProvider
import org.jetbrains.kotlin.test.util.trimTrailingWhitespacesAndAddNewlineAtEOF
import org.jetbrains.kotlin.test.utils.withExtension
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import java.io.File
import java.util.*

internal class Kapt4Handler(testServices: TestServices) : AnalysisHandler<Kapt4ContextBinaryArtifact>(
    testServices,
    failureDisablesNextSteps = true,
    doNotRunIfThereWerePreviousFailures = true
) {
    override val artifactKind: TestArtifactKind<Kapt4ContextBinaryArtifact>
        get() = Kapt4ContextBinaryArtifact.Kind

    override fun processModule(module: TestModule, info: Kapt4ContextBinaryArtifact) {
        val validate = KaptTestDirectives.NO_VALIDATION !in module.directives

        val (kaptContext) = info
        val convertedFiles = getJavaFiles(info)
        kaptContext.javaLog.interceptorData.files = convertedFiles.associateBy { it.sourceFile }
        if (validate) kaptContext.compiler.enterTrees(convertedFiles)

        val actualRaw = convertedFiles
            .sortedBy { it.sourceFile.name }
            .joinToString(ClassFileToSourceKaptStubHandler.FILE_SEPARATOR) { (it.sourceFile as KaptJavaFileObject).file!!.readText() }

        val actual = StringUtil.convertLineSeparators(actualRaw.trim { it <= ' ' })
            .trimTrailingWhitespacesAndAddNewlineAtEOF()
            .let { removeMetadataAnnotationContents(it) }

        assertions.assertAll(
            { assertions.checkTxt(module, actual) },
            {
                if (kaptContext.compiler.shouldStop(CompileStates.CompileState.ENTER)) {
                    checkJavaCompilerErrors(module, kaptContext, actual)
                }
            }
        )
    }

    private fun checkJavaCompilerErrors(
        module: TestModule,
        kaptContext: KaptContext,
        actualDump: String
    ) {
        val expectedErrors = (module.directives[EXPECTED_ERROR] + module.directives[Kapt4TestDirectives.EXPECTED_ERROR_K2]).sorted()
        val log = Log.instance(kaptContext.context) as KaptJavaLogBase

        val actualErrors = log.reportedDiagnostics
            .filter { it.type == JCDiagnostic.DiagnosticType.ERROR }
            .map {
                // Unfortunately, we can't use the file name as it can contain temporary prefix
                val name = it.source?.name?.substringAfterLast("/") ?: ""
                val kind = when (name.substringAfterLast(".").lowercase()) {
                    "kt" -> "kotlin"
                    "java" -> "java"
                    else -> "other"
                }

                val javaLocation = "($kind:${it.lineNumber}:${it.columnNumber}) "
                javaLocation + it.getMessage(Locale.US).lines().first()
            }
            .sorted()

        log.flush()

        val lineSeparator = System.getProperty("line.separator")
        val actualErrorsStr = actualErrors.joinToString(lineSeparator) { it.toDirectiveView() }

        if (expectedErrors.isEmpty()) {
            assertions.fail { "There were errors during analysis:\n$actualErrorsStr\n\nStubs:\n\n$actualDump" }
        } else {
            val expectedErrorsStr = expectedErrors.joinToString(lineSeparator) { it.toDirectiveView() }
            if (expectedErrorsStr != actualErrorsStr) {
                assertions.assertEquals(expectedErrorsStr, actualErrorsStr) {
                    System.err.println(testServices.messageCollectorProvider.getErrorStream(module).toString("UTF8"))
                    "Expected error matching failed"
                }
            }
        }
    }

    private fun getJavaFiles(
        info: Kapt4ContextBinaryArtifact
    ): List<JCTree.JCCompilationUnit> {
        val (kaptContext, kaptStubs) = info
        val convertedFiles = kaptStubs.mapIndexed { index, stub ->
            val sourceFile = createTempJavaFile("stub$index.java", stub.source)
            stub.writeMetadata(forSource = sourceFile)
            sourceFile
        }

        // A workaround needed for Javac to parse files correctly even if errors were already reported
        // If nerrors > 0, "parseFiles()" returns the empty list
        val oldErrorCount = kaptContext.compiler.log.nerrors
        kaptContext.compiler.log.nerrors = 0

        try {
            val parsedJavaFiles = kaptContext.parseJavaFiles(convertedFiles)

            for (tree in parsedJavaFiles) {
                val actualFile = File(tree.sourceFile.toUri())

                // By default, JavaFileObject.getName() returns the absolute path to the file.
                // In our test, such a path will be temporary, so the comparison against it will lead to flaky tests.
                tree.sourcefile = KaptJavaFileObject(tree, tree.defs.firstIsInstance(), actualFile)
            }

            return parsedJavaFiles
        } finally {
            kaptContext.compiler.log.nerrors = oldErrorCount
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}

    private fun createTempJavaFile(name: String, text: String): File {
        return testServices.sourceFileProvider.javaSourceDirectory.resolve(name).also {
            it.writeText(text)
        }
    }

    private fun String.toDirectiveView(): String = "// ${EXPECTED_ERROR.name}: $this"
}

fun Assertions.checkTxt(module: TestModule, actual: String) {
    val testDataFile = module.files.first().originalFile
    val firFile = testDataFile.withExtension("fir.txt")
    val txtFile = testDataFile.withExtension("txt")
    val expectedFile = if (firFile.exists()) firFile else txtFile

    assertEqualsToFile(expectedFile, actual)
    if (firFile.exists() && txtFile.exists() && txtFile.readText() == firFile.readText()) {
        fail { ".fir.txt and .txt golden files are identical. Remove $firFile." }
    }
}
