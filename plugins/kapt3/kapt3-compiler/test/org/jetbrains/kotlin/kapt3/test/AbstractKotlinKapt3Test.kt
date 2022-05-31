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

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.psi.PsiManager
import com.sun.tools.javac.comp.CompileStates
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit
import com.sun.tools.javac.util.JCDiagnostic
import com.sun.tools.javac.util.Log
import junit.framework.ComparisonFailure
import org.jetbrains.kotlin.base.kapt3.DetectMemoryLeaksMode
import org.jetbrains.kotlin.base.kapt3.KaptFlag
import org.jetbrains.kotlin.base.kapt3.KaptOptions
import org.jetbrains.kotlin.checkers.utils.CheckerTestUtil
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.codegen.ClassBuilderMode
import org.jetbrains.kotlin.codegen.CodegenTestFiles
import org.jetbrains.kotlin.codegen.GenerationUtils
import org.jetbrains.kotlin.codegen.OriginCollectingClassBuilderFactory
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.kapt.base.test.JavaKaptContextTest
import org.jetbrains.kotlin.kapt3.Kapt3ComponentRegistrar.KaptComponentContributor
import org.jetbrains.kotlin.kapt3.KaptContextForStubGeneration
import org.jetbrains.kotlin.kapt3.base.KaptContext
import org.jetbrains.kotlin.kapt3.base.doAnnotationProcessing
import org.jetbrains.kotlin.kapt3.base.javac.KaptJavaLogBase
import org.jetbrains.kotlin.kapt3.base.parseJavaFiles
import org.jetbrains.kotlin.kapt3.javac.KaptJavaFileObject
import org.jetbrains.kotlin.kapt3.prettyPrint
import org.jetbrains.kotlin.kapt3.stubs.ClassFileToSourceStubConverter
import org.jetbrains.kotlin.kapt3.util.MessageCollectorBackedKaptLogger
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.resolve.jvm.extensions.PartialAnalysisHandlerExtension
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.util.trimTrailingWhitespacesAndAddNewlineAtEOF
import org.jetbrains.kotlin.utils.PathUtil
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.util.*
import com.sun.tools.javac.util.List as JavacList

abstract class AbstractKotlinKapt3Test : KotlinKapt3TestBase() {
    companion object {
        const val FILE_SEPARATOR = "\n\n////////////////////\n\n"
        val ERR_BYTE_STREAM = ByteArrayOutputStream()
        private val ERR_PRINT_STREAM = PrintStream(ERR_BYTE_STREAM)

        val messageCollector = PrintingMessageCollector(ERR_PRINT_STREAM, MessageRenderer.PLAIN_FULL_PATHS, false)
    }

    override fun tearDown() {
        ERR_BYTE_STREAM.reset()
        super.tearDown()
    }

    private fun createTempJavaFile(prefix: String, text: String): File =
        FileUtil.createTempFile(prefix, ".java", false).apply {
            writeText(text)
        }

    override fun loadMultiFiles(files: List<TestFile>) {
        val project = myEnvironment.project
        val psiManager = PsiManager.getInstance(project)

        val tmpDir = tmpDir("kaptTest")

        val ktFiles = ArrayList<KtFile>(files.size)
        for (file in files.sorted()) {
            if (file.name.endsWith(".kt")) {
                // `rangesToDiagnosticNames` parameter is not-null only for diagnostic tests, it's using for lazy diagnostics
                val content = CheckerTestUtil.parseDiagnosedRanges(file.content, ArrayList(0), null)
                val tmpKtFile = File(tmpDir, file.name).apply { writeText(content) }
                val virtualFile = StandardFileSystems.local().findFileByPath(tmpKtFile.path) ?: error("Can't find ${file.name}")
                ktFiles.add(psiManager.findFile(virtualFile) as? KtFile ?: error("Can't load ${file.name}"))
            }
        }

        myFiles = CodegenTestFiles.create(ktFiles)
    }

    override fun doMultiFileTest(wholeFile: File, files: List<TestFile>) {
        val javaSourceRoots = listOfNotNull(writeJavaFiles(files))
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.ALL, files, TestJdkKind.MOCK_JDK, *javaSourceRoots.toTypedArray())

        addAnnotationProcessingRuntimeLibrary(myEnvironment)

        val project = myEnvironment.project

        val javacOptions = wholeFile.getOptionValues("JAVAC_OPTION").associate { opt ->
            val (key, value) = opt.split('=').map { it.trim() }.also { assert(it.size == 2) }
            key to value
        }

