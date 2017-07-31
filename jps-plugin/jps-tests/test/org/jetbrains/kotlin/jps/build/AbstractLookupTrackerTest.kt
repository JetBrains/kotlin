/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.jps.build

import com.intellij.testFramework.UsefulTestCase
import com.intellij.util.containers.HashMap
import com.intellij.util.containers.StringInterner
import org.jetbrains.kotlin.TestWithWorkingDir
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.compilerRunner.*
import org.jetbrains.kotlin.config.IncrementalCompilation
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.incremental.components.LookupInfo
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.components.Position
import org.jetbrains.kotlin.incremental.components.ScopeKind
import org.jetbrains.kotlin.incremental.isKotlinFile
import org.jetbrains.kotlin.incremental.makeModuleFile
import org.jetbrains.kotlin.incremental.testingUtils.TouchPolicy
import org.jetbrains.kotlin.incremental.testingUtils.copyTestSources
import org.jetbrains.kotlin.incremental.testingUtils.getModificationsToPerform
import org.jetbrains.kotlin.incremental.utils.TestMessageCollector
import org.jetbrains.kotlin.incremental.js.IncrementalDataProvider
import org.jetbrains.kotlin.incremental.js.IncrementalResultsConsumer
import org.jetbrains.kotlin.incremental.js.IncrementalResultsConsumerImpl
import org.jetbrains.kotlin.jps.incremental.runJSCompiler
import org.jetbrains.kotlin.jps.incremental.createTestingCompilerEnvironment
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.*
import java.util.*

abstract class AbstractJvmLookupTrackerTest : AbstractLookupTrackerTest() {
    override fun runCompiler(filesToCompile: Iterable<File>, env: JpsCompilerEnvironment): Any? {
        val moduleFile = makeModuleFile(
                name = "test",
                isTest = true,
                outputDir = outDir,
                sourcesToCompile = filesToCompile.toList(),
                javaSourceRoots = listOf(srcDir),
                classpath = listOf(outDir).filter { it.exists() },
                friendDirs = emptyList()
        )

        val args = K2JVMCompilerArguments().apply {
            buildFile = moduleFile.canonicalPath
            reportOutputFiles = true
        }
        val argsArray = ArgumentUtils.convertArgumentsToStringList(args).toTypedArray()

        try {
            val stream = ByteArrayOutputStream()
            val out = PrintStream(stream)
            val exitCode = CompilerRunnerUtil.invokeExecMethod(K2JVMCompiler::class.java.name, argsArray, env, out)
            val reader = BufferedReader(StringReader(stream.toString()))
            CompilerOutputParser.parseCompilerMessagesFromReader(env.messageCollector, reader, env.outputItemsCollector)

            return exitCode
        }
        finally {
            moduleFile.delete()
        }
    }
}

abstract class AbstractJsLookupTrackerTest : AbstractLookupTrackerTest() {
    private lateinit var incrementalDataDir: File
    private lateinit var binaryTreesDir: File
    private lateinit var packagesMetadataDir: File
    private lateinit var headerMetadataFile: File

    override fun setUp() {
        super.setUp()
        incrementalDataDir = File(workingDir, "incremental-data")
        binaryTreesDir = File(incrementalDataDir, "binary-trees")
        packagesMetadataDir = File(incrementalDataDir, "packages-metadata")
        headerMetadataFile = File(incrementalDataDir, "header.metadata")
    }

    override fun Services.Builder.registerAdditionalServices() {
        if (incrementalDataDir.exists()) {
            register(IncrementalDataProvider::class.java, object : IncrementalDataProvider {
                override val headerMetadata: ByteArray
                    get() = headerMetadataFile.readBytes()
                override val packagePartsMetadata: List<ByteArray>
                    get() = packagesMetadataDir.walk().filter { it.isFile }.map { it.readBytes() }.toList()
                override val binaryTrees: List<ByteArray>
                    get() = binaryTreesDir.walk().filter { it.isFile }.map { it.readBytes() }.toList()
            })
        }

        register(IncrementalResultsConsumer::class.java, IncrementalResultsConsumerImpl())
    }

