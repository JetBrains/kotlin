/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import junit.framework.TestCase
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.js.config.EcmaVersion
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.js.facade.K2JSTranslator
import org.jetbrains.kotlin.js.facade.MainCallParameters
import org.jetbrains.kotlin.js.facade.TranslationResult
import org.jetbrains.kotlin.js.facade.TranslationUnit
import org.jetbrains.kotlin.js.test.utils.ExceptionThrowingReporter
import org.jetbrains.kotlin.js.test.utils.LineCollector
import org.jetbrains.kotlin.js.test.utils.LineOutputToStringVisitor
import org.jetbrains.kotlin.js.util.TextOutputImpl
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.test.*
import org.jetbrains.kotlin.utils.DFS
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.File
import java.io.PrintStream
import java.nio.charset.Charset

abstract class AbstractJsLineNumberTest : KotlinTestWithEnvironment() {
    fun doTest(filePath: String) {
        val file = File(filePath)
        val sourceCode = file.readText()

        TestFileFactoryImpl().use { testFactory ->
            val inputFiles = TestFiles.createTestFiles(file.name, sourceCode, testFactory, true, "")
            val modules = inputFiles
                    .map { it.module }.distinct()
                    .associateBy { it.name }

            val orderedModules = DFS.topologicalOrder(modules.values) { module -> module.dependenciesSymbols.mapNotNull { modules[it] } }

            orderedModules.asReversed().forEach { module ->
                val baseOutputPath = module.outputFileName(file)

                val translator = K2JSTranslator(createConfig(module, file, modules), false)
                val units = module.files.map { TranslationUnit.SourceFile(createPsiFile(it.fileName)) }
                val translationResult = translator.translateUnits(ExceptionThrowingReporter, units, MainCallParameters.noCall())

                if (translationResult !is TranslationResult.Success) {
                    val outputStream = ByteArrayOutputStream()
                    val collector = PrintingMessageCollector(PrintStream(outputStream), MessageRenderer.PLAIN_FULL_PATHS, true)
                    AnalyzerWithCompilerReport.reportDiagnostics(translationResult.diagnostics, collector)
                    val messages = outputStream.toByteArray().toString(Charset.forName("UTF-8"))
                    throw AssertionError("The following errors occurred compiling test:\n" + messages)
                }

                val lineCollector = LineCollector()
                lineCollector.accept(translationResult.program)

                val programOutput = TextOutputImpl()
                translationResult.program.globalBlock.accept(LineOutputToStringVisitor(programOutput, lineCollector))
                val generatedCode = programOutput.toString()
                with(File(baseOutputPath + "-lines.js")) {
                    parentFile.mkdirs()
                    writeText(generatedCode)
                }

                val baseDir = File(baseOutputPath).parentFile
                for (outputFile in translationResult.getOutputFiles(File(baseOutputPath + ".js"), null, null).asList()) {
                    with (File(baseDir, outputFile.relativePath)) {
                        parentFile.mkdirs()
                        writeBytes(outputFile.asByteArray())
                    }
                }

                val linesMatcher = module.files
                        .mapNotNull { LINES_PATTERN.find(File(it.fileName).readText()) }
                        .firstOrNull()
                                   ?: error("'// LINES: ' comment was not found in source file. Generated code is:\n$generatedCode")

                val expectedLines = linesMatcher.groups[1]!!.value
                val actualLines = lineCollector.lines
                        .dropLastWhile { it == null }
                        .joinToString(" ") { if (it == null) "*" else (it + 1).toString() }

                TestCase.assertEquals(generatedCode, expectedLines, actualLines)
            }
        }
    }

    private fun TestModule.outputFileName(file: File): String = outputPath(file) + "-" + name

    private fun outputPath(file: File) = File(OUT_PATH, file.relativeTo(File(BASE_PATH)).path.removeSuffix(".kt")).path

    override fun createEnvironment(): KotlinCoreEnvironment =
            KotlinCoreEnvironment.createForTests(testRootDisposable, CompilerConfiguration(), EnvironmentConfigFiles.JS_CONFIG_FILES)

    private fun createConfig(module: TestModule, inputFile: File, modules: Map<String, TestModule>): JsConfig {
        val dependencies = module.dependenciesSymbols
                .mapNotNull { modules[it]?.outputFileName(inputFile) }
                .map { "$it.meta.js" }

        val configuration = environment.configuration.copy()

        configuration.put(JSConfigurationKeys.LIBRARIES, JsConfig.JS_STDLIB + JsConfig.JS_KOTLIN_TEST + dependencies)

        configuration.put(CommonConfigurationKeys.MODULE_NAME, module.name)
        configuration.put(JSConfigurationKeys.MODULE_KIND, ModuleKind.PLAIN)
        configuration.put(JSConfigurationKeys.TARGET, EcmaVersion.v5)

        configuration.put(JSConfigurationKeys.SOURCE_MAP, true)
        configuration.put(JSConfigurationKeys.META_INFO, true)

        return JsConfig(project, configuration)
    }

    private fun createPsiFile(fileName: String): KtFile {
        val psiManager = PsiManager.getInstance(project)
        val fileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL)

        return psiManager.findFile(fileSystem.findFileByPath(fileName)!!) as KtFile
    }

    private inner class TestFileFactoryImpl : TestFiles.TestFileFactory<TestModule, TestFile>, Closeable {
        private val tmpDir = KotlinTestUtils.tmpDir("js-tests")
        private val defaultModule = TestModule(BasicBoxTest.TEST_MODULE, emptyList(), emptyList())

        override fun createFile(module: TestModule?, fileName: String, text: String, directives: Directives): TestFile? {
            val currentModule = module ?: defaultModule

            val temporaryFile = File(tmpDir, "${currentModule.name}/$fileName")
            KotlinTestUtils.mkdirs(temporaryFile.parentFile)
            temporaryFile.writeText(text, Charsets.UTF_8)

            return TestFile(temporaryFile.absolutePath, text, currentModule, directives)
        }

        override fun createModule(name: String, dependencies: List<String>, friends: List<String>) = TestModule(name, dependencies, friends)

        override fun close() {
            FileUtil.delete(tmpDir)
        }
    }

    private class TestModule(
        name: String,
        dependenciesSymbols: List<String>,
        friendsSymbols: List<String>
    ): KotlinBaseTest.TestModule(name, dependenciesSymbols, friendsSymbols) {
        val files = mutableListOf<TestFile>()
    }

    private class TestFile(val fileName: String, content: String, val module: TestModule, directives: Directives) :
        KotlinBaseTest.TestFile(fileName, content, directives) {
        init {
            module.files += this
        }
    }

    companion object {
        private val DIR_NAME = "lineNumbers"
        private val LINES_PATTERN = Regex("^ *// *LINES: *(.*)$", RegexOption.MULTILINE)
        private val BASE_PATH = "${BasicBoxTest.TEST_DATA_DIR_PATH}/$DIR_NAME"
        private val OUT_PATH = "${BasicBoxTest.TEST_DATA_DIR_PATH}/out/$DIR_NAME"
    }
}