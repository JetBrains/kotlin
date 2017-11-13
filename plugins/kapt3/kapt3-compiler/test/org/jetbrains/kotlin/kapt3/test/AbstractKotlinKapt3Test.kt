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
import junit.framework.TestCase
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
import org.jetbrains.kotlin.kapt3.javac.KaptJavaFileObject
import org.jetbrains.kotlin.kapt3.javac.KaptJavaLog
import org.jetbrains.kotlin.kapt3.parseJavaFiles
import org.jetbrains.kotlin.kapt3.stubs.ClassFileToSourceStubConverter
import org.jetbrains.kotlin.kapt3.util.KaptLogger
import org.jetbrains.kotlin.kapt3.util.isJava9OrLater
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.resolve.jvm.extensions.PartialAnalysisHandlerExtension
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.util.trimTrailingWhitespacesAndAddNewlineAtEOF
import org.jetbrains.kotlin.utils.PathUtil
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Files
import java.util.*
import java.util.concurrent.TimeUnit
import com.sun.tools.javac.util.List as JavacList

abstract class AbstractKotlinKapt3Test : CodegenTestCase() {
    companion object {
        val FILE_SEPARATOR = "\n\n////////////////////\n\n"
        val messageCollector = PrintingMessageCollector(System.err, MessageRenderer.PLAIN_FULL_PATHS, false)
    }

    private val tempFiles = mutableListOf<File>()

    private fun createTempFile(prefix: String, suffix: String, text: String): File {
        return File.createTempFile(prefix, suffix).apply {
            writeText(text)
            tempFiles += this
        }
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
                .map { createTempFile(it.name.substringBeforeLast('.'), ".java", it.content) }

        try {
            check(kaptContext, javaFiles, txtFile, wholeFile)
        } catch (e: Throwable) {
            throw RuntimeException(e)
        } finally {
            javaFiles.forEach { it.delete() }
            tempFiles.forEach { it.delete() }
            tempFiles.clear()
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

        val convertedTrees = converter.convert()
        val convertedFiles = convertedTrees.map { tree -> createTempFile("stub", ".java", tree.toString()) }

        val allJavaFiles = javaFiles + convertedFiles

        // A workaround needed for Javac to parse files correctly even if errors were already reported
        // If nerrors > 0, "parseFiles()" returns the empty list
        val oldErrorCount = kaptContext.compiler.log.nerrors
        kaptContext.compiler.log.nerrors = 0

        try {
            val parsedJavaFiles = kaptContext.parseJavaFiles(allJavaFiles)

            for (file in parsedJavaFiles) {
                // By default, JavaFileObject.getName() returns the absolute path to the file.
                // In our test, such a path will be temporary, so the comparision against it will lead to flaky tests.
                file.sourcefile = KaptJavaFileObject(file, file.defs.firstIsInstance())
            }

            return parsedJavaFiles
        } finally {
            kaptContext.compiler.log.nerrors = oldErrorCount
        }
    }

    protected abstract fun check(
            kaptContext: KaptContext<GenerationState>,
            javaFiles: List<File>,
            txtFile: File,
            wholeFile: File)
}

open class AbstractClassFileToSourceStubConverterTest : AbstractKotlinKapt3Test() {
    companion object {
        private val KOTLIN_METADATA_GROUP = "[a-z0-9]+ = (\\{.+?\\}|[0-9]+)"
        private val KOTLIN_METADATA_REGEX = "@kotlin\\.Metadata\\(($KOTLIN_METADATA_GROUP)(, $KOTLIN_METADATA_GROUP)*\\)".toRegex()
        private val KAPT_METADATA_REGEX = "@kapt\\.internal\\.KaptMetadata\\((value = )?\"[^(].*?\"\\)".toRegex()

        private val EXPECTED_ERROR = "EXPECTED_ERROR"

        internal fun removeMetadataAnnotationContents(s: String): String {
            return s.replace(KOTLIN_METADATA_REGEX, "@kotlin.Metadata()")
                    .replace(KAPT_METADATA_REGEX, "@kapt.internal.KaptMetadata()")
        }

        @JvmStatic
        fun main(args: Array<String>) {
            if (args.isEmpty()) error("1 argument expected, 0 passed")
            AbstractClassFileToSourceStubConverterTest().doTest(args[0])
        }
    }

