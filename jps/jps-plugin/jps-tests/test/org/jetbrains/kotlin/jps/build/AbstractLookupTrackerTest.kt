/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.build

import com.intellij.testFramework.RunAll
import com.intellij.testFramework.UsefulTestCase
import com.intellij.util.ThrowableRunnable
import org.jetbrains.kotlin.TestWithWorkingDir
import org.jetbrains.kotlin.build.JvmSourceRoot
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.compilerRunner.*
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.incremental.components.LookupInfo
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.isKotlinFile
import org.jetbrains.kotlin.incremental.js.*
import org.jetbrains.kotlin.incremental.makeModuleFile
import org.jetbrains.kotlin.incremental.testingUtils.*
import org.jetbrains.kotlin.incremental.utils.TestLookupTracker
import org.jetbrains.kotlin.incremental.utils.TestMessageCollector
import org.jetbrains.kotlin.jps.build.fixtures.EnableICFixture
import org.jetbrains.kotlin.jps.incremental.createTestingCompilerEnvironment
import org.jetbrains.kotlin.jps.incremental.runJSCompiler
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.kotlinPathsForDistDirectoryForTests
import org.jetbrains.kotlin.utils.JsMetadataVersion
import org.jetbrains.kotlin.utils.PathUtil
import java.io.*

abstract class AbstractJvmLookupTrackerTest : AbstractLookupTrackerTest() {

    private val sourceToOutputMapping = hashMapOf<File, MutableSet<File>>()

    override fun setUp() {
        super.setUp()
        sourceToOutputMapping.clear()
        System.getProperties()
            .setProperty("kotlin.jps.classPrefixesToLoadByParent", "kotlin.") // for debugging tests with in-process compiler
    }

    override fun markDirty(removedAndModifiedSources: Iterable<File>) {
        for (sourceFile in removedAndModifiedSources) {
            val outputs = sourceToOutputMapping.remove(sourceFile) ?: continue
            for (output in outputs) {
                output.delete()
            }
        }
    }

    override fun processCompilationResults(outputItemsCollector: OutputItemsCollectorImpl, services: Services) {
        for ((sourceFiles, outputFile) in outputItemsCollector.outputs) {
            if (outputFile.extension == "kotlin_module") continue

            for (sourceFile in sourceFiles) {
                val outputsForSource = sourceToOutputMapping.getOrPut(sourceFile) { hashSetOf() }
                outputsForSource.add(outputFile)
            }
        }
    }

    open fun configureAdditionalArgs(args: K2JVMCompilerArguments) {
        args.useFirLT = false
    }

    override fun runCompiler(filesToCompile: Iterable<File>, env: JpsCompilerEnvironment): Any? {
        val moduleFile = makeModuleFile(
            name = "test",
            isTest = true,
            outputDir = outDir,
            sourcesToCompile = filesToCompile.toList(),
            commonSources = emptyList(),
            javaSourceRoots = listOf(JvmSourceRoot(srcDir, null)),
            classpath = listOf(outDir, PathUtil.kotlinPathsForDistDirectoryForTests.stdlibPath).filter { it.exists() },
            friendDirs = emptyList()
        )

        val args = K2JVMCompilerArguments().apply {
            disableDefaultScriptingPlugin = true
            buildFile = moduleFile.canonicalPath
            reportOutputFiles = true
        }
        configureAdditionalArgs(args)
        val argsArray = ArgumentUtils.convertArgumentsToStringList(args).toTypedArray()

        try {
            val stream = ByteArrayOutputStream()
            val out = PrintStream(stream)
            val exitCode = CompilerRunnerUtil.invokeExecMethod(K2JVMCompiler::class.java.name, argsArray, env, out)
            val reader = BufferedReader(StringReader(stream.toString()))
            CompilerOutputParser.parseCompilerMessagesFromReader(env.messageCollector, reader, env.outputItemsCollector)

            return exitCode
        } finally {
            moduleFile.delete()
        }
    }
}

abstract class AbstractK1JvmLookupTrackerTest : AbstractJvmLookupTrackerTest() {

    override var filterBuiltins = true
    override var distinguishPackageAndClassLookups = false

    override fun setUp() {
        super.setUp()
        optionalVariantSuffix = OptionalVariantSuffix.K1
    }

    override fun configureAdditionalArgs(args: K2JVMCompilerArguments) {
        args.languageVersion = "1.9"
    }

    override val buildLogFinder: BuildLogFinder
        get() = BuildLogFinder(isGradleEnabled = false, isFirEnabled = false)
}

abstract class AbstractJsKlibLookupTrackerTest : AbstractJsLookupTrackerTest() {
    override fun configureAdditionalArgs(args: K2JSCompilerArguments) {
        args.irProduceKlibDir = true
        args.irOnly = true
        args.outputDir = outDir.normalize().absolutePath
        args.moduleName = "out"
    }
}

abstract class AbstractJsLookupTrackerTest : AbstractLookupTrackerTest() {
    private var header: ByteArray? = null
    private val packageParts: MutableMap<File, TranslationResultValue> = hashMapOf()
    private val serializedIrFiles: MutableMap<File, IrTranslationResultValue> = hashMapOf()

