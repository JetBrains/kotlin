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
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.jvm.compiler.AbstractLoadJavaTest
import org.jetbrains.kotlin.kotlinp.Kotlinp
import org.jetbrains.kotlin.kotlinp.KotlinpSettings
import org.jetbrains.kotlin.load.java.JavaTypeEnhancementState
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.util.KtTestUtil
import java.io.File
import kotlin.test.fail

fun compileAndPrintAllFiles(
    file: File,
    disposable: Disposable,
    tmpdir: File,
    compareWithTxt: Boolean,
    readWriteAndCompare: Boolean,
    useK2: Boolean
) {
    if (useK2 && InTextDirectivesUtils.findStringWithPrefixes(file.readText(), "// IGNORE K2") != null) {
        return
    }

    val main = StringBuilder()
    val afterVisitors = StringBuilder()
    val afterNodes = StringBuilder()

    val kotlinp = Kotlinp(KotlinpSettings(isVerbose = true, sortDeclarations = true))

    @OptIn(UnstableMetadataApi::class)
    compile(file, disposable, tmpdir, useK2) { outputFile ->
        when (outputFile.extension) {
            "kotlin_module" -> {
                val moduleFile = kotlinp.readModuleFile(outputFile)!!
                val transformedWithVisitors = transformModuleFileWithReadWriteVisitors(moduleFile)
                val transformedWithNodes = transformModuleFileWithNodes(moduleFile)

                for ((sb, moduleFileToRender) in listOf(
                    main to moduleFile, afterVisitors to transformedWithVisitors, afterNodes to transformedWithNodes
                )) {
                    sb.appendFileName(outputFile.relativeTo(tmpdir))
                    sb.append(kotlinp.renderModuleFile(moduleFileToRender))
                }
            }
            "class" -> {
                val classFile = kotlinp.readClassFile(outputFile)!!
                val classFile2 = transformClassFileWithReadWriteVisitors(classFile)
                val classFile3 = transformClassFileWithNodes(classFile)

                for ((sb, classFileToRender) in listOf(
                    main to classFile, afterVisitors to classFile2, afterNodes to classFile3
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
        assertEquals("Metadata is different after transformation with visitors.", main.toString(), afterVisitors.toString())
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

// Reads the class file and writes it back with *Writer visitors.
// The resulting class file should be the same from the point of view of any metadata reader, including kotlinp
// (the exact bytes may differ though, because there are multiple ways to encode the same metadata)
@Suppress("DEPRECATION")
private fun transformClassFileWithReadWriteVisitors(classFile: KotlinClassMetadata): KotlinClassMetadata =
    when (classFile) {
        is KotlinClassMetadata.Class -> KotlinClassMetadata.Class.Writer().apply(classFile::accept).write()
        is KotlinClassMetadata.FileFacade -> KotlinClassMetadata.FileFacade.Writer().apply(classFile::accept).write()
        is KotlinClassMetadata.SyntheticClass -> {
            val writer = KotlinClassMetadata.SyntheticClass.Writer()
            if (classFile.isLambda) {
                classFile.accept(writer)
            }
            writer.write()
        }
        is KotlinClassMetadata.MultiFileClassFacade -> KotlinClassMetadata.MultiFileClassFacade.Writer().write(classFile.partClassNames)
        is KotlinClassMetadata.MultiFileClassPart ->
            KotlinClassMetadata.MultiFileClassPart.Writer().apply(classFile::accept).write(classFile.facadeClassName)
        else -> classFile
    }

// Reads the class file and writes it back with KmClass/KmFunction/... elements.
private fun transformClassFileWithNodes(classFile: KotlinClassMetadata): KotlinClassMetadata =
    when (classFile) {
        is KotlinClassMetadata.Class ->
            KotlinClassMetadata.writeClass(classFile.toKmClass())
        is KotlinClassMetadata.FileFacade ->
            KotlinClassMetadata.writeFileFacade(classFile.toKmPackage())
        is KotlinClassMetadata.SyntheticClass ->
            classFile.toKmLambda()?.let { KotlinClassMetadata.writeLambda(it) } ?: KotlinClassMetadata.writeSyntheticClass()
        is KotlinClassMetadata.MultiFileClassPart ->
            KotlinClassMetadata.writeMultiFileClassPart(classFile.toKmPackage(), classFile.facadeClassName)
        else -> classFile
    }

@Suppress("DEPRECATION") // We're testing that reading/writing with KmNodes is identical to direct
@OptIn(UnstableMetadataApi::class)
private fun transformModuleFileWithReadWriteVisitors(moduleFile: KotlinModuleMetadata): KotlinModuleMetadata =
    KotlinModuleMetadata.Writer().apply(moduleFile::accept).write()

@OptIn(UnstableMetadataApi::class)
private fun transformModuleFileWithNodes(moduleFile: KotlinModuleMetadata): KotlinModuleMetadata =
    KotlinModuleMetadata.write(moduleFile.toKmModule())
