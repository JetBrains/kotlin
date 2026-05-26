/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.converters

import org.jetbrains.kotlin.cli.pipeline.web.WebLoadedIrPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.web.js.JsCodegenPipelinePhase
import org.jetbrains.kotlin.cli.pipeline.web.js.JsIrLoweringPipelinePhase
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
import org.jetbrains.kotlin.js.config.WebArtifactConfiguration
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

        val [compilerResult, icCache] = if (JsEnvironmentConfigurator.incrementalEnabled(testServices)) {
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
        (val irModuleFragment = module, val moduleDependencies = dependencies, val _ = bultins, val _ = symbolTable, val _ = deserializer) = inputArtifact.cliArtifact.moduleInfo

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
        val moduleKind = configuration.get(JSConfigurationKeys.MODULE_KIND, ModuleKind.PLAIN)

        val sourceMapsEnabled = JsEnvironmentConfigurationDirectives.GENERATE_SOURCE_MAP in module.directives
        val dontSkipRegularMode = JsEnvironmentConfigurationDirectives.SKIP_REGULAR_MODE !in module.directives
        val delegateTranspilationToExternalTool =
            JsEnvironmentConfigurationDirectives.DELEGATE_JS_TRANSPILATION in module.directives &&
                    JsEnvironmentConfigurationDirectives.ES6_MODE !in module.directives


        if (dontSkipRegularMode) {
            for ([mode, output] in compilerResult) {
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
        val writtenFiles = writeAll()

        forEachModule { artifactConfiguration, depModuleId ->
            val builtJsFilePath = artifactConfiguration.outputJsFile()
            if (builtJsFilePath in writtenFiles) {
                val newFile = depModuleId?.let(outputFile::augmentWithModuleName) ?: outputFile
                val moduleId = depModuleId?.let { "./$it.js" } ?: artifactConfiguration.moduleName
                builtJsFilePath.fixJsFile(rootDir, newFile, moduleId, artifactConfiguration.moduleKind)
            }
        }

        val outputDtsFile = outputFile.withReplacedExtensionOrNull(".js", ".d.ts")
            ?: outputFile.withReplacedExtensionOrNull(".mjs", ".d.mts")
            ?: error("Output file $outputFile has unexpected extension")

        forEachModule { artifactConfiguration, depModuleId ->
            val builtDtsFilePath = artifactConfiguration.outputDtsFile()
            if (builtDtsFilePath in writtenFiles) {
                val newFile = depModuleId?.let(outputDtsFile::augmentWithModuleName) ?: outputDtsFile
                builtDtsFilePath.copyTo(newFile)
                builtDtsFilePath.delete()
            }
        }

        artifactConfiguration.outputDirectory.deleteRecursively()
    }

    private fun CompilationOutputs.forEachModule(body: (WebArtifactConfiguration, String?) -> Unit) {
        body(artifactConfiguration, null)
        for (dependency in dependencies) {
            body(dependency.artifactConfiguration, dependency.artifactConfiguration.moduleName)
        }
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
        endsWith(".d.ts") -> ".d.ts"
        endsWith(".d.mts") -> ".d.mts"
        else -> error("Unexpected file '$this' extension")
    }

    return if (suffix == ESM_EXTENSION) {
        replaceAfterLast(File.separator, moduleName.minifyPathForWindowsIfNeeded().replace("./", "")).removeSuffix(suffix) + suffix
    } else {
        removeSuffix("_v5$suffix") + "-${moduleName}_v5$suffix"
    }
}

fun File.augmentWithModuleName(moduleName: String): File = File(absolutePath.augmentWithModuleName(moduleName))