    override fun setUp() {
        super.setUp()
        header = null
        packageParts.clear()
        serializedIrFiles.clear()
    }

    override fun Services.Builder.registerAdditionalServices() {
        if (header != null) {
            register(
                IncrementalDataProvider::class.java,
                IncrementalDataProviderImpl(
                    headerMetadata = header!!,
                    compiledPackageParts = packageParts,
                    metadataVersion = JsMetadataVersion.INSTANCE.toArray(),
                    packageMetadata = emptyMap(), // TODO pass correct metadata
                    serializedIrFiles = serializedIrFiles
                )
            )
        }

        register(IncrementalResultsConsumer::class.java, IncrementalResultsConsumerImpl())
    }

    override fun markDirty(removedAndModifiedSources: Iterable<File>) {
        removedAndModifiedSources.forEach {
            packageParts.remove(it)
            serializedIrFiles.remove(it)
        }
    }

    override fun processCompilationResults(outputItemsCollector: OutputItemsCollectorImpl, services: Services) {
        val incrementalResults = services[IncrementalResultsConsumer::class.java] as IncrementalResultsConsumerImpl
        header = incrementalResults.headerMetadata
        packageParts.putAll(incrementalResults.packageParts)
        serializedIrFiles.putAll(incrementalResults.irFileData)
    }

    protected open val jsStdlibFile: File
        get() = PathUtil.kotlinPathsForDistDirectoryForTests.jsStdLibKlibPath

    protected open fun configureAdditionalArgs(args: K2JSCompilerArguments) {
    }

    override fun runCompiler(filesToCompile: Iterable<File>, env: JpsCompilerEnvironment): Any? {
        val args = K2JSCompilerArguments().apply {
            val libPaths = arrayListOf(jsStdlibFile.absolutePath) + (libraries ?: "").split(File.pathSeparator)
            libraries = libPaths.joinToString(File.pathSeparator)
            reportOutputFiles = true
            freeArgs = filesToCompile.map { it.canonicalPath }
            useFirLT = false
        }
        configureAdditionalArgs(args)
        return runJSCompiler(args, env)
    }
}

abstract class AbstractLookupTrackerTest : TestWithWorkingDir() {
    private val DECLARATION_KEYWORDS = listOf("interface", "class", "enum class", "object", "fun", "operator fun", "val", "var")
    private val DECLARATION_STARTS_WITH = DECLARATION_KEYWORDS.map { it + " " }

    // ignore KDoc like comments which starts with `/**`, example: /** text */
    private val COMMENT_WITH_LOOKUP_INFO = "/\\*[^*]+\\*/".toRegex()

    protected lateinit var srcDir: File
    protected lateinit var outDir: File
    private val enableICFixture = EnableICFixture()
    protected var optionalVariantSuffix: OptionalVariantSuffix = OptionalVariantSuffix.K2
    protected open var filterBuiltins = false
    protected open var distinguishPackageAndClassLookups = true

    override fun setUp() {
        super.setUp()
        srcDir = File(workingDir, "src").apply { mkdirs() }
        outDir = File(workingDir, "out")
        enableICFixture.setUp()
    }

    override fun tearDown() {
        RunAll(
            ThrowableRunnable { enableICFixture.tearDown() },
            ThrowableRunnable { super.tearDown() }
        ).run()
    }

    protected abstract fun markDirty(removedAndModifiedSources: Iterable<File>)
    protected abstract fun processCompilationResults(outputItemsCollector: OutputItemsCollectorImpl, services: Services)
    protected abstract fun runCompiler(filesToCompile: Iterable<File>, env: JpsCompilerEnvironment): Any?

    protected open val buildLogFinder: BuildLogFinder
        get() = BuildLogFinder(isGradleEnabled = false)