        val options = KaptOptions.Builder().apply {
            projectBaseDir = project.basePath?.let { File(it) }
            compileClasspath.addAll(PathUtil.getJdkClassesRootsFromCurrentJre() + PathUtil.kotlinPathsForIdeaPlugin.stdlibPath)

            sourcesOutputDir = tmpDir("kaptRunner")
            classesOutputDir = sourcesOutputDir
            stubsOutputDir = sourcesOutputDir
            incrementalDataOutputDir = sourcesOutputDir

            this.javacOptions.putAll(javacOptions)
            flags.addAll(kaptFlagsToAdd)
            flags.removeAll(kaptFlagsToRemove.toSet())

            detectMemoryLeaks = DetectMemoryLeaksMode.NONE
        }.build()

        val analysisExtension = object : PartialAnalysisHandlerExtension() {
            override val analyzeDefaultParameterValues: Boolean
                get() = options[KaptFlag.DUMP_DEFAULT_PARAMETER_VALUES]
        }

        AnalysisHandlerExtension.registerExtension(project, analysisExtension)
        StorageComponentContainerContributor.registerExtension(project, KaptComponentContributor(analysisExtension))

        loadMultiFiles(files)

        val txtFile = File(wholeFile.parentFile, wholeFile.nameWithoutExtension + ".txt")
        val classBuilderFactory = OriginCollectingClassBuilderFactory(ClassBuilderMode.KAPT3)
        val generationState = GenerationUtils.compileFiles(myFiles.psiFiles, myEnvironment, classBuilderFactory)

        val logger = MessageCollectorBackedKaptLogger(isVerbose = true, isInfoAsWarnings = false, messageCollector = messageCollector)

        var kaptContext: KaptContext? = null

        try {
            kaptContext = KaptContextForStubGeneration(
                options, true, logger,
                generationState.project, generationState.bindingContext, classBuilderFactory.compiledClasses,
                classBuilderFactory.origins, generationState
            )

            val javaFiles = files
                .filter { it.name.lowercase().endsWith(".java") }
                .map { createTempJavaFile(it.name.substringBeforeLast('.'), it.content) }

            check(kaptContext, javaFiles, txtFile, wholeFile)
        } finally {
            kaptContext?.close()
        }
    }

    protected fun convert(
        kaptContext: KaptContextForStubGeneration,
        javaFiles: List<File>,
        generateNonExistentClass: Boolean
    ): JavacList<JCCompilationUnit> {
        val converter = ClassFileToSourceStubConverter(kaptContext, generateNonExistentClass)

        val kaptStubs = converter.convert()
        val convertedFiles = kaptStubs.map { stub ->
            val sourceFile = createTempJavaFile("stub", stub.file.prettyPrint(kaptContext.context))
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
                // In our test, such a path will be temporary, so the comparison against it will lead to flaky tests.
                tree.sourcefile = KaptJavaFileObject(tree, tree.defs.firstIsInstance(), actualFile)
            }

            return parsedJavaFiles
        } finally {
            kaptContext.compiler.log.nerrors = oldErrorCount
        }
    }

    protected fun File.getRawOptionValues(name: String) = this.useLines { lines ->
        lines.filter { it.startsWith("// $name") }.toList()
    }

    private fun File.getOptionValues(name: String) = getRawOptionValues(name).map { it.drop("// ".length + name.length).trim() }

    protected abstract fun check(
        kaptContext: KaptContextForStubGeneration,
        javaFiles: List<File>,
        txtFile: File,
        wholeFile: File
    )
}

open class AbstractClassFileToSourceStubConverterTest : AbstractKotlinKapt3Test(), CustomJdkTestLauncher {
    companion object {
        private const val KOTLIN_METADATA_GROUP = "[a-z0-9]+ = (\\{.+?\\}|[0-9]+)"
        private val KOTLIN_METADATA_REGEX = "@kotlin\\.Metadata\\(($KOTLIN_METADATA_GROUP)(, $KOTLIN_METADATA_GROUP)*\\)".toRegex()

        private const val EXPECTED_ERROR = "EXPECTED_ERROR"

        internal fun removeMetadataAnnotationContents(s: String): String {
            return s.replace(KOTLIN_METADATA_REGEX, "@kotlin.Metadata()")
        }

        @JvmStatic
        fun main(args: Array<String>) {
            if (args.size != 2) error("2 argument expected, ${args.size} passed")

            val test = when (args[0]) {
                TargetBackend.JVM.name -> AbstractClassFileToSourceStubConverterTest()
                TargetBackend.JVM_IR.name -> AbstractIrClassFileToSourceStubConverterTest()
                else -> throw UnsupportedOperationException(args[0])
            }

            try {
                test.setUp()
                test.doTest(args[1])
            } finally {
                test.tearDown()
            }
        }
    }

