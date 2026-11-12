/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.test.handlers

import com.intellij.openapi.util.text.StringUtil
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.util.JCDiagnostic
import com.sun.tools.javac.util.Log
import org.jetbrains.kotlin.config.expectActualTracker
import org.jetbrains.kotlin.kapt.KaptContextForStubGeneration
import org.jetbrains.kotlin.kapt.base.javac.KaptJavaLogBase
import org.jetbrains.kotlin.kapt.base.stubs.KaptStubLineInformation
import org.jetbrains.kotlin.kapt.test.KaptContextBinaryArtifact
import org.jetbrains.kotlin.kapt.test.KaptTestDirectives
import org.jetbrains.kotlin.kapt.test.KaptTestDirectives.DUMP_EXPECT_ACTUAL_FILES
import org.jetbrains.kotlin.kapt.test.KaptTestDirectives.DUMP_LINE_MAPPINGS
import org.jetbrains.kotlin.kapt.test.KaptTestDirectives.DUMP_MODULE_MAPPING
import org.jetbrains.kotlin.kapt.test.KaptTestDirectives.EXPECTED_ERROR
import org.jetbrains.kotlin.kapt.test.KaptTestDirectives.NON_EXISTENT_CLASS
import org.jetbrains.kotlin.kapt.test.RecordingExpectActualTracker
import org.jetbrains.kotlin.kapt.util.prettyPrint
import org.jetbrains.kotlin.load.kotlin.loadModuleMapping
import org.jetbrains.kotlin.metadata.jvm.deserialization.ModuleMapping
import org.jetbrains.kotlin.serialization.deserialization.DeserializationConfiguration
import org.jetbrains.kotlin.test.directives.TestDumpDirectives
import org.jetbrains.kotlin.test.directives.assertEqualsToDump
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.util.trimTrailingWhitespacesAndAddNewlineAtEOF
import java.io.ByteArrayInputStream
import java.io.File
import java.io.ObjectInputStream
import java.util.*

class KaptStubConverterHandler(testServices: TestServices) : BaseKaptHandler(testServices) {
    companion object {
        const val FILE_SEPARATOR = "\n\n////////////////////\n\n"
    }

    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(TestDumpDirectives, KaptTestDirectives)

    override fun processModule(module: TestModule, info: KaptContextBinaryArtifact) {
        val generateNonExistentClass = NON_EXISTENT_CLASS in module.directives
        val kaptContext = info.kaptContext ?: assertions.fail { "KAPT context is not created" }

        val convertedFiles = convert(module, kaptContext, generateNonExistentClass)

        val actualRaw = convertedFiles
            .sortedBy { it.sourceFile.name }
            .joinToString(FILE_SEPARATOR) { it.prettyPrint(kaptContext.context, ::renderMetadata) }

        val actual = (StringUtil.convertLineSeparators(actualRaw.trim { it <= ' ' }) + lineMappings(module, convertedFiles) +
                moduleMapping(module, kaptContext) + expectActualFiles(module, kaptContext))
            .trimTrailingWhitespacesAndAddNewlineAtEOF()
            .let { removeMetadataAnnotationContents(it) }

        checkErrors(module, kaptContext, actual)

        assertEqualsToDump(extension = "txt", actual)
    }

    /** The positions written to `.kapt_metadata` next to each stub, as `fqName: file:offset`. */
    private fun lineMappings(module: TestModule, files: List<JCTree.JCCompilationUnit>): String {
        if (DUMP_LINE_MAPPINGS !in module.directives) return ""
        val mappings = files.flatMap { file ->
            val stub = File(file.sourcefile.toUri())
            val metadata = File(stub.parentFile, stub.nameWithoutExtension + KaptStubLineInformation.KAPT_METADATA_EXTENSION)
            if (metadata.isFile) readPositions(metadata.readBytes()) else emptyList()
        }
        return mappings.sorted().joinToString("\n", prefix = "\n\n// LINE MAPPINGS\n")
    }

    private fun readPositions(data: ByteArray): List<String> = ObjectInputStream(ByteArrayInputStream(data)).use { input ->
        input.readInt()
        List(input.readInt()) {
            val fqName = input.readUTF()
            val path = input.readUTF()
            input.readBoolean()
            "$fqName: ${File(path).name}:${input.readInt()}"
        }
    }

    /**
     * The package parts of the module mapping written to the incremental data, which incremental compilation reads, as
     * `package: part` or `package: part (facade)` for multifile class parts.
     */
    private fun moduleMapping(module: TestModule, kaptContext: KaptContextForStubGeneration): String {
        if (DUMP_MODULE_MAPPING !in module.directives) return ""
        val files = kaptContext.outputFiles.asList().filter { File(it.relativePath).extension == ModuleMapping.MAPPING_FILE_EXT }
        return files.joinToString("") { file ->
            val mapping = ModuleMapping.loadModuleMapping(file.asByteArray(), file.relativePath, DeserializationConfiguration.Default) {
                assertions.fail { "Incompatible metadata version of ${file.relativePath}: $it" }
            }
            val parts = mapping.packageFqName2Parts.values.flatMap { packageParts ->
                packageParts.parts.map { part ->
                    val facade = packageParts.getMultifileFacadeName(part)?.let { " ($it)" }.orEmpty()
                    "${packageParts.packageFqName.ifEmpty { "<root>" }}: $part$facade"
                }
            }
            parts.sorted().joinToString("\n", prefix = "\n\n// MODULE MAPPING: ${file.relativePath}\n")
        }
    }

    /**
     * The expect and actual files reported to incremental compilation, which recompiles them together, as `expect -> actual`, or as
     * `expect -> <lenient stub>` if the actual is a placeholder generated in lenient mode.
     */
    private fun expectActualFiles(module: TestModule, kaptContext: KaptContextForStubGeneration): String {
        if (DUMP_EXPECT_ACTUAL_FILES !in module.directives) return ""
        val tracker = kaptContext.configuration.expectActualTracker as RecordingExpectActualTracker
        val pairs = tracker.expectToActualFiles.map { [expect, actual] -> "${expect.name} -> ${actual.name}" } +
                tracker.expectFilesOfLenientStubs.map { "${it.name} -> <lenient stub>" }
        return pairs.sorted().joinToString("\n", prefix = "\n\n// EXPECT/ACTUAL FILES\n")
    }

    private fun KaptStubConverterHandler.checkErrors(
        module: TestModule,
        kaptContext: KaptContextForStubGeneration,
        actualStubs: String,
    ) {
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

        val expectedErrors = module.directives[EXPECTED_ERROR].sorted()
        if (expectedErrors.isNotEmpty() && actualErrorsStr.isEmpty()) {
            assertions.fail { "Kapt finished successfully but errors were expected." }
        } else if (expectedErrors.isEmpty() && actualErrorsStr.isNotEmpty()) {
            assertions.fail { "There were errors during kapt:\n$actualErrorsStr\n\nStubs:\n\n$actualStubs" }
        } else {
            val expectedErrorsStr = expectedErrors.joinToString(System.lineSeparator()) { it.toDirectiveView() }
            assertions.assertEquals(expectedErrorsStr, actualErrorsStr) { "Expected error matching failed" }
        }
    }

    private fun String.toDirectiveView(): String = "// ${EXPECTED_ERROR.name}: $this"

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}
}
