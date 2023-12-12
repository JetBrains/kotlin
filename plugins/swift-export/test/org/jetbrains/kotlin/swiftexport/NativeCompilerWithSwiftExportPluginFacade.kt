/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.cli.bc.K2Native
import org.jetbrains.kotlin.cli.common.CLITool
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.CompilerTestUtil
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.test.services.getKtFilesForSourceFiles
import org.jetbrains.kotlin.test.services.sourceFileProvider
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.reflect.KClass
import kotlin.test.assertEquals

internal class NativeCompilerWithSwiftExportPluginFacade(
    private val testServices: TestServices,
) : AbstractTestFacade<ResultingArtifact.Source, SwiftExportArtifact>() {
    override val inputKind: TestArtifactKind<ResultingArtifact.Source>
        get() = SourcesKind
    override val outputKind: TestArtifactKind<SwiftExportArtifact>
        get() = SwiftExportArtifact.Kind

    private val tmpDir = FileUtil.createTempDirectory("SwiftExportIntegrationTests", null, false)

    override fun transform(module: TestModule, inputArtifact: ResultingArtifact.Source): SwiftExportArtifact {
        val configurationProvider = testServices.compilerConfigurationProvider
        val project = configurationProvider.getProject(module)
        val ktFiles = testServices.sourceFileProvider.getKtFilesForSourceFiles(module.files, project, findViaVfs = true).values.toList()

        val outputDirPath = tmpDir.absolutePath + "/" + "swift_export_output"
        val outputDir = File(outputDirPath)
        outputDir.mkdirs()

        runTest(
            K2Native(),
            ktFiles,
            outputDir
        )

        return SwiftExportArtifact(
            File(outputDir.absolutePath + "/result.swift"),
            File(outputDir.absolutePath + "/result.h"),
            File(outputDir.absolutePath + "/result.kt"),
        )
    }

    private fun runTest(
        compiler: CLITool<*>,
        src: List<KtFile>,
        outputDir: File,
    ) {
        val sources = src.map {
            tmpDir.resolve(it.name).apply {
                writeText(it.text)
            }
        }

        val plugin = writePlugin(
            SwiftExportCommandLineProcessor::class,
            SwiftExportComponentRegistrar::class,
        )
        val args = listOf(
            "-Xplugin=$plugin",
            "-P", "plugin:org.jetbrains.kotlin.swiftexport:output_dir=${outputDir}"
        ) + sources.map { it.absolutePath }

        val outputPath = listOf(
            "-language-version", "2.0",
            "-produce", "library",
        )

        val (output, exitCode) = CompilerTestUtil.executeCompiler(
            compiler,
            args + outputPath
        )
        assertEquals(ExitCode.OK, exitCode, output)
    }

    private fun writePlugin(
        cliProcessor: KClass<out CommandLineProcessor>,
        registrarKClass: KClass<out CompilerPluginRegistrar>,
    ): String {
        val jarFile = tmpDir.resolve("plugin.jar")
        ZipOutputStream(jarFile.outputStream()).use {
            val entryRegistry = ZipEntry("META-INF/services/org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar")
            it.putNextEntry(entryRegistry)
            it.write(registrarKClass.java.name.toByteArray())

            val entryCLI = ZipEntry("META-INF/services/org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor")
            it.putNextEntry(entryCLI)
            it.write(cliProcessor.java.name.toByteArray())
        }
        return jarFile.absolutePath
    }

    override fun shouldRunAnalysis(module: TestModule): Boolean {
        return true
    }
}