    // This is to suppress "AssertionFailedError: No tests found"
    fun testSuppressWarning() {}

    override fun doTest(filePath: String) {
        super.doTest(filePath)

        if (!isJava9OrLater) {
            doTestWithJdk9(filePath)
        }
    }

    private fun doTestWithJdk9(filePath: String) {
        val jdk9Home = KotlinTestUtils.getJdk9HomeIfPossible() ?: run {
            println("JDK9 not found, the test was skipped")
            return
        }

        val javaExe = File(jdk9Home, "bin/java.exe").takeIf { it.exists() } ?: File(jdk9Home, "bin/java")
        assert(javaExe.exists()) { "Can't find 'java' executable in $jdk9Home" }

        val currentJavaHome = System.getProperty("java.home")
        val classpath = collectClasspath(AbstractClassFileToSourceStubConverterTest::class.java.classLoader)
                .filter { !it.path.startsWith(currentJavaHome) }

        val process = ProcessBuilder(
                javaExe.absolutePath,
                "--illegal-access=warn",
                "-ea",
                "-classpath",
                classpath.joinToString(File.pathSeparator),
                AbstractClassFileToSourceStubConverterTest::class.java.name,
                filePath
        ).inheritIO().start()

        process.waitFor(3, TimeUnit.MINUTES)
        if (process.exitValue() != 0) {
            throw AssertionError("Java 9 test process exited with exit code ${process.exitValue()} \n")
        }
    }

    private fun collectClasspath(classLoader: ClassLoader?): List<URL> = when (classLoader) {
        is URLClassLoader -> classLoader.urLs.asList() + collectClasspath(classLoader.parent)
        is ClassLoader -> collectClasspath(classLoader.parent)
        else -> emptyList()
    }

    override fun check(kaptContext: KaptContext<GenerationState>, javaFiles: List<File>, txtFile: File, wholeFile: File) {
        fun isOptionSet(name: String) = wholeFile.useLines { lines -> lines.any { it.trim() == "// $name" } }

        fun getOptionValues(name: String) = wholeFile.useLines { lines ->
            lines.filter { it.startsWith("// $name") }.toList()
        }

        val generateNonExistentClass = isOptionSet("NON_EXISTENT_CLASS")
        val correctErrorTypes = isOptionSet("CORRECT_ERROR_TYPES")
        val validate = !isOptionSet("NO_VALIDATION")
        val expectedErrors = getOptionValues(EXPECTED_ERROR).sorted()

        val convertedFiles = convert(kaptContext, javaFiles, generateNonExistentClass, correctErrorTypes)

        kaptContext.javaLog.interceptorData.files = convertedFiles.map { it.sourceFile to it }.toMap()
        if (validate) kaptContext.compiler.enterTrees(convertedFiles)

        val actualRaw = convertedFiles.sortedBy { it.sourceFile.name }.joinToString(FILE_SEPARATOR)
        val actual = StringUtil.convertLineSeparators(actualRaw.trim({ it <= ' ' }))
                .trimTrailingWhitespacesAndAddNewlineAtEOF()
                .let { removeMetadataAnnotationContents(it) }

        if (kaptContext.compiler.shouldStop(CompileStates.CompileState.ENTER)) {
            val log = Log.instance(kaptContext.context) as KaptJavaLog

            val actualErrors = log.reportedDiagnostics
                    .filter { it.type == JCDiagnostic.DiagnosticType.ERROR }
                    .map {
                        val location = it.subdiagnostics
                                .firstOrNull { it.getMessage(Locale.US).startsWith("Kotlin location:") }
                                ?.getMessage(Locale.US)

                        val javaLocation = "(${it.lineNumber};${it.columnNumber}) "
                        val message = javaLocation + it.getMessage(Locale.US).lines().first()
                        if (location != null) "$message ($location)" else message
                    }
                    .map { "// " + EXPECTED_ERROR + it }
                    .sorted()

            log.flush()

            if (expectedErrors.isEmpty()) {
                error("There were errors during analysis. See errors above. Stubs:\n\n$actual")
            } else {
                val lineSeparator = System.getProperty("line.separator")
                TestCase.assertEquals("Expected error matching failed",
                                      expectedErrors.joinToString(lineSeparator),
                                      actualErrors.joinToString(lineSeparator))
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