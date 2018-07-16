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
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.psi.PsiManager
import com.sun.tools.javac.comp.CompileStates
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit
import com.sun.tools.javac.util.JCDiagnostic
import com.sun.tools.javac.util.Log
import junit.framework.ComparisonFailure
import org.jetbrains.kotlin.checkers.CheckerTestUtil
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.kapt3.*
import org.jetbrains.kotlin.kapt3.Kapt3ComponentRegistrar.KaptComponentContributor
import org.jetbrains.kotlin.kapt3.base.KaptContext
import org.jetbrains.kotlin.kapt3.base.KaptPaths
import org.jetbrains.kotlin.kapt3.base.doAnnotationProcessing
import org.jetbrains.kotlin.kapt3.base.javac.KaptJavaLog
import org.jetbrains.kotlin.kapt3.base.parseJavaFiles
import org.jetbrains.kotlin.kapt3.javac.KaptJavaFileObject
import org.jetbrains.kotlin.kapt3.stubs.ClassFileToSourceStubConverter
import org.jetbrains.kotlin.kapt3.util.MessageCollectorBackedKaptLogger
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.resolve.jvm.extensions.PartialAnalysisHandlerExtension
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.util.trimTrailingWhitespacesAndAddNewlineAtEOF
import org.jetbrains.kotlin.utils.PathUtil
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.nio.file.Files
import java.util.*
import com.sun.tools.javac.util.List as JavacList


abstract class AbstractKotlinKapt3Test : CodegenTestCase() {
    companion object {
        const val FILE_SEPARATOR = "\n\n////////////////////\n\n"
        val ERR_BYTE_STREAM = ByteArrayOutputStream()
        private val ERR_PRINT_STREAM = PrintStream(ERR_BYTE_STREAM)

        val messageCollector = PrintingMessageCollector(ERR_PRINT_STREAM, MessageRenderer.PLAIN_FULL_PATHS, false)
    }

    private val tempFiles = mutableListOf<File>()

    override fun tearDown() {
        ERR_BYTE_STREAM.reset()
        super.tearDown()
    }

    private fun createTempFile(prefix: String, suffix: String, text: String): File {
        return File.createTempFile(prefix, suffix).apply {
            writeText(text)
            tempFiles += this
        }
    }

    override fun loadMultiFiles(files: List<TestFile>) {
        val project = myEnvironment.project
        val psiManager = PsiManager.getInstance(project)

        val tmpDir = Files.createTempDirectory("kaptTest").toFile()
        tempFiles += tmpDir

        val ktFiles = ArrayList<KtFile>(files.size)
        for (file in files.sorted()) {
            if (file.name.endsWith(".kt")) {
                val content = CheckerTestUtil.parseDiagnosedRanges(file.content, ArrayList<CheckerTestUtil.DiagnosedRange>(0))
                val tmpKtFile = File(tmpDir, file.name).apply { writeText(content) }
                val virtualFile = StandardFileSystems.local().findFileByPath(tmpKtFile.path) ?: error("Can't find ${file.name}")
                ktFiles.add(psiManager.findFile(virtualFile) as? KtFile ?: error("Can't load ${file.name}"))
            }
        }

        myFiles = CodegenTestFiles.create(ktFiles)
    }

    override fun doMultiFileTest(wholeFile: File, files: List<TestFile>, javaFilesDir: File?) {
        val javaSources = javaFilesDir?.let { arrayOf(it) } ?: emptyArray()

        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.ALL, *javaSources)
        addAnnotationProcessingRuntimeLibrary(myEnvironment)

        // Use light analysis mode in tests
        val project = myEnvironment.project
        AnalysisHandlerExtension.registerExtension(project, PartialAnalysisHandlerExtension())
        StorageComponentContainerContributor.registerExtension(project, KaptComponentContributor())

        loadMultiFiles(files)

        val txtFile = File(wholeFile.parentFile, wholeFile.nameWithoutExtension + ".txt")
        val classBuilderFactory = OriginCollectingClassBuilderFactory(ClassBuilderMode.KAPT3)
        val generationState = GenerationUtils.compileFiles(myFiles.psiFiles, myEnvironment, classBuilderFactory)

        val logger = MessageCollectorBackedKaptLogger(isVerbose = true, messageCollector = messageCollector)

        val javacOptions = wholeFile.getOptionValues("JAVAC_OPTION")
            .map { opt ->
                val (key, value) = opt.split('=').map { it.trim() }.also { assert(it.size == 2) }
                key to value
            }.toMap()

        var javaFiles: List<File>? = null
        var kaptContext: KaptContext? = null

