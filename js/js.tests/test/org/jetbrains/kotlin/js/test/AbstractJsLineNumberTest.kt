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
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.test.KotlinTestWithEnvironment
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.nio.charset.Charset

abstract class AbstractJsLineNumberTest : KotlinTestWithEnvironment() {
    fun doTest(filePath: String) {
        val translator = K2JSTranslator(createConfig())
        val unit = TranslationUnit.SourceFile(createPsiFile(filePath))
        val translationResult = translator.translateUnits(listOf(unit), MainCallParameters.noCall())

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
        val relativePath = File(filePath).relativeTo(File(BASE_PATH)).path.removeSuffix(".kt")
        with(File(File(OUT_PATH), relativePath + ".js")) {
            parentFile.mkdirs()
            writeText(generatedCode)
        }

        val sourceCode = FileUtil.loadFile(File(filePath))
        val linesMatcher = LINES_PATTERN.find(sourceCode) ?:
                           error("'// LINES: ' comment was not found in source file. Generated code is:\n$generatedCode")

        val expectedLines = linesMatcher.groups[1]!!.value
        val actualLines = lineCollector.lines
                .dropLastWhile { it == null }
                .joinToString(" ") { if (it == null) "*" else (it + 1).toString() }

        TestCase.assertEquals(generatedCode, expectedLines, actualLines)
    }

    override fun createEnvironment(): KotlinCoreEnvironment {
        return KotlinCoreEnvironment.createForTests(testRootDisposable, CompilerConfiguration(), EnvironmentConfigFiles.JS_CONFIG_FILES)
    }

    private fun createConfig(): JsConfig {
        val configuration = environment.configuration.copy()

        configuration.put(JSConfigurationKeys.LIBRARIES, JsConfig.JS_STDLIB + JsConfig.JS_KOTLIN_TEST)

        configuration.put(CommonConfigurationKeys.MODULE_NAME, "test")
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

    companion object {
        private val DIR_NAME = "lineNumbers"
        private val LINES_PATTERN = Regex("^ *// *LINES: *(.*)$", RegexOption.MULTILINE)
        private val BASE_PATH = "${BasicBoxTest.TEST_DATA_DIR_PATH}/$DIR_NAME"
        private val OUT_PATH = "${BasicBoxTest.TEST_DATA_DIR_PATH}/out/$DIR_NAME"
    }
}