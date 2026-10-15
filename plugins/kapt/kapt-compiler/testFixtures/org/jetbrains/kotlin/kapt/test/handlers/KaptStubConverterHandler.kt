/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.test.handlers

import com.intellij.openapi.util.text.StringUtil
import com.sun.tools.javac.util.JCDiagnostic
import com.sun.tools.javac.util.Log
import org.jetbrains.kotlin.kapt.KaptContextForStubGeneration
import org.jetbrains.kotlin.kapt.base.javac.KaptJavaLogBase
import org.jetbrains.kotlin.kapt.test.KaptContextBinaryArtifact
import org.jetbrains.kotlin.kapt.test.KaptTestDirectives
import org.jetbrains.kotlin.kapt.test.KaptTestDirectives.EXPECTED_ERROR
import org.jetbrains.kotlin.kapt.test.KaptTestDirectives.INJECT_EXCEPTION_TYPE_ANNOTATION
import org.jetbrains.kotlin.kapt.test.KaptTestDirectives.INJECT_FIELD_TYPE_ANNOTATION
import org.jetbrains.kotlin.kapt.test.KaptTestDirectives.INJECT_PARAMETER_TYPE_ANNOTATION
import org.jetbrains.kotlin.kapt.test.KaptTestDirectives.NON_EXISTENT_CLASS
import org.jetbrains.kotlin.kapt.util.prettyPrint
import org.jetbrains.kotlin.test.directives.TestDumpDirectives
import org.jetbrains.kotlin.test.directives.assertEqualsToDump
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.util.trimTrailingWhitespacesAndAddNewlineAtEOF
import org.jetbrains.org.objectweb.asm.TypePath
import org.jetbrains.org.objectweb.asm.TypeReference
import org.jetbrains.org.objectweb.asm.tree.TypeAnnotationNode
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
        injectFieldTypeAnnotations(module, kaptContext)
        injectParameterTypeAnnotations(module, kaptContext)
        injectExceptionTypeAnnotations(module, kaptContext)

        val convertedFiles = convert(module, kaptContext, generateNonExistentClass)

        val actualRaw = convertedFiles
            .sortedBy { it.sourceFile.name }
            .joinToString(FILE_SEPARATOR) { it.prettyPrint(kaptContext.context, ::renderMetadata) }

        val actual = StringUtil.convertLineSeparators(actualRaw.trim { it <= ' ' })
            .trimTrailingWhitespacesAndAddNewlineAtEOF()
            .let { removeMetadataAnnotationContents(it) }

        checkErrors(module, kaptContext, actual)

        assertEqualsToDump(extension = "txt", actual)
    }

    private fun injectFieldTypeAnnotations(module: TestModule, kaptContext: KaptContextForStubGeneration) {
        for (directiveValue in module.directives[INJECT_FIELD_TYPE_ANNOTATION]) {
            val parts = directiveValue.split('|').map(String::trim)
            assertions.assertEquals(5, parts.size) {
                "Expected five pipe-separated values in ${INJECT_FIELD_TYPE_ANNOTATION.name}, got: '$directiveValue'"
            }
            val className = parts[0]
            val fieldName = parts[1]
            val typePathString = parts[2]
            val annotationDescriptor = parts[3]
            val visibility = parts[4]

            val clazz = kaptContext.compiledClassByName[className]
                ?: assertions.fail { "Class '$className' was not found for type annotation injection" }
            val field = clazz.fields.singleOrNull { it.name == fieldName }
                ?: assertions.fail { "Field '$fieldName' was not found exactly once in class '$className'" }
            val typePath = typePathString.takeUnless { it == "null" }?.let { TypePath.fromString(it) }
            val annotation = TypeAnnotationNode(
                TypeReference.newTypeReference(TypeReference.FIELD).value,
                typePath,
                annotationDescriptor,
            )

            when (visibility) {
                "visible" -> field.visibleTypeAnnotations = field.visibleTypeAnnotations.orEmpty().toMutableList().apply { add(annotation) }
                "invisible" -> field.invisibleTypeAnnotations = field.invisibleTypeAnnotations.orEmpty().toMutableList().apply { add(annotation) }
                else -> assertions.fail { "Unknown type annotation visibility '$visibility'" }
            }
        }
    }

    private fun injectParameterTypeAnnotations(module: TestModule, kaptContext: KaptContextForStubGeneration) {
        for (directiveValue in module.directives[INJECT_PARAMETER_TYPE_ANNOTATION]) {
            val parts = directiveValue.split('|').map(String::trim)
            assertions.assertEquals(6, parts.size) {
                "Expected six pipe-separated values in ${INJECT_PARAMETER_TYPE_ANNOTATION.name}, got: '$directiveValue'"
            }
            val className = parts[0]
            val methodName = parts[1]
            val parameterIndex = parts[2].toInt()
            val typePathString = parts[3]
            val annotationDescriptor = parts[4]
            val visibility = parts[5]

            val clazz = kaptContext.compiledClassByName[className]
                ?: assertions.fail { "Class '$className' was not found for type annotation injection" }
            val method = clazz.methods.singleOrNull { it.name == methodName }
                ?: assertions.fail { "Method '$methodName' was not found exactly once in class '$className'" }
            val typePath = typePathString.takeUnless { it == "null" }?.let { TypePath.fromString(it) }
            val annotation = TypeAnnotationNode(
                TypeReference.newFormalParameterReference(parameterIndex).value,
                typePath,
                annotationDescriptor,
            )

            when (visibility) {
                "visible" -> method.visibleTypeAnnotations = method.visibleTypeAnnotations.orEmpty().toMutableList().apply {
                    add(annotation)
                }
                "invisible" -> method.invisibleTypeAnnotations = method.invisibleTypeAnnotations.orEmpty().toMutableList().apply {
                    add(annotation)
                }
                else -> assertions.fail { "Unknown type annotation visibility '$visibility'" }
            }
        }
    }

    private fun injectExceptionTypeAnnotations(module: TestModule, kaptContext: KaptContextForStubGeneration) {
        for (directiveValue in module.directives[INJECT_EXCEPTION_TYPE_ANNOTATION]) {
            val parts = directiveValue.split('|').map(String::trim)
            assertions.assertEquals(6, parts.size) {
                "Expected six pipe-separated values in ${INJECT_EXCEPTION_TYPE_ANNOTATION.name}, got: '$directiveValue'"
            }
            val className = parts[0]
            val methodName = parts[1]
            val exceptionIndex = parts[2].toInt()
            val typePathString = parts[3]
            val annotationDescriptor = parts[4]
            val visibility = parts[5]

            val clazz = kaptContext.compiledClassByName[className]
                ?: assertions.fail { "Class '$className' was not found for type annotation injection" }
            val method = clazz.methods.singleOrNull { it.name == methodName }
                ?: assertions.fail { "Method '$methodName' was not found exactly once in class '$className'" }
            assertions.assertTrue(exceptionIndex in method.exceptions.indices) {
                "Exception index $exceptionIndex is out of bounds for '$className.$methodName'"
            }
            val typePath = typePathString.takeUnless { it == "null" }?.let { TypePath.fromString(it) }
            val annotation = TypeAnnotationNode(
                TypeReference.newExceptionReference(exceptionIndex).value,
                typePath,
                annotationDescriptor,
            )

            when (visibility) {
                "visible" -> method.visibleTypeAnnotations = method.visibleTypeAnnotations.orEmpty().toMutableList().apply {
                    add(annotation)
                }
                "invisible" -> method.invisibleTypeAnnotations = method.invisibleTypeAnnotations.orEmpty().toMutableList().apply {
                    add(annotation)
                }
                else -> assertions.fail { "Unknown type annotation visibility '$visibility'" }
            }
        }
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
