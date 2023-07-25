/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kotlinp.test

import com.intellij.openapi.Disposable
import junit.framework.TestCase.assertEquals
import kotlinx.metadata.jvm.KotlinClassMetadata
import kotlinx.metadata.jvm.KotlinModuleMetadata
import kotlinx.metadata.jvm.UnstableMetadataApi
import org.jetbrains.kotlin.checkers.setupLanguageVersionSettingsForCompilerTests
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.codegen.GenerationUtils
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.jvm.compiler.AbstractLoadJavaTest
import org.jetbrains.kotlin.kotlinp.Kotlinp
import org.jetbrains.kotlin.kotlinp.KotlinpSettings
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.util.KtTestUtil
import java.io.File
import kotlin.test.fail

private const val IGNORE_K1_DIRECTIVE = "// IGNORE K1"
private const val IGNORE_K2_DIRECTIVE = "// IGNORE K2"

fun compareAllFiles(
    file: File,
    disposable: Disposable,
    tmpdir: File,
    compareWithTxt: Boolean,
    readWriteAndCompare: Boolean,
    useK2: Boolean,
) {
    val directive = if (useK2) IGNORE_K2_DIRECTIVE else IGNORE_K1_DIRECTIVE
    val isMuted = InTextDirectivesUtils.findStringWithPrefixes(file.readText(), directive) != null
    try {
        compileAndPrintAllFiles(file, disposable, tmpdir, compareWithTxt, readWriteAndCompare, useK2)
    } catch (e: Throwable) {
        if (isMuted) return
        throw e
    }
    if (isMuted) {
        throw AssertionError("Looks like this test can be unmuted. Remove the \"$directive\" directive.")
    }
}

private fun compileAndPrintAllFiles(
    file: File,
    disposable: Disposable,
    tmpdir: File,
    compareWithTxt: Boolean,
    readWriteAndCompare: Boolean,
    useK2: Boolean,
) {
    val main = StringBuilder()
    val afterNodes = StringBuilder()

    val kotlinp = Kotlinp(KotlinpSettings(isVerbose = true, sortDeclarations = true))

    @OptIn(UnstableMetadataApi::class)
    compile(file, disposable, tmpdir, useK2) { outputFile ->
        when (outputFile.extension) {
            "kotlin_module" -> {
                val moduleFile = kotlinp.readModuleFile(outputFile)!!
                val transformedWithNodes = KotlinModuleMetadata.read(transformModuleFileWithNodes(moduleFile))

                for ((sb, moduleFileToRender) in listOf(
                    main to moduleFile, afterNodes to transformedWithNodes
                )) {
                    sb.appendFileName(outputFile.relativeTo(tmpdir))
                    sb.append(kotlinp.renderModuleFile(moduleFileToRender))
                }
            }
            "class" -> {
                val metadata = kotlinp.readClassFile(outputFile)
                val classFile = kotlinp.readMetadata(metadata)
                val classFile2 = KotlinClassMetadata.read(transformClassFileWithNodes(metadata, classFile))

                for ((sb, classFileToRender) in listOf(
                    main to classFile, afterNodes to classFile2
                )) {
                    sb.appendFileName(outputFile.relativeTo(tmpdir))
                    sb.append(kotlinp.renderClassFile(classFileToRender))
                }
            }
            else -> fail("Unknown file: $outputFile")
        }
    }

    if (compareWithTxt) {
        KotlinTestUtils.assertEqualsToFile(File(file.path.replace(".kt", ".txt")), main.toString())
    }

    if (readWriteAndCompare && InTextDirectivesUtils.findStringWithPrefixes(file.readText(), "// NO_READ_WRITE_COMPARE") == null) {
        assertEquals("Metadata is different after transformation with nodes.", main.toString(), afterNodes.toString())
    }
}

private fun compile(file: File, disposable: Disposable, tmpdir: File, useK2: Boolean, forEachOutputFile: (File) -> Unit) {
    val content = file.readText()
    val configuration = KotlinTestUtils.newConfiguration(ConfigurationKind.ALL, TestJdkKind.MOCK_JDK)
    configuration.put(JVMConfigurationKeys.IR, true)
    configuration.put(CommonConfigurationKeys.USE_FIR, useK2)
    AbstractLoadJavaTest.updateConfigurationWithDirectives(content, configuration)
    val environment = KotlinCoreEnvironment.createForTests(disposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
    setupLanguageVersionSettingsForCompilerTests(content, environment)
    val ktFile = KtTestUtil.createFile(file.name, content, environment.project)
    GenerationUtils.compileFileTo(ktFile, environment, tmpdir)

    for (outputFile in tmpdir.walkTopDown().sortedBy { it.nameWithoutExtension }) {
        if (outputFile.isFile) {
            forEachOutputFile(outputFile)
        }
    }
}

private fun StringBuilder.appendFileName(file: File) {
    appendLine("// ${file.invariantSeparatorsPath}")
    appendLine("// ------------------------------------------")
}

// Reads the class file and writes it back with KmClass/KmFunction/... elements.
private fun transformClassFileWithNodes(metadata: Metadata, classFile: KotlinClassMetadata): Metadata =
    when (classFile) {
        is KotlinClassMetadata.Class ->
            KotlinClassMetadata.writeClass(classFile.kmClass)
        is KotlinClassMetadata.FileFacade ->
            KotlinClassMetadata.writeFileFacade(classFile.kmPackage)
        is KotlinClassMetadata.SyntheticClass ->
            classFile.kmLambda?.let { KotlinClassMetadata.writeLambda(it) } ?: KotlinClassMetadata.writeSyntheticClass()
        is KotlinClassMetadata.MultiFileClassPart ->
            KotlinClassMetadata.writeMultiFileClassPart(classFile.kmPackage, classFile.facadeClassName)
        is KotlinClassMetadata.MultiFileClassFacade -> KotlinClassMetadata.writeMultiFileClassFacade(classFile.partClassNames)
        is KotlinClassMetadata.Unknown -> metadata
    }

@OptIn(UnstableMetadataApi::class)
private fun transformModuleFileWithNodes(moduleFile: KotlinModuleMetadata): ByteArray =
    KotlinModuleMetadata.write(moduleFile.kmModule)
