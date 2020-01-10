/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kaptlite.test

import com.intellij.compiler.JavaInMemoryCompiler
import org.jetbrains.kaptlite.AbstractKaptLiteAnalysisHandlerExtension
import org.jetbrains.kaptlite.KaptLiteClassBuilderInterceptorExtension
import org.jetbrains.kaptlite.stubs.GeneratorOutput
import org.jetbrains.kaptlite.stubs.util.CodeScope
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.CliBindingTrace
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration
import org.jetbrains.kotlin.codegen.CodegenTestCase
import org.jetbrains.kotlin.codegen.CodegenTestCase.TestFile
import org.jetbrains.kotlin.codegen.extensions.ClassBuilderInterceptorExtension
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File
import java.util.*
import javax.lang.model.element.NestingKind
import javax.tools.DiagnosticListener
import javax.tools.JavaFileObject
import javax.tools.SimpleJavaFileObject
import javax.tools.ToolProvider
import kotlin.collections.ArrayList

abstract class AbstractStubGeneratorTest : CodegenTestCase() {
    companion object {
        private const val KOTLIN_METADATA_GROUP = "[a-z0-9]+ = (\\{.+?\\}|[0-9]+)"
        private val KOTLIN_METADATA_REGEX = "@kotlin\\.Metadata\\(($KOTLIN_METADATA_GROUP)(, $KOTLIN_METADATA_GROUP)*\\)".toRegex()

        private fun removeMetadataAnnotationContents(test: String): String {
            return test.replace(KOTLIN_METADATA_REGEX, "@kotlin.Metadata()")
        }
    }

    private val origins = mutableMapOf<String, JvmDeclarationOrigin>()

    override fun setupEnvironment(environment: KotlinCoreEnvironment) {
        ClassBuilderInterceptorExtension.registerExtension(environment.project, KaptLiteClassBuilderInterceptorExtension(origins))
    }

    override fun tearDown() {
        origins.clear()
        super.tearDown()
    }

    override fun doMultiFileTest(wholeFile: File, files: List<TestFile>) {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.ALL, *listOfNotNull(writeJavaFiles(files)).toTypedArray())

        val messageCollector = object : MessageCollector {
            val errors = ArrayList<String>(0)

            override fun hasErrors() = errors.isNotEmpty()
            override fun clear() = errors.clear()

            override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageLocation?) {
                if (severity.isError) {
                    errors += message
                }
            }
        }

        val analysisHandlerExtension = KaptLiteAnalysisHandlerExtensionForTests(myEnvironment.configuration, messageCollector)
        AnalysisHandlerExtension.registerExtension(myEnvironment.project, analysisHandlerExtension)

        loadMultiFiles(files)

        val trace = CliBindingTrace()
        analyzeFilesWithJavaIntegration(
            myEnvironment.project,
            myFiles.psiFiles,
            trace,
            myEnvironment.configuration,
            myEnvironment::createPackagePartProvider
        )

        val expectedFile = File(wholeFile.parentFile, wholeFile.nameWithoutExtension + ".txt")

        if (messageCollector.hasErrors()) {
            KotlinTestUtils.assertEqualsToFile(expectedFile, messageCollector.errors.joinToString("\n"))
            return
        }

        checkGeneratorOutput(analysisHandlerExtension.output, expectedFile, files)
    }

    private fun checkGeneratorOutput(output: TestGeneratorOutput, expectedFile: File, files: List<TestFile>) {
        val filesContent = output.files
            .sortedBy { it.name }
            .joinToString("\n\n") { "// FILE: " + it.name + "\n" + it.content }
            .lineSequence()
            .map { line -> if (line.isBlank()) "" else line }
            .joinToString("\n")
            .let(::removeMetadataAnnotationContents)

        val diagnostics = checkWithJavac(output.files + files.filter { it.name.endsWith(".java") })

        val actualContent = buildString {
            append(filesContent)
            if (diagnostics.isNotEmpty()) {
                appendln().appendln().append(diagnostics)
            }
        }

        KotlinTestUtils.assertEqualsToFile(expectedFile, actualContent)
    }

    private fun checkWithJavac(files: List<TestFile>): String {
        val locale = Locale.US
        val compiler = ToolProvider.getSystemJavaCompiler()
        val diagnostics = mutableListOf<String>()

        val diagnosticListener = DiagnosticListener<JavaFileObject> { diagnostic ->
            val path = diagnostic.source.name
            val line = diagnostic.lineNumber
            val column = diagnostic.columnNumber
            val message = diagnostic.getMessage(locale)
            diagnostics += "$path[$line:$column]: $message"
        }

        val fileManager = JavaInMemoryCompiler.JavaMemFileManager()
        val javaFiles = files.map { createJavaFile(it) }
        val task = compiler.getTask(null, fileManager, diagnosticListener, null, null, javaFiles)
        task.call()

        return diagnostics.joinToString("\n")
    }

    private fun createJavaFile(file: TestFile): SimpleJavaFileObject {
        val uri = File(file.name).toURI()
        return object : SimpleJavaFileObject(uri, JavaFileObject.Kind.SOURCE) {
            private fun unsupported(): Nothing = throw UnsupportedOperationException("File is read-only")

            override fun getName() = file.name
            override fun getNestingKind() = NestingKind.TOP_LEVEL

            override fun openInputStream() = file.content.byteInputStream()
            override fun getCharContent(ignoreEncodingErrors: Boolean) = file.content

            override fun openWriter() = unsupported()
            override fun openOutputStream() = unsupported()
            override fun delete() = unsupported()
        }
    }
}

class KaptLiteAnalysisHandlerExtensionForTests(
    configuration: CompilerConfiguration,
    override val messageCollector: MessageCollector
) : AbstractKaptLiteAnalysisHandlerExtension(configuration) {
    val output = TestGeneratorOutput()
    override fun getOutput(state: GenerationState) = output
}

class TestGeneratorOutput : GeneratorOutput {
    val files = mutableListOf<TestFile>()

    override fun produce(internalName: String, path: String, block: CodeScope.() -> Unit) {
        val sb = StringBuilder()
        CodeScope(sb).block()
        files += TestFile(path, sb.toString())
    }
}