/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.kapt3.test

import com.intellij.openapi.util.text.StringUtil
import com.sun.tools.javac.comp.CompileStates
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit
import com.sun.tools.javac.util.Log
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.codegen.CodegenTestCase
import org.jetbrains.kotlin.codegen.CodegenTestUtil
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.kapt3.*
import org.jetbrains.kotlin.kapt3.stubs.ClassFileToSourceStubConverter
import org.jetbrains.kotlin.kapt3.util.KaptLogger
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.resolve.jvm.extensions.PartialAnalysisHandlerExtension
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.util.trimTrailingWhitespacesAndAddNewlineAtEOF
import com.sun.tools.javac.util.List as JavacList
import java.io.File
import java.nio.file.Files

abstract class AbstractKotlinKapt3Test : CodegenTestCase() {
    companion object {
        val FILE_SEPARATOR = "\n\n////////////////////\n\n"
        val messageCollector = PrintingMessageCollector(System.err, MessageRenderer.PLAIN_FULL_PATHS, false)
    }

    override fun doMultiFileTest(wholeFile: File, files: List<TestFile>, javaFilesDir: File?) {
        val javaSources = javaFilesDir?.let { arrayOf(it) } ?: emptyArray()

        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.ALL, *javaSources)

        // Use light analysis mode in tests
        AnalysisHandlerExtension.registerExtension(myEnvironment.project, PartialAnalysisHandlerExtension())

        loadMultiFiles(files)

        val txtFile = File(wholeFile.parentFile, wholeFile.nameWithoutExtension + ".txt")
        val classBuilderFactory = Kapt3BuilderFactory()
        val factory = CodegenTestUtil.generateFiles(myEnvironment, myFiles, classBuilderFactory)
        val typeMapper = factory.generationState.typeMapper

        val logger = KaptLogger(isVerbose = true, messageCollector = messageCollector)
        val kaptContext = KaptContext(logger, classBuilderFactory.compiledClasses,
                                      classBuilderFactory.origins, processorOptions = emptyMap())
        try {
            check(kaptContext, typeMapper, txtFile, wholeFile)
        } finally {
            kaptContext.close()
        }
    }

    protected fun convert(kaptRunner: KaptContext, typeMapper: KotlinTypeMapper, generateNonExistentClass: Boolean): JavacList<JCCompilationUnit> {
        val converter = ClassFileToSourceStubConverter(kaptRunner, typeMapper, generateNonExistentClass)
        return converter.convert()
    }

    protected abstract fun check(
            kaptRunner: KaptContext,
            typeMapper: KotlinTypeMapper,
            txtFile: File,
            wholeFile: File)
}

abstract class AbstractClassFileToSourceStubConverterTest : AbstractKotlinKapt3Test() {
    override fun check(kaptRunner: KaptContext, typeMapper: KotlinTypeMapper, txtFile: File, wholeFile: File) {
        val generateNonExistentClass = wholeFile.useLines { lines -> lines.any { it.trim() == "// NON_EXISTENT_CLASS" } }
        val javaFiles = convert(kaptRunner, typeMapper, generateNonExistentClass)
        kaptRunner.compiler.enterTrees(javaFiles)

        val actualRaw = javaFiles.joinToString (FILE_SEPARATOR)
        val actual = StringUtil.convertLineSeparators(actualRaw.trim({ it <= ' ' })).trimTrailingWhitespacesAndAddNewlineAtEOF()

        if (kaptRunner.compiler.shouldStop(CompileStates.CompileState.ENTER)) {
            Log.instance(kaptRunner.context).flush()
            error("There were errors during analysis. See errors above. Stubs:\n\n$actual")
        }
        KotlinTestUtils.assertEqualsToFile(txtFile, actual)
    }
}

abstract class AbstractKotlinKaptContextTest : AbstractKotlinKapt3Test() {
    override fun check(kaptRunner: KaptContext, typeMapper: KotlinTypeMapper, txtFile: File, wholeFile: File) {
        val compilationUnits = convert(kaptRunner, typeMapper, generateNonExistentClass = false)
        val sourceOutputDir = Files.createTempDirectory("kaptRunner").toFile()
        try {
            kaptRunner.doAnnotationProcessing(emptyList(), listOf(JavaKaptContextTest.simpleProcessor()),
                                              compileClasspath = emptyList(), annotationProcessingClasspath = emptyList(),
                                              sourcesOutputDir = sourceOutputDir, classesOutputDir = sourceOutputDir,
                                              additionalSources = compilationUnits, withJdk = true)

            val javaFiles = sourceOutputDir.walkTopDown().filter { it.isFile && it.extension == "java" }
            val actualRaw = javaFiles.sortedBy { it.name }.joinToString(FILE_SEPARATOR) { it.name + ":\n\n" + it.readText() }
            val actual = StringUtil.convertLineSeparators(actualRaw.trim({ it <= ' ' })).trimTrailingWhitespacesAndAddNewlineAtEOF()
            KotlinTestUtils.assertEqualsToFile(txtFile, actual)
        } finally {
            sourceOutputDir.deleteRecursively()
        }
    }
}