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

package org.jetbrains.kotlin.js.test

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.common.output.outputUtils.writeAllTo
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.js.JavaScript
import org.jetbrains.kotlin.js.backend.ast.JsProgram
import org.jetbrains.kotlin.js.config.EcmaVersion
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.js.config.LibrarySourcesConfig
import org.jetbrains.kotlin.js.facade.K2JSTranslator
import org.jetbrains.kotlin.js.facade.MainCallParameters
import org.jetbrains.kotlin.js.facade.TranslationResult
import org.jetbrains.kotlin.js.test.rhino.RhinoFunctionResultChecker
import org.jetbrains.kotlin.js.test.rhino.RhinoUtils
import org.jetbrains.kotlin.js.test.utils.DirectiveTestUtils
import org.jetbrains.kotlin.js.test.utils.JsTestUtils
import org.jetbrains.kotlin.js.test.utils.verifyAst
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.KotlinTestUtils.TestFileFactory
import org.jetbrains.kotlin.test.KotlinTestWithEnvironment
import org.jetbrains.kotlin.utils.DFS
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.File
import java.io.PrintStream
import java.nio.charset.Charset
import java.util.regex.Pattern

abstract class BasicBoxTest(
        private val pathToTestDir: String,
        private val pathToOutputDir: String
) : KotlinTestWithEnvironment() {
    private val COMMON_FILES_NAME = "_common"
    private val COMMON_FILES_DIR = "_commonFiles/"
    val MODULE_EMULATION_FILE = TEST_DATA_DIR_PATH + "/moduleEmulation.js"
    val additionalCommonFileDirectories = mutableListOf<String>()

    val TEST_MODULE = "JS_TESTS"
    val DEFAULT_MODULE = "main"
    val TEST_FUNCTION = "box"
    private val OLD_MODULE_SUFFIX = "-old"

    fun doTest(filePath: String) {
        val file = File(filePath)
        val outputDir = getOutputDir(file)
        val expectedText = KotlinTestUtils.doLoadFile(file)

        TestFileFactoryImpl().use { testFactory ->
            testFactory.defaultModule.moduleKind

            val inputFiles = KotlinTestUtils.createTestFiles(file.name, expectedText, testFactory)
            val modules = inputFiles
                    .map { it.module }.distinct()
                    .map { it.name to it }.toMap()

            val orderedModules = DFS.topologicalOrder(modules.values) { module -> module.dependencies.mapNotNull { modules[it] } }

            val generatedJsFiles = orderedModules.asReversed().mapNotNull { module ->
                val dependencies = module.dependencies.mapNotNull { modules[it]?.outputFileName(outputDir) + ".meta.js" }

                val outputFileName = module.outputFileName(outputDir) + ".js"
                generateJavaScriptFile(file.parent, module, outputFileName, dependencies, modules.size > 1)

                if (!module.name.endsWith(OLD_MODULE_SUFFIX)) outputFileName else null
            }
            val mainModuleName = if (TEST_MODULE in modules) TEST_MODULE else DEFAULT_MODULE
            val mainModule = modules[mainModuleName]!!

            val globalCommonFiles = JsTestUtils.getFilesInDirectoryByExtension(
                    TEST_DATA_DIR_PATH + COMMON_FILES_DIR, JavaScript.EXTENSION)
            val localCommonFile = file.parent + "/" + COMMON_FILES_NAME + JavaScript.DOT_EXTENSION
            val localCommonFiles = if (File(localCommonFile).exists()) listOf(localCommonFile) else emptyList()

            val additionalCommonFiles = additionalCommonFileDirectories.flatMap { baseDir ->
                JsTestUtils.getFilesInDirectoryByExtension(baseDir + "/", JavaScript.EXTENSION)
            }
            val inputJsFiles = inputFiles
                    .filter { it.fileName.endsWith(".js") }
                    .map { file ->
                        val sourceFile = File(file.fileName)
                        val targetFile = File(outputDir, file.module.outputFileSimpleName() + "-js-" + sourceFile.name)
                        FileUtil.copy(File(file.fileName), targetFile)
                        targetFile.absolutePath
                    }

            val additionalFiles = mutableListOf<String>()

            val moduleKindMatcher = MODULE_KIND_PATTERN.matcher(expectedText)
            val moduleKind = if (moduleKindMatcher.find()) ModuleKind.valueOf(moduleKindMatcher.group(1)) else ModuleKind.PLAIN

            val withModuleSystem = moduleKind != ModuleKind.PLAIN && !NO_MODULE_SYSTEM_PATTERN.matcher(expectedText).find()

            if (withModuleSystem) {
                additionalFiles += MODULE_EMULATION_FILE
            }

            val additionalJsFile = filePath.removeSuffix("." + KotlinFileType.EXTENSION) + JavaScript.DOT_EXTENSION
            if (File(additionalJsFile).exists()) {
                additionalFiles += additionalJsFile
            }

            val allJsFiles = additionalFiles + inputJsFiles + generatedJsFiles + globalCommonFiles + localCommonFiles +
                             additionalCommonFiles

            if (!SKIP_NODE_JS.matcher(expectedText).find()) {
                val nodeRunnerName = mainModule.outputFileName(outputDir) + ".node.js"
                val nodeRunnerText = generateNodeRunner(allJsFiles, outputDir, mainModuleName, testFactory.testPackage)
                FileUtil.writeToFile(File(nodeRunnerName), nodeRunnerText)
            }

            val checker = RhinoFunctionResultChecker(mainModuleName, testFactory.testPackage, TEST_FUNCTION, "OK", withModuleSystem)
            RhinoUtils.runRhinoTest(allJsFiles, checker)
        }
    }

    private fun generateNodeRunner(files: Collection<String>, dir: File, moduleName: String, testPackage: String?): String {
        val sb = StringBuilder("var text = \"\";\n")
        sb.append("var fs = require('fs');\n")

        sb.append("module.exports = function(kotlin, requireFromString) {\n")
        sb.append("text += 'module.exports = function(kotlin) {\\n';\n")

        for (file in files) {
            val fileName = FileUtil.getRelativePath(dir, File(file))!!
            sb.append("text += fs.readFileSync(__dirname + \"/$fileName\") + \"\\n\";\n")
        }
        sb.append("text += 'return $moduleName;';\n")
        sb.append("text += \"};\";\n")

        val fqn = testPackage?.let { ".$it" } ?: ""

        sb.append("var testModule = requireFromString(text)(kotlin);\n")
        sb.append("return testModule$fqn.box();\n")
        sb.append("};")

        return sb.toString()
    }

    protected fun getOutputDir(file: File): File {
        val stopFile = File(pathToTestDir)
        return generateSequence(file.parentFile) { it.parentFile }
                .takeWhile { it != stopFile }
                .map { it.name }
                .toList().asReversed()
                .fold(File(pathToOutputDir), ::File)
    }

    private fun TestModule.outputFileSimpleName(): String {
        val outputFileSuffix = if (this.name == TEST_MODULE) "" else "-$name"
        return getTestName(true) + outputFileSuffix
    }

    private fun TestModule.outputFileName(directory: File): String {
        return directory.absolutePath + "/" + outputFileSimpleName() + "_v5"
    }

    private fun generateJavaScriptFile(
            directory: String,
            module: TestModule,
            outputFileName: String,
            dependencies: List<String>,
            multiModule: Boolean
    ) {
        val testFiles = module.files.map { it.fileName }.filter { it.endsWith(".kt") }
        val globalCommonFiles = JsTestUtils.getFilesInDirectoryByExtension(
                TEST_DATA_DIR_PATH + COMMON_FILES_DIR, KotlinFileType.EXTENSION)
        val localCommonFile = directory + "/" + COMMON_FILES_NAME + "." + KotlinFileType.EXTENSION
        val localCommonFiles = if (File(localCommonFile).exists()) listOf(localCommonFile) else emptyList()
        val additionalCommonFiles = additionalCommonFileDirectories.flatMap { baseDir ->
            JsTestUtils.getFilesInDirectoryByExtension(baseDir + "/", KotlinFileType.EXTENSION)
        }
        val psiFiles = createPsiFiles(testFiles + globalCommonFiles + localCommonFiles + additionalCommonFiles)

        val config = createConfig(module, dependencies, multiModule)
        val outputFile = File(outputFileName)

        translateFiles(psiFiles, outputFile, config)
    }

    protected fun translateFiles(psiFiles: List<KtFile>, outputFile: File, config: JsConfig) {
        val translator = K2JSTranslator(config)
        val translationResult = translator.translate(psiFiles, MainCallParameters.noCall())

        if (translationResult !is TranslationResult.Success) {
            val outputStream = ByteArrayOutputStream()
            val collector = PrintingMessageCollector(PrintStream(outputStream), MessageRenderer.PLAIN_FULL_PATHS, true)
            AnalyzerWithCompilerReport.reportDiagnostics(translationResult.diagnostics, collector)
            val messages = outputStream.toByteArray().toString(Charset.forName("UTF-8"))
            throw AssertionError("The following errors occurred compiling test:\n" + messages)
        }

        val outputFiles = translationResult.getOutputFiles(outputFile, null, null)
        val outputDir = outputFile.parentFile ?: error("Parent file for output file should not be null, outputFilePath: " + outputFile.path)
        outputFiles.writeAllTo(outputDir)

        if (config.moduleKind == ModuleKind.COMMON_JS) {
            val content = FileUtil.loadFile(outputFile, true)
            val wrappedContent = "$KOTLIN_TEST_INTERNAL.beginModule();\n" +
                                 "$content\n" +
                                 "$KOTLIN_TEST_INTERNAL.endModule(\"${StringUtil.escapeStringCharacters(config.moduleId)}\");"
            FileUtil.writeToFile(outputFile, wrappedContent)
        }

        processJsProgram(translationResult.program, psiFiles)
    }

    protected fun processJsProgram(program: JsProgram, psiFiles: List<KtFile>) {
        psiFiles.asSequence()
                .map { it.text }
                .forEach { DirectiveTestUtils.processDirectives(program, it) }
        program.verifyAst()
    }

    private fun createPsiFiles(fileNames: List<String>): List<KtFile> {
        val psiManager = PsiManager.getInstance(project)
        val fileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL)

        return fileNames.map { fileName -> psiManager.findFile(fileSystem.findFileByPath(fileName)!!) as KtFile }
    }

    private fun createConfig(module: TestModule, dependencies: List<String>, multiModule: Boolean): JsConfig {
        val configuration = environment.configuration.copy()

        configuration.put(CommonConfigurationKeys.DISABLE_INLINE, module.inliningDisabled)
        module.languageVersion?.let { languageVersion ->
            configuration.languageVersionSettings =
                    LanguageVersionSettingsImpl(languageVersion, LanguageVersionSettingsImpl.DEFAULT.apiVersion)
        }

        configuration.put(JSConfigurationKeys.LIBRARIES, LibrarySourcesConfig.JS_STDLIB + LibrarySourcesConfig.JS_KOTLIN_TEST + dependencies)

        configuration.put(CommonConfigurationKeys.MODULE_NAME, module.name.removeSuffix(OLD_MODULE_SUFFIX))
        configuration.put(JSConfigurationKeys.MODULE_KIND, module.moduleKind)
        configuration.put(JSConfigurationKeys.TARGET, EcmaVersion.v5)

        //configuration.put(JSConfigurationKeys.SOURCE_MAP, shouldGenerateSourceMap())
        configuration.put(JSConfigurationKeys.META_INFO, multiModule)

        return LibrarySourcesConfig(project, configuration)
    }

    private inner class TestFileFactoryImpl : TestFileFactory<TestModule, TestFile>, Closeable {
        var testPackage: String? = null
        val tmpDir = KotlinTestUtils.tmpDir("js-tests")
        val defaultModule = TestModule(TEST_MODULE, emptyList())

        override fun createFile(module: TestModule?, fileName: String, text: String, directives: Map<String, String>): TestFile? {
            val currentModule = module ?: defaultModule

            val ktFile = KtPsiFactory(project).createFile(text)
            val boxFunction = ktFile.declarations.find { it is KtNamedFunction && it.name == TEST_FUNCTION  }
            if (boxFunction != null) {
                testPackage = ktFile.packageFqName.asString()
                if (testPackage?.isEmpty() ?: false) {
                    testPackage = null
                }
            }

            val moduleKindMatcher = MODULE_KIND_PATTERN.matcher(text)
            if (moduleKindMatcher.find()) {
                currentModule.moduleKind = ModuleKind.valueOf(moduleKindMatcher.group(1))
            }

            if (NO_INLINE_PATTERN.matcher(text).find()) {
                currentModule.inliningDisabled = true
            }

            val temporaryFile = File(tmpDir, "${currentModule.name}/$fileName")
            KotlinTestUtils.mkdirs(temporaryFile.parentFile)
            temporaryFile.writeText(text, Charsets.UTF_8)

            val version = InTextDirectivesUtils.findStringWithPrefixes(text, "// LANGUAGE_VERSION:")
            if (version != null) {
                assert(currentModule.languageVersion == null) { "Should not specify LANGUAGE_VERSION twice" }
                currentModule.languageVersion = LanguageVersion.fromVersionString(version)
            }

            return TestFile(temporaryFile.absolutePath, currentModule)
        }

        override fun createModule(name: String, dependencies: List<String>): TestModule? {
            return TestModule(name, dependencies)
        }

        override fun close() {
            FileUtil.delete(tmpDir)
        }
    }

    private class TestFile(val fileName: String, val module: TestModule) {
        init {
            module.files += this
        }
    }

    private class TestModule(
            val name: String,
            dependencies: List<String>
    ) {
        val dependencies = dependencies.toMutableList()
        var moduleKind = ModuleKind.PLAIN
        var inliningDisabled = false
        val files = mutableListOf<TestFile>()
        var languageVersion: LanguageVersion? = null
    }

    override fun createEnvironment(): KotlinCoreEnvironment {
        return KotlinCoreEnvironment.createForTests(testRootDisposable, CompilerConfiguration(), EnvironmentConfigFiles.JS_CONFIG_FILES)
    }

    companion object {
        val TEST_DATA_DIR_PATH = "js/js.translator/testData/"

        private val MODULE_KIND_PATTERN = Pattern.compile("^// *MODULE_KIND: *(.+)$", Pattern.MULTILINE)
        private val NO_MODULE_SYSTEM_PATTERN = Pattern.compile("^// *NO_JS_MODULE_SYSTEM", Pattern.MULTILINE)
        private val NO_INLINE_PATTERN = Pattern.compile("^// *NO_INLINE *$", Pattern.MULTILINE)
        private val SKIP_NODE_JS = Pattern.compile("^// *SKIP_NODE_JS *$", Pattern.MULTILINE)

        const val KOTLIN_TEST_INTERNAL = "\$kotlin_test_internal\$"
    }
}