    // This is to suppress "AssertionFailedError: No tests found"
    fun testSuppressWarning() {}

    override fun doTest(filePath: String) {
        val testFile = File(filePath)

        kaptFlagsToAdd.add(KaptFlag.MAP_DIAGNOSTIC_LOCATIONS)

        addOrRemoveFlag(KaptFlag.CORRECT_ERROR_TYPES, testFile)
        if (isFlagEnabled("STRICT_MODE", testFile)) {
            kaptFlagsToAdd.add(KaptFlag.STRICT)
        }
        addOrRemoveFlag(KaptFlag.STRIP_METADATA, testFile)
        addOrRemoveFlag(KaptFlag.KEEP_KDOC_COMMENTS_IN_STUBS, testFile)

        super.doTest(filePath)
        doTestWithJdk11(AbstractClassFileToSourceStubConverterTest::class.java, backend.name, filePath)
    }

    override fun check(kaptContext: KaptContextForStubGeneration, javaFiles: List<File>, txtFile: File, wholeFile: File) {
        val generateNonExistentClass = isFlagEnabled("NON_EXISTENT_CLASS", wholeFile)
        val validate = !isFlagEnabled("NO_VALIDATION", wholeFile)
        val expectedErrors = wholeFile.getRawOptionValues(EXPECTED_ERROR).sorted()

        val convertedFiles = convert(kaptContext, javaFiles, generateNonExistentClass)

        kaptContext.javaLog.interceptorData.files = convertedFiles.associateBy { it.sourceFile }
        if (validate) kaptContext.compiler.enterTrees(convertedFiles)

        val actualRaw = convertedFiles
            .sortedBy { it.sourceFile.name }
            .joinToString(FILE_SEPARATOR) { it.prettyPrint(kaptContext.context) }

        val actual = StringUtil.convertLineSeparators(actualRaw.trim { it <= ' ' })
            .trimTrailingWhitespacesAndAddNewlineAtEOF()
            .let { removeMetadataAnnotationContents(it) }

        if (kaptContext.compiler.shouldStop(CompileStates.CompileState.ENTER)) {
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
                .map { "// $EXPECTED_ERROR$it" }
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

        checkTxtAccordingToBackend(txtFile, actual)
    }
}

abstract class AbstractKotlinKaptContextTest : AbstractKotlinKapt3Test() {
    override fun doTest(filePath: String) {
        kaptFlagsToAdd.add(KaptFlag.CORRECT_ERROR_TYPES)
        kaptFlagsToAdd.add(KaptFlag.STRICT)
        kaptFlagsToAdd.add(KaptFlag.MAP_DIAGNOSTIC_LOCATIONS)
        super.doTest(filePath)
    }

    override fun check(kaptContext: KaptContextForStubGeneration, javaFiles: List<File>, txtFile: File, wholeFile: File) {
        val compilationUnits = convert(kaptContext, javaFiles, generateNonExistentClass = false)
        kaptContext.doAnnotationProcessing(
            emptyList(),
            listOf(JavaKaptContextTest.simpleProcessor()),
            additionalSources = compilationUnits
        )

        val stubJavaFiles = kaptContext.options.sourcesOutputDir.walkTopDown().filter { it.isFile && it.extension == "java" }
        val actualRaw = stubJavaFiles.sortedBy { it.name }.joinToString(FILE_SEPARATOR) { it.name + ":\n\n" + it.readText() }
        val actual = StringUtil.convertLineSeparators(actualRaw.trim { it <= ' ' }).trimTrailingWhitespacesAndAddNewlineAtEOF()
        KotlinTestUtils.assertEqualsToFile(txtFile, actual)
    }
}

private fun addAnnotationProcessingRuntimeLibrary(environment: KotlinCoreEnvironment) {
    environment.apply {
        val runtimeLibrary = File(PathUtil.kotlinPathsForCompiler.libPath, "kotlin-annotation-processing-runtime.jar")
        updateClasspath(listOf(JvmClasspathRoot(runtimeLibrary)))
    }
}
