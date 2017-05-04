/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.sourceSections

import com.intellij.openapi.vfs.StandardFileSystems
import junit.framework.TestCase
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.*
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinToJVMBytecodeCompiler
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.addKotlinSourceRoots
import org.jetbrains.kotlin.daemon.client.DaemonReportingTargets
import org.jetbrains.kotlin.daemon.client.KotlinCompilerClient
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.script.StandardScriptDefinition
import org.jetbrains.kotlin.script.tryConstructClassFromStringArgs
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestCaseWithTmpdir
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.utils.KotlinPaths
import org.jetbrains.kotlin.utils.PathUtil
import java.io.*
import java.lang.management.ManagementFactory
import java.net.URLClassLoader
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

class SourceSectionsTest : TestCaseWithTmpdir() {

    companion object {
        val TEST_ALLOWED_SECTIONS = listOf("let", "apply") // using standard function names that can be used as sections in the script context without crafting special ones
        val TEST_DATA_DIR = File(KotlinTestUtils.getHomeDirectory(), "plugins/source-sections/source-sections-compiler/testData")
    }

    private val kotlinPaths: KotlinPaths by lazy {
        val paths = PathUtil.getKotlinPathsForDistDirectory()
        TestCase.assertTrue("Lib directory doesn't exist. Run 'ant dist'", paths.libPath.absoluteFile.isDirectory)
        paths
    }

    val compilerClassPath = listOf(kotlinPaths.compilerPath)
    val scriptRuntimeClassPath = listOf( kotlinPaths.runtimePath, kotlinPaths.scriptRuntimePath)
    val sourceSectionsPluginJar = File(kotlinPaths.libPath, "kotlin-source-sections-compiler-plugin.jar")
    val compilerId by lazy(LazyThreadSafetyMode.NONE) { CompilerId.makeCompilerId(compilerClassPath) }