    fun doTest(path: String) {
        val sb = StringBuilder()
        fun StringBuilder.indentln(string: String) {
            appendLine("  $string")
        }

        fun CompilerOutput.logOutput(stepName: String) {
            sb.appendLine("==== $stepName ====")

            sb.appendLine("Compiling files:")
            for (compiledFile in compiledFiles.sortedBy { it.canonicalPath }) {
                val lookupsFromFile = lookups[compiledFile]
                val lookupStatus = when {
                    lookupsFromFile == null -> "(unknown)"
                    lookupsFromFile.isEmpty() -> "(no lookups)"
                    else -> ""
                }
                val relativePath = compiledFile.toRelativeString(workingDir).replace("\\", "/")
                sb.indentln("$relativePath$lookupStatus")
            }

            sb.appendLine("Exit code: $exitCode")
            errors.forEach(sb::indentln)

            sb.appendLine()
        }

        val testDir = File(path)
        val workToOriginalFileMap = HashMap(copyTestSources(testDir, srcDir, filePrefix = "", optionalVariantSuffix = optionalVariantSuffix))
        var dirtyFiles = srcDir.walk().filterTo(HashSet()) { it.isKotlinFile(setOf("kt", "kts")) }
        val steps = getModificationsToPerform(
            testDir,
            moduleNames = null,
            allowNoFilesWithSuffixInTestData = true,
            touchPolicy = TouchPolicy.CHECKSUM,
            optionalVariantSuffix = optionalVariantSuffix
        )
            .filter { it.isNotEmpty() }

        val filesToLookups = arrayListOf<Map<File, List<LookupInfo>>>()
        fun CompilerOutput.originalFilesToLookups() =
            compiledFiles.associateBy({ workToOriginalFileMap[it]!! }, { lookups[it] ?: emptyList() })

        make(dirtyFiles).apply {
            logOutput("INITIAL BUILD")
            filesToLookups.add(originalFilesToLookups())

        }
        for ((i, modifications) in steps.withIndex()) {
            dirtyFiles = modifications.mapNotNullTo(HashSet()) { it.perform(workingDir, workToOriginalFileMap) }
            make(dirtyFiles).apply {
                logOutput("STEP ${i + 1}")
                filesToLookups.add(originalFilesToLookups())
            }
        }

        buildLogFinder.findBuildLog(testDir)?.let { expectedBuildLog ->
            UsefulTestCase.assertSameLinesWithFile(expectedBuildLog.canonicalPath, sb.toString())
        }

        assertEquals(steps.size + 1, filesToLookups.size)
        for ((i, lookupsAtStepI) in filesToLookups.withIndex()) {
            val step = if (i == 0) "INITIAL BUILD" else "STEP $i"
            for ((file, lookups) in lookupsAtStepI) {
                checkLookupsInFile(step, file, lookups)
            }
        }
    }

    private class CompilerOutput(
        val exitCode: String,
        val errors: List<String>,
        val compiledFiles: Iterable<File>,
        val lookups: Map<File, List<LookupInfo>>,
    )

    private fun make(filesToCompile: Iterable<File>): CompilerOutput {
        filesToCompile.forEach {
            it.writeText(it.readText().replace(COMMENT_WITH_LOOKUP_INFO, ""))
        }

        markDirty(filesToCompile)
        val lookupTracker = TestLookupTracker()
        val messageCollector = TestMessageCollector()
        val outputItemsCollector = OutputItemsCollectorImpl()
        val services = Services.Builder().run {
            register(LookupTracker::class.java, lookupTracker)
            registerAdditionalServices()
            build()
        }
        val environment = createTestingCompilerEnvironment(messageCollector, outputItemsCollector, services)
        val exitCode = runCompiler(filesToCompile, environment) as? ExitCode
        if (exitCode == ExitCode.OK) {
            processCompilationResults(outputItemsCollector, environment.services)
        }

        val lookups = lookupTracker.lookups.groupBy { File(it.filePath) }
        val lookupsFromCompiledFiles = filesToCompile.associateWith { (lookups[it] ?: emptyList()) }
        return CompilerOutput(exitCode.toString(), messageCollector.errors, filesToCompile, lookupsFromCompiledFiles)
    }

    protected open fun Services.Builder.registerAdditionalServices() {}

    private fun checkLookupsInFile(step: String, expectedFile: File, lookupsFromFile: List<LookupInfo>) {
        val text = expectedFile.readText().replace(COMMENT_WITH_LOOKUP_INFO, "")
        val lines = text.lines().toMutableList()

        for ((line, lookupsFromLine) in lookupsFromFile.groupBy { it.position.line }) {
            val columnToLookups = lookupsFromLine.groupBy { it.position.column }.toList().sortedBy { it.first }

            val lineContent = lines[line - 1]
            val parts = ArrayList<CharSequence>(columnToLookups.size * 2)

            var start = 0

            for ((column, lookupsFromColumn) in columnToLookups) {
                val end = column - 1
                parts.add(lineContent.subSequence(start, end))

                lookupsFromColumn.mapNotNullTo(sortedSetOf()) { lookupInfo ->
                    if (filterBuiltins &&
                        ClassId(FqName(lookupInfo.scopeFqName), Name.identifier(lookupInfo.name)) in StandardClassIds.allBuiltinTypes
                    ) {
                        null
                    } else {
                        val rest = lineContent.substring(end)

                        val name =
                            when {
                                rest.startsWith(lookupInfo.name) || // same name
                                        rest.startsWith("$" + lookupInfo.name) || // backing field
                                        DECLARATION_STARTS_WITH.any { rest.startsWith(it) } // it's declaration
                                -> ""
                                else -> "(" + lookupInfo.name + ")"
                            }

                        val prefix =
                            if (distinguishPackageAndClassLookups) lookupInfo.scopeKind.toString()[0].lowercaseChar().toString()
                            else "p"
                        prefix + ":" + lookupInfo.scopeFqName.let { it.ifEmpty { "<root>" } } + name
                    }
                }.takeIf { it.isNotEmpty() }
                    ?.joinToString(separator = " ", prefix = "/*", postfix = "*/")
                    ?.also {
                        parts.add(it)
                    }

                start = end
            }

            lines[line - 1] = parts.joinToString("") + lineContent.subSequence(start, lineContent.length)
        }

        val actual = lines.joinToString("\n")
        KotlinTestUtils.assertEqualsToFile("Lookups do not match after $step", expectedFile, actual)
    }
}
