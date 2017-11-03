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
import com.sun.tools.javac.util.JCDiagnostic
import com.sun.tools.javac.util.Log
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.codegen.CodegenTestCase
import org.jetbrains.kotlin.codegen.GenerationUtils
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.kapt3.Kapt3BuilderFactory
import org.jetbrains.kotlin.kapt3.KaptContext
import org.jetbrains.kotlin.kapt3.doAnnotationProcessing
import org.jetbrains.kotlin.kapt3.javac.KaptJavaLog
import org.jetbrains.kotlin.kapt3.stubs.ClassFileToSourceStubConverter
import org.jetbrains.kotlin.kapt3.util.KaptLogger
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.resolve.jvm.extensions.PartialAnalysisHandlerExtension
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.util.trimTrailingWhitespacesAndAddNewlineAtEOF
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import java.nio.file.Files
import java.util.*
import com.sun.tools.javac.util.List as JavacList

abstract class AbstractKotlinKapt3Test : CodegenTestCase() {
    companion object {
        val FILE_SEPARATOR = "\n\n////////////////////\n\n"
        val messageCollector = PrintingMessageCollector(System.err, MessageRenderer.PLAIN_FULL_PATHS, false)
    }

    override fun doMultiFileTest(wholeFile: File, files: List<TestFile>, javaFilesDir: File?) {
        val javaSources = javaFilesDir?.let { arrayOf(it) } ?: emptyArray()

        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.ALL, *javaSources)
        addAnnotationProcessingRuntimeLibrary(myEnvironment)

        // Use light analysis mode in tests
        AnalysisHandlerExtension.registerExtension(myEnvironment.project, PartialAnalysisHandlerExtension())

        loadMultiFiles(files)

        val txtFile = File(wholeFile.parentFile, wholeFile.nameWithoutExtension + ".txt")
        val classBuilderFactory = Kapt3BuilderFactory()
        val generationState = GenerationUtils.compileFiles(myFiles.psiFiles, myEnvironment, classBuilderFactory)

        val logger = KaptLogger(isVerbose = true, messageCollector = messageCollector)
        val kaptContext = KaptContext(logger, generationState.project, generationState.bindingContext, classBuilderFactory.compiledClasses,
                                      classBuilderFactory.origins, generationState, processorOptions = emptyMap())

        val javaFiles = files
                .filter { it.name.toLowerCase().endsWith(".java") }
                .map { File.createTempFile(it.name.substringBeforeLast('.'), ".java").apply { writeText(it.content) } }

        try {
            check(kaptContext, javaFiles, txtFile, wholeFile)
        } finally {
            javaFiles.forEach { it.delete() }
            kaptContext.close()
        }
    }

    protected fun convert(
            kaptContext: KaptContext<GenerationState>,
            javaFiles: List<File>,
            generateNonExistentClass: Boolean,
            correctErrorTypes: Boolean
    ): JavacList<JCCompilationUnit> {
        val converter = ClassFileToSourceStubConverter(kaptContext, generateNonExistentClass, correctErrorTypes)
        val parsedJavaFiles = kaptContext.compiler.parseFiles(kaptContext.fileManager.getJavaFileObjectsFromFiles(javaFiles))
        return converter.convert().appendList(parsedJavaFiles)
    }

    protected abstract fun check(
            kaptContext: KaptContext<GenerationState>,
            javaFiles: List<File>,
            txtFile: File,
            wholeFile: File)
}

abstract class AbstractClassFileToSourceStubConverterTest : AbstractKotlinKapt3Test() {
    internal companion object {
        private val KOTLIN_METADATA_GROUP = "[a-z0-9]+ = (\\{.+?\\}|[0-9]+)"
        private val KOTLIN_METADATA_REGEX = "@kotlin\\.Metadata\\(($KOTLIN_METADATA_GROUP)(, $KOTLIN_METADATA_GROUP)*\\)".toRegex()

        fun removeMetadataAnnotationContents(s: String): String = s.replace(KOTLIN_METADATA_REGEX, "@kotlin.Metadata()")
    }

    override fun check(kaptContext: KaptContext<GenerationState>, javaFiles: List<File>, txtFile: File, wholeFile: File) {
        fun isOptionSet(name: String) = wholeFile.useLines { lines -> lines.any { it.trim() == "// $name" } }

        fun getOptionValues(name: String) = wholeFile.useLines { lines ->
            lines.filter { it.startsWith("// $name") }
                    .map { it.drop(name.length + 3).trim() }
                    .filter { it.isNotEmpty() }
                    .toList()
        }

        val generateNonExistentClass = isOptionSet("NON_EXISTENT_CLASS")
        val correctErrorTypes = isOptionSet("CORRECT_ERROR_TYPES")
        val validate = !isOptionSet("NO_VALIDATION")
        val expectedErrors = getOptionValues("EXPECTED_ERROR").sorted()

        val convertedFiles = convert(kaptContext, javaFiles, generateNonExistentClass, correctErrorTypes)

        kaptContext.javaLog.interceptorData.files = convertedFiles.map { it.sourceFile to it }.toMap()
        if (validate) kaptContext.compiler.enterTrees(convertedFiles)

        val actualRaw = convertedFiles.sortedBy { it.sourceFile.name }.joinToString (FILE_SEPARATOR)
        val actual = StringUtil.convertLineSeparators(actualRaw.trim({ it <= ' ' }))
                .trimTrailingWhitespacesAndAddNewlineAtEOF()
                .let { removeMetadataAnnotationContents(it) }

        if (kaptContext.compiler.shouldStop(CompileStates.CompileState.ENTER)) {
            val log = Log.instance(kaptContext.context) as KaptJavaLog

            val actualErrors = log.reportedDiagnostics
                    .filter { it.type == JCDiagnostic.DiagnosticType.ERROR }
                    .map { it.getMessage(Locale.US).lines().first() }
                    .sorted()

            log.flush()

            if (expectedErrors.isEmpty()) {
                error("There were errors during analysis. See errors above. Stubs:\n\n$actual")
            } else if (actualErrors != expectedErrors) {
                error("Expected error matching assertion. Expected: \n"
                      + expectedErrors.joinToString("\n") { "'$it'" }
                      + "\n, found: \n"
                      + actualErrors.joinToString("\n") { "'$it'" })
            }
        }
        KotlinTestUtils.assertEqualsToFile(txtFile, actual)
    }
}

abstract class AbstractKotlinKaptContextTest : AbstractKotlinKapt3Test() {
    override fun check(kaptContext: KaptContext<GenerationState>, javaFiles: List<File>, txtFile: File, wholeFile: File) {
        val compilationUnits = convert(kaptContext, javaFiles, generateNonExistentClass = false, correctErrorTypes = true)
        val sourceOutputDir = Files.createTempDirectory("kaptRunner").toFile()
        try {
            kaptContext.doAnnotationProcessing(emptyList(), listOf(JavaKaptContextTest.simpleProcessor()),
                                               compileClasspath = PathUtil.getJdkClassesRootsFromCurrentJre() + PathUtil.kotlinPathsForIdeaPlugin.stdlibPath,
                                               annotationProcessingClasspath = emptyList(), annotationProcessors = "",
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

private fun addAnnotationProcessingRuntimeLibrary(environment: KotlinCoreEnvironment) {
    environment.apply {
        val runtimeLibrary = File(PathUtil.kotlinPathsForCompiler.libPath, "kotlin-annotation-processing-runtime.jar")
        updateClasspath(listOf(JvmClasspathRoot(runtimeLibrary)))
    }
}