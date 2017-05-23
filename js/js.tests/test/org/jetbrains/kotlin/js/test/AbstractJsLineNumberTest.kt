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
import org.jetbrains.kotlin.js.test.utils.LineCollector
import org.jetbrains.kotlin.js.test.utils.LineOutputToStringVisitor
import org.jetbrains.kotlin.js.util.TextOutputImpl
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.serialization.js.JsModuleDescriptor
import org.jetbrains.kotlin.serialization.js.KotlinJavascriptSerializationUtil
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.KotlinTestWithEnvironment
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
            val inputFiles = KotlinTestUtils.createTestFiles(file.name, sourceCode, testFactory, true)
            val modules = inputFiles
                    .map { it.module }.distinct()
                    .associateBy { it.name }

            val orderedModules = DFS.topologicalOrder(modules.values) { module -> module.dependencies.mapNotNull { modules[it] } }

            orderedModules.asReversed().forEach { module ->
                val baseOutputPath = module.outputFileName(file)

                val translator = K2JSTranslator(createConfig(module, file, modules))
                val units = module.files.map { TranslationUnit.SourceFile(createPsiFile(it.fileName)) }
                val translationResult = translator.translateUnits(units, MainCallParameters.noCall())

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

                File(baseOutputPath + ".js").writeText(translationResult.program.globalBlock.toString())

                val moduleDescription = JsModuleDescriptor(
                        name = module.name,
                        data = translationResult.moduleDescriptor,
                        kind = ModuleKind.PLAIN,
                        imported = emptyList()
                )
                val metaFileContent = KotlinJavascriptSerializationUtil.metadataAsString(
                        translationResult.bindingContext, moduleDescription)
                File(baseOutputPath + ".meta.js").writeText(metaFileContent)

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

    private fun TestModule.outputFileName(file: File): String {
        return outputPath(file) + "-" + name
    }

    private fun outputPath(file: File) = File(OUT_PATH, file.relativeTo(File(BASE_PATH)).path.removeSuffix(".kt")).path

    override fun createEnvironment(): KotlinCoreEnvironment {
        return KotlinCoreEnvironment.createForTests(testRootDisposable, CompilerConfiguration(), EnvironmentConfigFiles.JS_CONFIG_FILES)
    }

    private fun createConfig(module: TestModule, inputFile: File, modules: Map<String, TestModule>): JsConfig {
        val dependencies = module.dependencies
                .mapNotNull { modules[it]?.outputFileName(inputFile) }
                .map { "$it.meta.js" }

        val configuration = environment.configuration.copy()

        configuration.put(JSConfigurationKeys.LIBRARIES, JsConfig.JS_STDLIB + JsConfig.JS_KOTLIN_TEST + dependencies )

        configuration.put(CommonConfigurationKeys.MODULE_NAME, module.name)
        configuration.put(JSConfigurationKeys.MODULE_KIND, ModuleKind.PLAIN)
        configuration.put(JSConfigurationKeys.TARGET, EcmaVersion.v5)

        configuration.put(JSConfigurationKeys.SOURCE_MAP, true)

        return JsConfig(project, configuration)
    }

    private fun createPsiFile(fileName: String): KtFile {
        val psiManager = PsiManager.getInstance(project)
        val fileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL)

        return psiManager.findFile(fileSystem.findFileByPath(fileName)!!) as KtFile
    }

    private inner class TestFileFactoryImpl : KotlinTestUtils.TestFileFactory<TestModule, TestFile>, Closeable {
        val tmpDir = KotlinTestUtils.tmpDir("js-tests")
        val defaultModule = TestModule(BasicBoxTest.TEST_MODULE, emptyList())

        override fun createFile(module: TestModule?, fileName: String, text: String, directives: Map<String, String>): TestFile? {
            val currentModule = module ?: defaultModule

            val temporaryFile = File(tmpDir, "${currentModule.name}/$fileName")
            KotlinTestUtils.mkdirs(temporaryFile.parentFile)
            temporaryFile.writeText(text, Charsets.UTF_8)


            return TestFile(temporaryFile.absolutePath, currentModule)
        }

        override fun createModule(name: String, dependencies: List<String>, friends: List<String>): TestModule? {
            return TestModule(name, dependencies)
        }

        override fun close() {
            FileUtil.delete(tmpDir)
        }
    }

    private class TestModule(
            val name: String,
            dependencies: List<String>
    ) {
        val dependencies = dependencies.toMutableList()
        val files = mutableListOf<TestFile>()
    }

    private class TestFile(val fileName: String, val module: TestModule) {
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