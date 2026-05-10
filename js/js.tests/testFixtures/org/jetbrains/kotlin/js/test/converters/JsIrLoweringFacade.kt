/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.converters

import org.jetbrains.kotlin.cli.pipeline.web.WebLoadedIrPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.web.js.JsCodegenPipelinePhase
import org.jetbrains.kotlin.cli.pipeline.web.js.JsIrLoweringPipelinePhase
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.ir.backend.js.SourceMapsInfo
import org.jetbrains.kotlin.ir.backend.js.ic.JsExecutableProducer
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.CompilationOutputs
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.CompilerResult
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.TranslationMode
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.js.backend.ast.ESM_EXTENSION
import org.jetbrains.kotlin.js.backend.ast.REGULAR_EXTENSION
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.config.ModuleKind
import org.jetbrains.kotlin.js.config.artifactConfigurations
import org.jetbrains.kotlin.js.test.tools.SwcRunner
import org.jetbrains.kotlin.js.test.utils.jsIrIncrementalDataProvider
import org.jetbrains.kotlin.js.test.utils.wrapWithModuleEmulationMarkers
import org.jetbrains.kotlin.test.backend.ir.DeserializedFromKlibBackendInput
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.frontend.fir.processErrorFromCliPhase
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.finalizePath
import org.jetbrains.kotlin.test.services.configuration.minifyPathForWindowsIfNeeded
import org.jetbrains.kotlin.test.services.defaultsProvider
import org.jetbrains.kotlin.utils.fileUtils.withReplacedExtensionOrNull
import java.io.File