    override fun runCompiler(filesToCompile: Iterable<File>, env: JpsCompilerEnvironment): Any? {
        val args = K2JSCompilerArguments().apply {
            outputFile = File(outDir, "out.js").canonicalPath
            reportOutputFiles = true
            freeArgs.addAll(filesToCompile.map { it.canonicalPath })
        }
        val exitCode = runJSCompiler(args, env)

        if (exitCode != ExitCode.OK) return exitCode

        val incrementalResults = env.services.get(IncrementalResultsConsumer::class.java) as IncrementalResultsConsumerImpl
        incrementalResults.apply {
            packageParts.forEach {
                val relativePath = it.sourceFile.toRelativeString(srcDir)
                val treeFile = File(binaryTreesDir, relativePath + ".ast").apply { parentFile.mkdirs() }
                treeFile.writeBytes(it.binaryAst)

                val partProtoFile = File(packagesMetadataDir, relativePath + ".proto").apply { parentFile.mkdirs() }
                partProtoFile.writeBytes(it.proto)

                env.outputItemsCollector.outputs.apply {
                    val sources = listOf(it.sourceFile)
                    add(SimpleOutputItem(sources, treeFile))
                    add(SimpleOutputItem(sources, partProtoFile))
                }
            }

            headerMetadataFile.parentFile.mkdirs()
            headerMetadataFile.writeBytes(headerMetadata)
        }

        return exitCode
    }
}

abstract class AbstractLookupTrackerTest : TestWithWorkingDir() {
    private val DECLARATION_KEYWORDS = listOf("interface", "class", "enum class", "object", "fun", "operator fun", "val", "var")
    private val DECLARATION_STARTS_WITH = DECLARATION_KEYWORDS.map { it + " " }
    // ignore KDoc like comments which starts with `/**`, example: /** text */
    private val COMMENT_WITH_LOOKUP_INFO = "/\\*[^*]+\\*/".toRegex()

    private fun removeCommentsCommentsWithLookupInfo(files: Iterable<File>) {
        files.forEach {
            val content = it.readText()
            it.writeText(content.replace(COMMENT_WITH_LOOKUP_INFO, ""))
        }
    }

    protected lateinit var srcDir: File
    protected lateinit var outDir: File
    private var isICEnabledBackup: Boolean = false

    override fun setUp() {
        super.setUp()
        srcDir = File(workingDir, "src").apply { mkdirs() }
        outDir = File(workingDir, "out")
        isICEnabledBackup = IncrementalCompilation.isEnabled()
        IncrementalCompilation.setIsEnabled(true)
    }

    override fun tearDown() {
        IncrementalCompilation.setIsEnabled(isICEnabledBackup)
        super.tearDown()
    }

    protected abstract fun runCompiler(filesToCompile: Iterable<File>, env: JpsCompilerEnvironment): Any?

    fun doTest(path: String) {
        val sb = StringBuilder()
        fun StringBuilder.indentln(string: String) {
            appendln("  $string")
        }
        fun CompilerOutput.logOutput(stepName: String) {
            sb.appendln("==== $stepName ====")

            sb.appendln("Compiling files:")
            for (compiledFile in compiledFiles.sortedBy { it.canonicalPath }) {
                val lookupCount = lookupsCount[compiledFile]
                val lookupStatus = when {
                    lookupCount == 0 -> "(no lookups)"
                    lookupCount != null && lookupCount > 0 -> ""
                    else -> "(unknown)"
                }
                sb.indentln("${compiledFile.toRelativeString(workingDir)}$lookupStatus")
            }

            sb.appendln("Exit code: $exitCode")
            errors.forEach(sb::indentln)

            sb.appendln()
        }

        val testDir = File(path)
        val workToOriginalFileMap = HashMap(copyTestSources(testDir, srcDir, filePrefix = ""))
        var dirtyFiles = srcDir.walk().filterTo(HashSet()) { it.isKotlinFile() }
        val incrementalData = IncrementalData()
        val steps = getModificationsToPerform(testDir, moduleNames = null, allowNoFilesWithSuffixInTestData = true, touchPolicy = TouchPolicy.CHECKSUM)
                .filter { it.isNotEmpty() }

        makeAndCheckLookups(dirtyFiles, workToOriginalFileMap, incrementalData).logOutput("INITIAL BUILD")
        for ((i, modifications) in steps.withIndex()) {
            dirtyFiles = modifications.mapNotNullTo(HashSet()) { it.perform(workingDir, workToOriginalFileMap) }
            makeAndCheckLookups(dirtyFiles, workToOriginalFileMap, incrementalData).logOutput("STEP ${i + 1}")
        }

        val expectedBuildLog = File(testDir, "build.log")
        UsefulTestCase.assertSameLinesWithFile(expectedBuildLog.canonicalPath, sb.toString())
    }

    private class CompilerOutput(
        val exitCode: String,
        val errors: List<String>,
        val compiledFiles: Iterable<File>,
        val lookupsCount: Map<File, Int>
    )
    private class IncrementalData(val sourceToOutput: MutableMap<File, MutableSet<File>> = hashMapOf())

