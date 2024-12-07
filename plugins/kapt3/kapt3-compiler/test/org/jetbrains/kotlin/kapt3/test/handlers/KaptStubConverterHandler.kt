/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt3.test.handlers

import com.intellij.openapi.util.text.StringUtil
import com.sun.tools.javac.util.JCDiagnostic
import com.sun.tools.javac.util.Log
import org.jetbrains.kotlin.kapt3.KaptContextForStubGeneration
import org.jetbrains.kotlin.kapt3.base.javac.KaptJavaLogBase
import org.jetbrains.kotlin.kapt3.test.KaptContextBinaryArtifact
import org.jetbrains.kotlin.kapt3.test.KaptTestDirectives.EXPECTED_ERROR
import org.jetbrains.kotlin.kapt3.test.KaptTestDirectives.EXPECTED_ERROR_K1
import org.jetbrains.kotlin.kapt3.test.KaptTestDirectives.EXPECTED_ERROR_K2
import org.jetbrains.kotlin.kapt3.test.KaptTestDirectives.NON_EXISTENT_CLASS
import org.jetbrains.kotlin.kapt3.test.messageCollectorProvider
import org.jetbrains.kotlin.kapt3.util.prettyPrint
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.util.trimTrailingWhitespacesAndAddNewlineAtEOF
import org.jetbrains.kotlin.test.utils.withExtension
import java.util.Locale

class KaptStubConverterHandler(testServices: TestServices) : BaseKaptHandler(testServices) {
    companion object {
        const val FILE_SEPARATOR = "\n\n////////////////////\n\n"
    }

    override fun processModule(module: TestModule, info: KaptContextBinaryArtifact) {
        val generateNonExistentClass = NON_EXISTENT_CLASS in module.directives
        val kaptContext = info.kaptContext

        val convertedFiles = convert(module, kaptContext, generateNonExistentClass)

        val actualRaw = convertedFiles
            .sortedBy { it.sourceFile.name }
            .joinToString(FILE_SEPARATOR) { it.prettyPrint(kaptContext.context, ::renderMetadata) }

        val actual = StringUtil.convertLineSeparators(actualRaw.trim { it <= ' ' })
            .trimTrailingWhitespacesAndAddNewlineAtEOF()
            .let { removeMetadataAnnotationContents(it) }

        checkErrors(module, kaptContext, actual)

        val isFir = module.frontendKind == FrontendKinds.FIR
        val testDataFile = module.files.first().originalFile
        val firFile = testDataFile.withExtension("fir.txt")
        val txtFile = testDataFile.withExtension("txt")
        val expectedFile = if (isFir && firFile.exists()) firFile else txtFile

        assertions.assertEqualsToFile(expectedFile, actual)

        if (isFir && firFile.exists() && txtFile.exists() && txtFile.readText() == firFile.readText()) {
            assertions.fail { ".fir.txt and .txt golden files are identical. Remove $firFile." }
        }
    }

    private fun KaptStubConverterHandler.checkErrors(
        module: TestModule,
        kaptContext: KaptContextForStubGeneration,
        actualStubs: String,
    ) {
        val expectedErrors = (
                module.directives[EXPECTED_ERROR] +
                        module.directives[if (module.frontendKind == FrontendKinds.FIR) EXPECTED_ERROR_K2 else EXPECTED_ERROR_K1]
                ).sorted()

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

        val actualErrorsStr = actualErrors.joinToString(System.lineSeparator()) { it.toDirectiveView() }

        if (expectedErrors.isNotEmpty() && actualErrorsStr.isEmpty()) {
            assertions.fail { "Kapt finished successfully but errors were expected." }
        } else if (expectedErrors.isEmpty() && actualErrorsStr.isNotEmpty()) {
            assertions.fail { "There were errors during kapt:\n$actualErrorsStr\n\nStubs:\n\n$actualStubs" }
        } else {
            val expectedErrorsStr = expectedErrors.joinToString(System.lineSeparator()) { it.toDirectiveView() }
            if (expectedErrorsStr != actualErrorsStr) {
                assertions.assertEquals(expectedErrorsStr, actualErrorsStr) {
                    System.err.println(testServices.messageCollectorProvider.getErrorStream(module).toString("UTF8"))
                    "Expected error matching failed"
                }
            }
        }
    }

    private fun String.toDirectiveView(): String = "// ${EXPECTED_ERROR.name}: $this"

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}
}
