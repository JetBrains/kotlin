/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.integration

import com.intellij.mock.MockProject
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.cliArgument
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.test.CompilerTestUtil
import org.jetbrains.kotlin.test.TestCaseWithTmpdir
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

private data class TestKtFile(
    val name: String,
    val content: String
)

private val classNotFound = TestKtFile("C.kt", "class C : ClassNotFound")
private val repeatedAnalysis = TestKtFile("D.kt", "class D : Generated")

class JsIrAnalysisHandlerExtensionTest : TestCaseWithTmpdir() {

    // Writes the service file only; CustomComponentRegistrar is already in classpath.
    private fun writePlugin(): String {
        val jarFile = tmpdir.resolve("plugin.jar")
        ZipOutputStream(jarFile.outputStream()).use {
            val entry = ZipEntry("META-INF/services/org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar")
            it.putNextEntry(entry)
            it.write(CustomComponentRegistrar::class.java.name.toByteArray())
        }
        return jarFile.absolutePath
    }

    private val jsirStdlib: String?
        get() = System.getProperty("kotlin.js.full.stdlib.path")

    private val wasmStdlib: String?
        get() = System.getProperty("kotlin.wasm.stdlib.path")

    private val outjs: String
        get() = tmpdir.resolve("out.js").absolutePath

    private val outklib: String
        get() = tmpdir.resolve("out.klib").absolutePath

    private fun runTest(
        compiler: CLICompiler<*>, src: TestKtFile, libs: String, outFile: String, extras: List<String> = emptyList(),
        messageRenderer: MessageRenderer? = null,
    ) {
        val mainKt = tmpdir.resolve(src.name).apply {
            writeText(src.content)
        }
        val plugin = writePlugin()
        val outputFile = File(outFile)
        val args = listOf(
            K2JSCompilerArguments::pluginClasspaths.cliArgument(plugin),
            K2JSCompilerArguments::libraries.cliArgument, libs,
            K2JSCompilerArguments::outputDir.cliArgument, outputFile.parentFile.path,
            K2JSCompilerArguments::moduleName.cliArgument, outputFile.nameWithoutExtension,
            K2JSCompilerArguments::languageVersion.cliArgument, "1.9",
            mainKt.absolutePath
        )
        CompilerTestUtil.executeCompilerAssertSuccessful(compiler, args + extras, messageRenderer)
    }

    fun testShouldNotGenerateCodeJs() {
        if (jsirStdlib != null)
            runTest(K2JSCompiler(), classNotFound, jsirStdlib!!, outjs, listOf(K2JSCompilerArguments::irProduceJs.cliArgument))
    }

    fun testShouldNotGenerateCodeKlib() {
        if (jsirStdlib != null)
            runTest(K2JSCompiler(), classNotFound, jsirStdlib!!, outklib, listOf(K2JSCompilerArguments::irProduceKlibFile.cliArgument))
    }

    fun testShouldNotGenerateCodeWasm() {
        if (jsirStdlib != null && wasmStdlib != null)
            runTest(K2JSCompiler(), classNotFound, "$jsirStdlib,$wasmStdlib", outjs, listOf(K2JSCompilerArguments::irProduceJs.cliArgument, K2JSCompilerArguments::wasm.cliArgument))
    }

    fun testRepeatedAnalysisJs() {
        if (jsirStdlib != null)
            runTest(K2JSCompiler(), repeatedAnalysis, jsirStdlib!!, outjs, listOf(K2JSCompilerArguments::irProduceJs.cliArgument))
    }

    fun testRepeatedAnalysisKlib() {
        if (jsirStdlib != null)
            runTest(K2JSCompiler(), repeatedAnalysis, jsirStdlib!!, outklib, listOf(K2JSCompilerArguments::irProduceKlibFile.cliArgument))
    }

    fun testRepeatedAnalysisWasm() {
        if (jsirStdlib != null && wasmStdlib != null)
            runTest(
                K2JSCompiler(), repeatedAnalysis, "$jsirStdlib,$wasmStdlib", outjs,
                listOf(K2JSCompilerArguments::irProduceJs.cliArgument, K2JSCompilerArguments::wasm.cliArgument)
            )
    }
}

class CustomComponentRegistrar : ComponentRegistrar {
    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        AnalysisHandlerExtension.registerExtension(project, CustomAnalysisHandler())
    }
}

// Assume that the directory of input files is writeable!
private class CustomAnalysisHandler : AnalysisHandlerExtension {
    override fun doAnalysis(
        project: Project,
        module: ModuleDescriptor,
        projectContext: ProjectContext,
        files: Collection<KtFile>,
        bindingTrace: BindingTrace,
        componentProvider: ComponentProvider
    ): AnalysisResult? {
        val filenames = files.map { it.name }
        if (repeatedAnalysis.name in filenames) {
            if ("Generated.kt" in filenames) {
                return null
            } else {
                val outDir = File(files.single { it.name == repeatedAnalysis.name }.virtualFilePath).parentFile
                outDir.resolve("Generated.kt").apply {
                    writeText("interface Generated")
                }
                return AnalysisResult.RetryWithAdditionalRoots(BindingContext.EMPTY, module, emptyList(), listOf(outDir))
            }
        }
        return AnalysisResult.success(BindingContext.EMPTY, module, false)
    }
}