    private fun makeAndCheckLookups(
            filesToCompile: Iterable<File>,
            workingToOriginalFileMap: Map<File, File>,
            incrementalData: IncrementalData
    ): CompilerOutput {
        removeCommentsCommentsWithLookupInfo(filesToCompile)

        for (dirtyFile in filesToCompile) {
            incrementalData.sourceToOutput.remove(dirtyFile)?.forEach {
                it.delete()
            }
        }

        val lookupTracker = TestLookupTracker()
        val messageCollector = TestMessageCollector()
        val outputItemsCollector = OutputItemsCollectorImpl()
        val services = Services.Builder().run {
            register(LookupTracker::class.java, lookupTracker)
            registerAdditionalServices()
            build()
        }
        val environment = createTestingCompilerEnvironment(messageCollector, outputItemsCollector, services)
        val exitCode = runCompiler(filesToCompile, environment)

        for (output in outputItemsCollector.outputs) {
            val outputFile = output.outputFile
            if (outputFile.extension == "kotlin_module") continue

            for (sourceFile in output.sourceFiles) {
                val outputsForSource = incrementalData.sourceToOutput.getOrPut(sourceFile) { hashSetOf() }
                outputsForSource.add(outputFile)
            }
        }

        val lookupsCount = checkLookups(filesToCompile, lookupTracker, workingToOriginalFileMap)
        return CompilerOutput(exitCode.toString(), messageCollector.errors, filesToCompile, lookupsCount)
    }

    protected open fun Services.Builder.registerAdditionalServices() {}

    private fun checkLookups(
            compiledFiles: Iterable<File>,
            lookupTracker: TestLookupTracker,
            workingToOriginalFileMap: Map<File, File>
    ): Map<File, Int> {
        fun checkLookupsInFile(expectedFile: File, actualFile: File, lookupsFromFile: List<LookupInfo>) {
            val text = actualFile.readText()

            val matchResult = COMMENT_WITH_LOOKUP_INFO.find(text)
            if (matchResult != null) {
                throw AssertionError("File $actualFile contains multiline comment in range ${matchResult.range}")
            }

            val lines = text.lines().toMutableList()

            for ((line, lookupsFromLine) in lookupsFromFile.groupBy { it.position.line }) {
                val columnToLookups = lookupsFromLine.groupBy { it.position.column }.toList().sortedBy { it.first }

                val lineContent = lines[line - 1]
                val parts = ArrayList<CharSequence>(columnToLookups.size * 2)

                var start = 0

                for ((column, lookupsFromColumn) in columnToLookups) {
                    val end = column - 1
                    parts.add(lineContent.subSequence(start, end))

                    val lookups = lookupsFromColumn.distinct().joinToString(separator = " ", prefix = "/*", postfix = "*/") {
                        val rest = lineContent.substring(end)

                        val name =
                                when {
                                    rest.startsWith(it.name) || // same name
                                    rest.startsWith("$" + it.name) || // backing field
                                    DECLARATION_STARTS_WITH.any { rest.startsWith(it) } // it's declaration
                                         -> ""
                                    else -> "(" + it.name + ")"
                                }

                        it.scopeKind.toString()[0].toLowerCase().toString() + ":" + it.scopeFqName.let { if (it.isNotEmpty()) it else "<root>" } + name
                    }

                    parts.add(lookups)

                    start = end
                }

                lines[line - 1] = parts.joinToString("") + lineContent.subSequence(start, lineContent.length)
            }

            val actual = lines.joinToString("\n")

            KotlinTestUtils.assertEqualsToFile(expectedFile, actual)
        }

        val fileToLookups = lookupTracker.lookups.groupBy { File(it.filePath) }
        return compiledFiles.associate { actualFile ->
            val expectedFile = workingToOriginalFileMap[actualFile]!!
            val lookupsInFile = fileToLookups[actualFile]
            lookupsInFile?.let { checkLookupsInFile(expectedFile, actualFile, it) }
            actualFile to (lookupsInFile?.size ?: 0)
        }
    }
}

class TestLookupTracker : LookupTracker {
    val lookups = arrayListOf<LookupInfo>()
    private val interner = StringInterner()

    override val requiresPosition: Boolean
        get() = true

    override fun record(filePath: String, position: Position, scopeFqName: String, scopeKind: ScopeKind, name: String) {
        val internedFilePath = interner.intern(filePath)
        val internedScopeFqName = interner.intern(scopeFqName)
        val internedName = interner.intern(name)

        lookups.add(LookupInfo(internedFilePath, position, internedScopeFqName, scopeKind, internedName))
    }
}


