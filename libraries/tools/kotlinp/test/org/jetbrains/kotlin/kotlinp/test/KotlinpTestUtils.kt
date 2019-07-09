/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kotlinp.test

import com.intellij.openapi.Disposable
import junit.framework.TestCase.assertEquals
import kotlinx.metadata.jvm.KotlinClassMetadata
import kotlinx.metadata.jvm.KotlinModuleMetadata
import org.jetbrains.kotlin.checkers.setupLanguageVersionSettingsForCompilerTests
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.codegen.GenerationUtils
import org.jetbrains.kotlin.jvm.compiler.AbstractLoadJavaTest
import org.jetbrains.kotlin.kotlinp.Kotlinp
import org.jetbrains.kotlin.kotlinp.KotlinpSettings
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestJdkKind
import java.io.File
import kotlin.test.fail

fun compileAndPrintAllFiles(file: File, disposable: Disposable, tmpdir: File, compareWithTxt: Boolean, readWriteAndCompare: Boolean) {
    val main = StringBuilder()
    val afterVisitors = StringBuilder()
    val afterNodes = StringBuilder()

    val kotlinp = Kotlinp(KotlinpSettings(isVerbose = true))

    compile(file, disposable, tmpdir) { outputFile ->
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

    if (readWriteAndCompare) {
        assertEquals("Metadata is different after transformation with visitors.", main.toString(), afterVisitors.toString())
        assertEquals("Metadata is different after transformation with nodes.", main.toString(), afterNodes.toString())
    }
}

private fun compile(file: File, disposable: Disposable, tmpdir: File, forEachOutputFile: (File) -> Unit) {
    val content = file.readText()
    val configuration = KotlinTestUtils.newConfiguration(ConfigurationKind.ALL, TestJdkKind.MOCK_JDK)
    AbstractLoadJavaTest.updateConfigurationWithDirectives(content, configuration)
    val environment = KotlinCoreEnvironment.createForTests(disposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
    setupLanguageVersionSettingsForCompilerTests(content, environment)
    val ktFile = KotlinTestUtils.createFile(file.name, content, environment.project)
    GenerationUtils.compileFileTo(ktFile, environment, tmpdir)

    for (outputFile in tmpdir.walkTopDown().sortedBy { it.nameWithoutExtension }) {
        if (outputFile.isFile) {
            forEachOutputFile(outputFile)
        }
    }
}

private fun StringBuilder.appendFileName(file: File) {
    appendln("// ${file.invariantSeparatorsPath}")
    appendln("// ------------------------------------------")
}

// Reads the class file and writes it back with *Writer visitors.
// The resulting class file should be the same from the point of view of any metadata reader, including kotlinp
// (the exact bytes may differ though, because there are multiple ways to encode the same metadata)
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
            KotlinClassMetadata.Class.Writer().apply(classFile.toKmClass()::accept).write()
        is KotlinClassMetadata.FileFacade ->
            KotlinClassMetadata.FileFacade.Writer().apply(classFile.toKmPackage()::accept).write()
        is KotlinClassMetadata.SyntheticClass ->
            KotlinClassMetadata.SyntheticClass.Writer().apply { classFile.toKmLambda()?.accept(this) }.write()
        is KotlinClassMetadata.MultiFileClassPart ->
            KotlinClassMetadata.MultiFileClassPart.Writer().apply(classFile.toKmPackage()::accept).write(classFile.facadeClassName)
        else -> classFile
    }

private fun transformModuleFileWithReadWriteVisitors(moduleFile: KotlinModuleMetadata): KotlinModuleMetadata =
    KotlinModuleMetadata.Writer().apply(moduleFile::accept).write()

private fun transformModuleFileWithNodes(moduleFile: KotlinModuleMetadata): KotlinModuleMetadata =
    KotlinModuleMetadata.Writer().apply(moduleFile.toKmModule()::accept).write()