class JsIrLoweringFacade(
    testServices: TestServices,
    private val firstTimeCompilation: Boolean,
) : BackendFacade<IrBackendInput, BinaryArtifacts.Js>(testServices, BackendKinds.IrBackend, ArtifactKinds.Js) {

    private val jsIrPathReplacer by lazy { JsIrPathReplacer(testServices) }

    override fun shouldTransform(module: TestModule): Boolean {
        return with(testServices.defaultsProvider) {
            backendKind == inputKind && artifactKind == outputKind
        } && JsEnvironmentConfigurator.isMainModule(module, testServices)
    }

    override fun transform(module: TestModule, inputArtifact: IrBackendInput): BinaryArtifacts.Js? {
        require(JsEnvironmentConfigurator.isMainModule(module, testServices))
        require(inputArtifact is DeserializedFromKlibBackendInput<*>) {
            "JsIrLoweringFacade expects IrBackendInput.DeserializedFromKlibBackendInput as input"
        }

        val skipRegularMode = JsEnvironmentConfigurationDirectives.SKIP_REGULAR_MODE in module.directives

        if (skipRegularMode) return null

        val (compilerResult, icCache) = if (JsEnvironmentConfigurator.incrementalEnabled(testServices)) {
            compileIncrementally(inputArtifact, module)
        } else {
            compileNonIncrementally(inputArtifact)
        } ?: return null

        val outputFile = File(
            JsEnvironmentConfigurator.getJsModuleArtifactPath(testServices, module.name, TranslationMode.FULL_DEV, firstTimeCompilation)
                .finalizePath(JsEnvironmentConfigurator.getModuleKind(testServices, module)),
        )

        return JsIrArtifact(outputFile, compilerResult, icCache).dump(module, firstTimeCompilation)
    }

    private fun compileIncrementally(
        inputArtifact: DeserializedFromKlibBackendInput<*>,
        module: TestModule,
    ): Pair<CompilerResult, Map<String, ByteArray>?> {
        val configuration = inputArtifact.cliArtifact.configuration
        return CompilerResult(
            configuration.artifactConfigurations.map {
                val jsExecutableProducer = JsExecutableProducer(
                    artifactConfiguration = it,
                    sourceMapsInfo = SourceMapsInfo.from(configuration),
                    caches = testServices.jsIrIncrementalDataProvider.getCaches(),
                )
                jsExecutableProducer.buildExecutable(true).compilationOut
            },
        ) to testServices.jsIrIncrementalDataProvider.getCacheForModule(module)
    }

    private fun compileNonIncrementally(inputArtifact: DeserializedFromKlibBackendInput<*>): Pair<CompilerResult, Map<String, ByteArray>?>? {
        val (irModuleFragment, moduleDependencies, _, _, _) = inputArtifact.cliArtifact.moduleInfo

        irModuleFragment.resolveTestPaths()
        moduleDependencies.all.forEach { it.resolveTestPaths() }

        val cliInputArtifact = inputArtifact.cliArtifact as? WebLoadedIrPipelineArtifact
            ?: error("JsIrLoweringFacade expects WebLoadedIrPipelineArtifact")
        val loweredIr = JsIrLoweringPipelinePhase.executePhase(cliInputArtifact)
            ?: return processErrorFromCliPhase(inputArtifact.cliArtifact.configuration, testServices)

        val output = JsCodegenPipelinePhase.executePhase(loweredIr)
            ?: return processErrorFromCliPhase(loweredIr.configuration, testServices)

        return output.result to null
    }

    private fun IrModuleFragment.resolveTestPaths() {
        files.forEach(jsIrPathReplacer::lower)
    }

    private fun JsIrArtifact.dump(
        module: TestModule,
        firstTimeCompilation: Boolean = true
    ): JsIrArtifact {
        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
        val moduleId = configuration.getNotNull(CommonConfigurationKeys.MODULE_NAME)
        val moduleKind = configuration.get(JSConfigurationKeys.MODULE_KIND, ModuleKind.PLAIN)

        val generateDts = JsEnvironmentConfigurationDirectives.GENERATE_DTS in module.directives
        val sourceMapsEnabled = JsEnvironmentConfigurationDirectives.GENERATE_SOURCE_MAP in module.directives
        val dontSkipRegularMode = JsEnvironmentConfigurationDirectives.SKIP_REGULAR_MODE !in module.directives
        val delegateTranspilationToExternalTool =
            JsEnvironmentConfigurationDirectives.DELEGATE_JS_TRANSPILATION in module.directives &&
                    JsEnvironmentConfigurationDirectives.ES6_MODE !in module.directives


        if (dontSkipRegularMode) {
            for ((mode, output) in compilerResult.entries) {
                val outputFile = File(
                    JsEnvironmentConfigurator.getJsModuleArtifactPath(testServices, module.name, mode, firstTimeCompilation)
                        .finalizePath(moduleKind)
                )

                output.writeTo(outputFile)

                if (delegateTranspilationToExternalTool) {
                    SwcRunner.exec(output.rootDir, moduleKind, mode, sourceMapsEnabled)
                }
            }
        }

        if (generateDts) {
            val tsFiles = compilerResult.entries.associate { it.value.getFullTsDefinition(moduleId, moduleKind) to it.key }
            val tsDefinitions = tsFiles.entries.singleOrNull()?.key
                ?: error("[${tsFiles.values.joinToString { it.name }}] make different TypeScript")

            outputFile
                .withReplacedExtensionOrNull("_v5${moduleKind.jsExtension}", ".d.ts")!!
                .write(tsDefinitions)
        }

        return this
    }

    private fun File.fixJsFile(rootDir: File, newJsTarget: File, moduleId: String, moduleKind: ModuleKind) {
        val newJsCode = wrapWithModuleEmulationMarkers(readText(), moduleKind, moduleId)
        val jsCodeWithCorrectImportPath = jsIrPathReplacer.replacePathTokensWithRealPath(newJsCode, newJsTarget, rootDir)

        delete()
        newJsTarget.write(jsCodeWithCorrectImportPath)

        File("$absolutePath.map")
            .takeIf { it.exists() }
            ?.let {
                it.copyTo(File("${newJsTarget.absolutePath}.map"))
                it.delete()
            }
    }

    private val CompilationOutputs.rootDir: File
        get() = artifactConfiguration.outputDirectory.parentFile

    private fun CompilationOutputs.writeTo(outputFile: File) {
        val allJsFiles = writeAll().filter {
            it.extension == "js" || it.extension == "mjs"
        }

        val mainModuleFile = allJsFiles.last()
        mainModuleFile.fixJsFile(rootDir, outputFile, artifactConfiguration.moduleName, artifactConfiguration.moduleKind)

        dependencies.map { it.artifactConfiguration.moduleName }.zip(allJsFiles.dropLast(1)).forEach { (depModuleId, builtJsFilePath) ->
            val newFile = outputFile.augmentWithModuleName(depModuleId)
            builtJsFilePath.fixJsFile(rootDir, newFile, "./$depModuleId.js", artifactConfiguration.moduleKind)
        }
        artifactConfiguration.outputDirectory.deleteRecursively()
    }

    private fun File.write(text: String) {
        parentFile.mkdirs()
        writeText(text)
    }
}

fun String.augmentWithModuleName(moduleName: String): String {
    val suffix = when {
        endsWith(ESM_EXTENSION) -> ESM_EXTENSION
        endsWith(REGULAR_EXTENSION) -> REGULAR_EXTENSION
        else -> error("Unexpected file '$this' extension")
    }

    return if (suffix == ESM_EXTENSION) {
        replaceAfterLast(File.separator, moduleName.minifyPathForWindowsIfNeeded().replace("./", "")).removeSuffix(suffix) + suffix
    } else {
        return removeSuffix("_v5$suffix") + "-${moduleName}_v5$suffix"
    }
}

fun File.augmentWithModuleName(moduleName: String): File = File(absolutePath.augmentWithModuleName(moduleName))