    private fun createEnvironment(vararg sources: String, withSourceSectionsPlugin: Boolean = true): KotlinCoreEnvironment {
        val configuration = KotlinTestUtils.newConfiguration(ConfigurationKind.NO_KOTLIN_REFLECT, TestJdkKind.FULL_JDK)

        configuration.addKotlinSourceRoots(sources.asList())
        configuration.put<MessageCollector>(
                CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY,
                PrintingMessageCollector(System.err, MessageRenderer.PLAIN_FULL_PATHS, false)
        )
        configuration.add(JVMConfigurationKeys.SCRIPT_DEFINITIONS, StandardScriptDefinition)
        if (withSourceSectionsPlugin) {
            configuration.addAll(SourceSectionsConfigurationKeys.SECTIONS_OPTION, TEST_ALLOWED_SECTIONS)
            configuration.add(ComponentRegistrar.PLUGIN_COMPONENT_REGISTRARS, SourceSectionsComponentRegistrar())
        }

        val environment = KotlinCoreEnvironment.createForTests(testRootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
        return environment
    }

    private data class SourceToExpectedResults(val source: File, val expectedResults: File)

    private fun getTestFiles(expectedExt: String): List<SourceToExpectedResults> {
        val testDataFiles = TEST_DATA_DIR.listFiles()
        val sourceToExpected = testDataFiles.filter { it.isFile && it.extension == "kts" }
                .mapNotNull { testFile ->
                    testDataFiles.find { it.isFile && it.name == testFile.name + expectedExt }?.let { SourceToExpectedResults(testFile, it) }
                }
        TestCase.assertTrue("No test files found", sourceToExpected.isNotEmpty())
        return sourceToExpected
    }

    private fun InputStream.trimmedLines(charset: Charset): List<String> = use {
        bufferedReader(charset)
                .lineSequence()
                .map(String::trimEnd)
                .toList()
                .dropLastWhile { it.isBlank() }
    }

    fun testSourceSectionsFilter() {
        val sourceToFiltered = getTestFiles(".filtered")

        createEnvironment() // creates VirtualFileManager
        val fileCreator = FilteredSectionsVirtualFileExtension(TEST_ALLOWED_SECTIONS.toSet())

        sourceToFiltered.forEach { (source, expectedResult) ->
            val filteredVF = fileCreator.createPreprocessedFile(StandardFileSystems.local().findFileByPath(source.canonicalPath))
            TestCase.assertNotNull("Cannot generate preprocessed file", filteredVF)
            val expected = expectedResult.inputStream().trimmedLines(Charset.defaultCharset())
            val filteredBytes = filteredVF!!.contentsToByteArray()
            val actual = ByteArrayInputStream(filteredBytes).trimmedLines(filteredVF.charset)
            TestCase.assertEquals("Unexpected result on preprocessing file '${source.name}'", expected, actual)
        }
    }

    fun testSourceSectionsFilterWithCRLF() {
        val sourceToFiltered = getTestFiles(".filtered")

        createEnvironment() // creates VirtualFileManager
        val fileCreator = FilteredSectionsVirtualFileExtension(TEST_ALLOWED_SECTIONS.toSet())

        sourceToFiltered.forEach { (source, expectedResult) ->
            val sourceWithCRLF = createTempFile(source.name)
            sourceWithCRLF.writeText(source.readText().replace("\r\n", "\n").replace("\n", "\r\n"))
            val filteredVF = fileCreator.createPreprocessedFile(StandardFileSystems.local().findFileByPath(sourceWithCRLF.canonicalPath))
            TestCase.assertNotNull("Cannot generate preprocessed file", filteredVF)
            val expected = expectedResult.inputStream().trimmedLines(Charset.defaultCharset())
            val actual = ByteArrayInputStream(filteredVF!!.contentsToByteArray()).trimmedLines(filteredVF.charset)
            TestCase.assertEquals("Unexpected result on preprocessing file '${source.name}'", expected, actual)
        }
    }

    fun testSourceSectionsRun() {
        val sourceToOutput = getTestFiles(".out")

        sourceToOutput.forEach { (source, expectedOutput) ->
            val environment = createEnvironment(source.canonicalPath)
            val scriptClass = KotlinToJVMBytecodeCompiler.compileScript(environment, Thread.currentThread().contextClassLoader)
            TestCase.assertNotNull("Compilation errors", scriptClass)
            verifyScriptOutput(scriptClass, expectedOutput)
        }
    }

    // Note: the test is flaky, because it is statistical and the thresholds are not big enough.
    // Therefore it was decided to ignore it, but leave in the code in order to be able to quickly check overheads when needed.
    @Suppress("unused")
    fun ignored_testSourceSectionsRunBench() {
        val mxBeans = ManagementFactory.getThreadMXBean()
        val (source, _) = getTestFiles(".out").first()

        // warming up application environment
        KotlinToJVMBytecodeCompiler.compileScript(createEnvironment(source.canonicalPath, withSourceSectionsPlugin = false), Thread.currentThread().contextClassLoader)

        val times = generateSequence {
            val t0 = mxBeans.threadCpuTime()
            KotlinToJVMBytecodeCompiler.compileScript(createEnvironment(source.canonicalPath, withSourceSectionsPlugin = false), Thread.currentThread().contextClassLoader)
            val t1 = mxBeans.threadCpuTime()
            KotlinToJVMBytecodeCompiler.compileScript(createEnvironment(source.canonicalPath, withSourceSectionsPlugin = true), Thread.currentThread().contextClassLoader)
            val t2 = mxBeans.threadCpuTime()
            Triple(t1 - t0, t2 - t1, t2 - t1)
        }.take(10).toList()

        val adjustedMaxDiff = times.sortedByDescending { (_, _, diff) -> diff }.drop(2).first()

        fun Long.ms() = TimeUnit.NANOSECONDS.toMillis(this)
        TestCase.assertTrue("sourceSections plugin brings too much overheads: ${times.joinToString { "(${it.first.ms()}, ${it.second.ms()})" }} (expecting it to be faster than regular compilation due to less lines compiled)",
                            adjustedMaxDiff.third < 20 /* assuming it measurement error */ || adjustedMaxDiff.first >= adjustedMaxDiff.second )
    }

    fun testSourceSectionCompileLocal() {
        val sourceToOutput = getTestFiles(".out")

        sourceToOutput.forEach { (source, expectedOutput) ->
            val args = arrayOf(source.canonicalPath, "-d", tmpdir.canonicalPath,
                               "-Xplugin=${sourceSectionsPluginJar.canonicalPath}",
                               "-P", TEST_ALLOWED_SECTIONS.joinToString(",") { "plugin:${SourceSectionsCommandLineProcessor.PLUGIN_ID}:${SourceSectionsCommandLineProcessor.SECTIONS_OPTION.name}=$it" })
            val (output, code) = captureOut {
                CLICompiler.doMainNoExit(K2JVMCompiler(), args)
            }

            TestCase.assertEquals("Compilation failed:\n$output", 0, code.code)

            val scriptClass = loadScriptClass(File(tmpdir, source.nameWithoutExtension.capitalize() + ".class"))

            verifyScriptOutput(scriptClass, expectedOutput)
        }
    }

    fun testSourceSectionCompileOnDaemon() {
        val sourceToOutput = getTestFiles(".out")

        withFlagFile("sourceSections", ".alive") { aliveFile ->

            val daemonOptions = DaemonOptions(runFilesPath = File(tmpdir, getTestName(true)).absolutePath, verbose = true, reportPerf = true)
            val daemonJVMOptions = configureDaemonJVMOptions(inheritMemoryLimits = false, inheritOtherJvmOptions = false, inheritAdditionalProperties = false)
            val messageCollector = TestMessageCollector()

            val daemonWithSession = KotlinCompilerClient.connectAndLease(compilerId, aliveFile, daemonJVMOptions, daemonOptions,
                                                                         DaemonReportingTargets(messageCollector = messageCollector), autostart = true, leaseSession = true)
            assertNotNull("failed to connect daemon", daemonWithSession)

            try {

                sourceToOutput.forEach { (source, expectedOutput) ->
                    val args = arrayOf(source.canonicalPath, "-d", tmpdir.canonicalPath,
                                       "-Xplugin=${sourceSectionsPluginJar.canonicalPath}",
                                       "-P", TEST_ALLOWED_SECTIONS.joinToString(",") { "plugin:${SourceSectionsCommandLineProcessor.PLUGIN_ID}:${SourceSectionsCommandLineProcessor.SECTIONS_OPTION.name}=$it" },
                                       "-Xreport-output-files")

                    messageCollector.clear()
                    val outputs = arrayListOf<OutputMessageUtil.Output>()

                    val code = KotlinCompilerClient.compile(daemonWithSession!!.compileService, daemonWithSession.sessionId, CompileService.TargetPlatform.JVM,
                                                            args, messageCollector,
                                                            { outFile, srcFiles -> outputs.add(OutputMessageUtil.Output(srcFiles, outFile)) },
                                                            reportSeverity = ReportSeverity.DEBUG)

                    TestCase.assertEquals("Compilation failed:\n${messageCollector.messages.joinToString("\n")}", 0, code)
                    TestCase.assertFalse("Compilation failed:\n${messageCollector.messages.joinToString("\n")}", messageCollector.hasErrors())
                    val scriptClassFile = outputs.first().outputFile
                    TestCase.assertEquals("unexpected class file generated", source.nameWithoutExtension.capitalize(), scriptClassFile?.nameWithoutExtension)

                    verifyScriptOutput(loadScriptClass(scriptClassFile), expectedOutput)
                }
            }
            finally {
                daemonWithSession!!.compileService.shutdown()
            }
        }
    }

    private fun loadScriptClass(scriptClassFile: File?): Class<*>? {
        val cl = URLClassLoader((scriptRuntimeClassPath + tmpdir).map { it.toURI().toURL() }.toTypedArray())
        val scriptClass = cl.loadClass(scriptClassFile!!.nameWithoutExtension)

        TestCase.assertNotNull("Unable to load class $scriptClassFile", scriptClass)
        return scriptClass
    }

    private fun verifyScriptOutput(scriptClass: Class<*>?, expectedOutput: File) {
        val scriptOut = captureOut {
            tryConstructClassFromStringArgs(scriptClass!!, emptyList())
        }.first.lines()
                .map(String::trimEnd)
                .dropLastWhile { it.isBlank() }

        val expected = expectedOutput.inputStream().trimmedLines(Charset.defaultCharset())

        TestCase.assertEquals("Unexpected result on evaluating ${scriptClass?.name}", expected, scriptOut)
    }
}

internal inline fun withFlagFile(prefix: String, suffix: String? = null, body: (File) -> Unit) {
    val file = createTempFile(prefix, suffix)
    try {
        body(file)
    }
    finally {
        file.delete()
    }
}

internal fun<T> captureOut(body: () -> T): Pair<String, T> {
    val outStream = ByteArrayOutputStream()
    val prevOut = System.out
    val prevErr = System.err
    System.setOut(PrintStream(outStream))
    System.setErr(PrintStream(outStream))
    val res =try {
        body()
    }
    finally {
        System.out.flush()
        System.setOut(prevOut)
        System.err.flush()
        System.setErr(prevErr)
    }
    return outStream.toString() to res
}

class TestMessageCollector : MessageCollector {
    data class Message(val severity: CompilerMessageSeverity, val message: String, val location: CompilerMessageLocation?)

    val messages = arrayListOf<Message>()

    override fun clear() {
        messages.clear()
    }

    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageLocation?) {
        messages.add(Message(severity, message, location))
    }

    override fun hasErrors(): Boolean = messages.any { it.severity == CompilerMessageSeverity.EXCEPTION || it.severity == CompilerMessageSeverity.ERROR }
}