        try {
            val sourceOutputDir = Files.createTempDirectory("kaptRunner").toFile()

            val paths = KaptPaths(
                generationState.project.basePath?.let(::File),
                compileClasspath = PathUtil.getJdkClassesRootsFromCurrentJre() + PathUtil.kotlinPathsForIdeaPlugin.stdlibPath,
                annotationProcessingClasspath = emptyList(), javaSourceRoots = emptyList(),
                sourcesOutputDir = sourceOutputDir, classFilesOutputDir = sourceOutputDir,
                stubsOutputDir = sourceOutputDir, incrementalDataOutputDir = sourceOutputDir
            )

            kaptContext = KaptContextForStubGeneration(
                paths, true,
                logger, generationState.project, generationState.bindingContext, classBuilderFactory.compiledClasses,
                classBuilderFactory.origins, generationState, mapDiagnosticLocations = true,
                processorOptions = emptyMap(), javacOptions = javacOptions
            )

            javaFiles = files
                .filter { it.name.toLowerCase().endsWith(".java") }
                .map { createTempFile(it.name.substringBeforeLast('.'), ".java", it.content) }

            check(kaptContext, javaFiles, txtFile, wholeFile)
        } catch (e: Throwable) {
            throw RuntimeException(e)
        } finally {
            javaFiles?.forEach { it.delete() }
            tempFiles.forEach { if (it.isFile) it.delete() else it.deleteRecursively() }
            tempFiles.clear()
            kaptContext?.close()
        }
    }

    protected fun convert(
        kaptContext: KaptContextForStubGeneration,
        javaFiles: List<File>,
        generateNonExistentClass: Boolean,
        correctErrorTypes: Boolean,
        strictMode: Boolean
    ): JavacList<JCCompilationUnit> {
        val converter = ClassFileToSourceStubConverter(kaptContext, generateNonExistentClass, correctErrorTypes, strictMode)

        val kaptStubs = converter.convert()
        val convertedFiles = kaptStubs.map { stub ->
            val sourceFile = createTempFile("stub", ".java", stub.file.prettyPrint(kaptContext.context))
            stub.writeMetadataIfNeeded(forSource = sourceFile)
            sourceFile
        }

        val allJavaFiles = javaFiles + convertedFiles

        // A workaround needed for Javac to parse files correctly even if errors were already reported
        // If nerrors > 0, "parseFiles()" returns the empty list
        val oldErrorCount = kaptContext.compiler.log.nerrors
        kaptContext.compiler.log.nerrors = 0

        try {
            val parsedJavaFiles = kaptContext.parseJavaFiles(allJavaFiles)

            for (tree in parsedJavaFiles) {
                val actualFile = File(tree.sourceFile.toUri())

                // By default, JavaFileObject.getName() returns the absolute path to the file.
                // In our test, such a path will be temporary, so the comparision against it will lead to flaky tests.
                tree.sourcefile = KaptJavaFileObject(tree, tree.defs.firstIsInstance(), actualFile)
            }

            return parsedJavaFiles
        } finally {
            kaptContext.compiler.log.nerrors = oldErrorCount
        }
    }

    protected fun File.isOptionSet(name: String) = this.useLines { lines -> lines.any { it.trim() == "// $name" } }

    protected fun File.getRawOptionValues(name: String) = this.useLines { lines ->
        lines.filter { it.startsWith("// $name") }.toList()
    }

    protected fun File.getOptionValues(name: String) = getRawOptionValues(name).map { it.drop("// ".length + name.length).trim() }

    protected abstract fun check(
        kaptContext: KaptContextForStubGeneration,
        javaFiles: List<File>,
        txtFile: File,
            wholeFile: File)
}

open class AbstractClassFileToSourceStubConverterTest : AbstractKotlinKapt3Test(), Java9TestLauncher {
    companion object {
        private val KOTLIN_METADATA_GROUP = "[a-z0-9]+ = (\\{.+?\\}|[0-9]+)"
        private val KOTLIN_METADATA_REGEX = "@kotlin\\.Metadata\\(($KOTLIN_METADATA_GROUP)(, $KOTLIN_METADATA_GROUP)*\\)".toRegex()

        private val EXPECTED_ERROR = "EXPECTED_ERROR"

        internal fun removeMetadataAnnotationContents(s: String): String {
            return s.replace(KOTLIN_METADATA_REGEX, "@kotlin.Metadata()")
        }

        @JvmStatic
        fun main(args: Array<String>) {
            if (args.isEmpty()) error("1 argument expected, 0 passed")
            val test = AbstractClassFileToSourceStubConverterTest()
            try {
                test.setUp()
                test.doTest(args[0])
            } finally {
                test.tearDown()
            }
        }
    }

    // This is to suppress "AssertionFailedError: No tests found"
    fun testSuppressWarning() {}

    override fun doTest(filePath: String) {
        super.doTest(filePath)
        doTestWithJdk9(AbstractClassFileToSourceStubConverterTest::class.java, filePath)
    }

    override fun check(kaptContext: KaptContextForStubGeneration, javaFiles: List<File>, txtFile: File, wholeFile: File) {
        val generateNonExistentClass = wholeFile.isOptionSet("NON_EXISTENT_CLASS")
        val correctErrorTypes = wholeFile.isOptionSet("CORRECT_ERROR_TYPES")
        val validate = !wholeFile.isOptionSet("NO_VALIDATION")
        val strictMode = wholeFile.isOptionSet("STRICT_MODE")
        val expectedErrors = wholeFile.getRawOptionValues(EXPECTED_ERROR).sorted()

        val convertedFiles = convert(kaptContext, javaFiles, generateNonExistentClass, correctErrorTypes, strictMode)

        kaptContext.javaLog.interceptorData.files = convertedFiles.map { it.sourceFile to it }.toMap()
        if (validate) kaptContext.compiler.enterTrees(convertedFiles)

        val actualRaw = convertedFiles
            .sortedBy { it.sourceFile.name }
            .joinToString(FILE_SEPARATOR) { it.prettyPrint(kaptContext.context) }

        val actual = StringUtil.convertLineSeparators(actualRaw.trim({ it <= ' ' }))
            .trimTrailingWhitespacesAndAddNewlineAtEOF()
            .let { removeMetadataAnnotationContents(it) }

        if (kaptContext.compiler.shouldStop(CompileStates.CompileState.ENTER)) {
            val log = Log.instance(kaptContext.context) as KaptJavaLog

            val actualErrors = log.reportedDiagnostics
                .filter { it.type == JCDiagnostic.DiagnosticType.ERROR }
                .map {
                    // Unfortunately, we can't use the file name as it can contain temporary prefix
                    val name = it.source?.name?.substringAfterLast("/") ?: ""
                    val kind = when (name.substringAfterLast(".").toLowerCase()) {
                        "kt" -> "kotlin"
                        "java" -> "java"
                        else -> "other"
                    }

                    val javaLocation = "($kind:${it.lineNumber}:${it.columnNumber}) "
                    javaLocation + it.getMessage(Locale.US).lines().first()
                }
                .map { "// " + EXPECTED_ERROR + it }
                .sorted()

            log.flush()

            val lineSeparator = System.getProperty("line.separator")
            val actualErrorsStr = actualErrors.joinToString(lineSeparator)

            if (expectedErrors.isEmpty()) {
                error("There were errors during analysis:\n$actualErrorsStr\n\nStubs:\n\n$actual")
            } else {
                val expectedErrorsStr = expectedErrors.joinToString(lineSeparator)
                if (expectedErrorsStr != actualErrorsStr) {
                    System.err.println(ERR_BYTE_STREAM.toString("UTF8"))
                    throw ComparisonFailure("Expected error matching failed", expectedErrorsStr, actualErrorsStr)
                }
            }
        }
        KotlinTestUtils.assertEqualsToFile(txtFile, actual)
    }
}

abstract class AbstractKotlinKaptContextTest : AbstractKotlinKapt3Test() {
    override fun check(kaptContext: KaptContextForStubGeneration, javaFiles: List<File>, txtFile: File, wholeFile: File) {
        val compilationUnits = convert(
            kaptContext, javaFiles,
            generateNonExistentClass = false, correctErrorTypes = true, strictMode = false
        )

        kaptContext.doAnnotationProcessing(
            emptyList(),
            listOf(JavaKaptContextTest.simpleProcessor()),
            additionalSources = compilationUnits
        )

        val stubJavaFiles = kaptContext.paths.sourcesOutputDir.walkTopDown().filter { it.isFile && it.extension == "java" }
        val actualRaw = stubJavaFiles.sortedBy { it.name }.joinToString(FILE_SEPARATOR) { it.name + ":\n\n" + it.readText() }
        val actual = StringUtil.convertLineSeparators(actualRaw.trim({ it <= ' ' })).trimTrailingWhitespacesAndAddNewlineAtEOF()
        KotlinTestUtils.assertEqualsToFile(txtFile, actual)
    }
}

private fun addAnnotationProcessingRuntimeLibrary(environment: KotlinCoreEnvironment) {
    environment.apply {
        val runtimeLibrary = File(PathUtil.kotlinPathsForCompiler.libPath, "kotlin-annotation-processing-runtime.jar")
        updateClasspath(listOf(JvmClasspathRoot(runtimeLibrary)))
    }
}