/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.converters

import org.jetbrains.kotlin.backend.common.IrModuleInfo
import org.jetbrains.kotlin.backend.common.serialization.cityHash64
import org.jetbrains.kotlin.cli.common.isWindows
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.phaseConfig
import org.jetbrains.kotlin.ir.backend.js.*
import org.jetbrains.kotlin.ir.backend.js.ic.JsExecutableProducer
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.CompilationOutputs
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.IrModuleToJsTransformer
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.TranslationMode
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.safeName
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.js.backend.ast.ESM_EXTENSION
import org.jetbrains.kotlin.js.backend.ast.REGULAR_EXTENSION
import org.jetbrains.kotlin.js.config.*
import org.jetbrains.kotlin.js.test.handlers.JsBoxRunner
import org.jetbrains.kotlin.js.test.utils.extractTestPackage
import org.jetbrains.kotlin.js.test.utils.jsIrIncrementalDataProvider
import org.jetbrains.kotlin.js.test.utils.wrapWithModuleEmulationMarkers
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator.Companion.getJsModuleArtifactName
import org.jetbrains.kotlin.test.services.configuration.createJsTestPhaseConfig
import org.jetbrains.kotlin.test.services.defaultsProvider
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.fileUtils.withReplacedExtensionOrNull
import org.jetbrains.kotlin.js.test.tools.SwcRunner
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
        require(inputArtifact is IrBackendInput.JsIrDeserializedFromKlibBackendInput) {
            "JsIrLoweringFacade expects IrBackendInput.JsIrDeserializedFromKlibBackendInput as input"
        }

        return compileIrToJs(
            module,
            inputArtifact.moduleInfo,
            testServices.compilerConfigurationProvider.getCompilerConfiguration(module),
            inputArtifact.klib,
        )
    }

    private fun compileIrToJs(
        module: TestModule,
        moduleInfo: IrModuleInfo,
        configuration: CompilerConfiguration,
        klib: File,
    ): BinaryArtifacts.Js? {
        val (irModuleFragment, moduleDependencies, irBuiltIns, symbolTable, deserializer) = moduleInfo

        val splitPerModule = JsEnvironmentConfigurationDirectives.SPLIT_PER_MODULE in module.directives
        val splitPerFile = JsEnvironmentConfigurationDirectives.SPLIT_PER_FILE in module.directives
        val perModule = JsEnvironmentConfigurationDirectives.PER_MODULE in module.directives
        val keep = module.directives[JsEnvironmentConfigurationDirectives.KEEP].toSet()

        val moduleKind = JsEnvironmentConfigurator.getModuleKind(testServices, module)
        val granularity = when {
            !firstTimeCompilation -> JsGenerationGranularity.WHOLE_PROGRAM
            splitPerFile || moduleKind == ModuleKind.ES -> JsGenerationGranularity.PER_FILE
            splitPerModule || perModule -> JsGenerationGranularity.PER_MODULE
            else -> JsGenerationGranularity.WHOLE_PROGRAM
        }

        val testPackage = extractTestPackage(testServices, ignoreEsModules = false)
        val skipRegularMode = JsEnvironmentConfigurationDirectives.SKIP_REGULAR_MODE in module.directives

        if (skipRegularMode) return null

        if (JsEnvironmentConfigurator.incrementalEnabled(testServices)) {
            val outputFile = if (firstTimeCompilation) {
                File(JsEnvironmentConfigurator.getJsModuleArtifactPath(testServices, module.name) + moduleKind.jsExtension)
            } else {
                File(JsEnvironmentConfigurator.getRecompiledJsModuleArtifactPath(testServices, module.name) + moduleKind.jsExtension)
            }

            val compiledModule = CompilerResult(
                outputs = listOf(TranslationMode.FULL_DEV, TranslationMode.PER_MODULE_DEV).associateWith {
                    val jsExecutableProducer = JsExecutableProducer(
                        mainModuleName = configuration.getNotNull(CommonConfigurationKeys.MODULE_NAME),
                        moduleKind = configuration.get(JSConfigurationKeys.MODULE_KIND, ModuleKind.PLAIN),
                        sourceMapsInfo = SourceMapsInfo.from(configuration),
                        caches = testServices.jsIrIncrementalDataProvider.getCaches(),
                        relativeRequirePath = false,
                        dtsCompilationStrategy = configuration.dtsCompilationStrategy ?: error("dtsCompilationStrategy was not provided")
                    )
                    jsExecutableProducer.buildExecutable(it.granularity, true).compilationOut
                }
            )
            return BinaryArtifacts.Js.JsIrArtifact(
                outputFile, compiledModule, testServices.jsIrIncrementalDataProvider.getCacheForModule(module)
            ).dump(module, firstTimeCompilation)
        }

        configuration.phaseConfig = createJsTestPhaseConfig(testServices, module)

        val mainArguments = JsEnvironmentConfigurator.getMainCallParametersForModule(module)

        val loweredIr = compileIr(
            moduleFragment = irModuleFragment.apply { resolveTestPaths() },
            mainModule = MainModule.Klib(klib.absolutePath),
            mainCallArguments = mainArguments,
            configuration = configuration,
            moduleDependencies = moduleDependencies.apply { all.onEach { it.resolveTestPaths() } },
            irBuiltIns = irBuiltIns,
            symbolTable = symbolTable,
            irLinker = deserializer,
            exportedDeclarations = setOf(FqName.fromSegments(listOfNotNull(testPackage, JsBoxRunner.TEST_FUNCTION))),
            keep = keep,
            dceRuntimeDiagnostic = null,
            safeExternalBoolean = JsEnvironmentConfigurationDirectives.SAFE_EXTERNAL_BOOLEAN in module.directives,
            safeExternalBooleanDiagnostic = module.directives[JsEnvironmentConfigurationDirectives.SAFE_EXTERNAL_BOOLEAN_DIAGNOSTIC].singleOrNull(),
            granularity = granularity,
        )

        return loweredIr2JsArtifact(module, loweredIr, mainArguments != null)
    }

    private fun loweredIr2JsArtifact(module: TestModule, loweredIr: LoweredIr, shouldReferMainFunction: Boolean): BinaryArtifacts.Js {
        val moduleKind = JsEnvironmentConfigurator.getModuleKind(testServices, module)
        val isEsModules = moduleKind == ModuleKind.ES

        val outputFile = File(
            JsEnvironmentConfigurator.getJsModuleArtifactPath(testServices, module.name, TranslationMode.FULL_DEV).finalizePath(moduleKind)
        )

        val transformer = IrModuleToJsTransformer(
            loweredIr.context,
            moduleToName = runIf(isEsModules) {
                loweredIr.allModules.associateWith {
                    "./${getJsModuleArtifactName(testServices, it.safeName)}".minifyIfNeed()
                }
            } ?: emptyMap(),
            shouldReferMainFunction,
        )
        val translationModes = JsEnvironmentConfigurator.getTranslationModesForTest(testServices, module)
        val compilationOut = transformer.generateModule(loweredIr.allModules, translationModes, isEsModules)
        return BinaryArtifacts.Js.JsIrArtifact(outputFile, compilationOut).dump(module)
    }

    private fun IrModuleFragment.resolveTestPaths() {
        files.forEach(jsIrPathReplacer::lower)
    }

    private fun BinaryArtifacts.Js.JsIrArtifact.dump(
        module: TestModule,
        firstTimeCompilation: Boolean = true
    ): BinaryArtifacts.Js.JsIrArtifact {
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
            for ((mode, output) in compilerResult.outputs.entries) {
                val outputFile = if (firstTimeCompilation) {
                    File(JsEnvironmentConfigurator.getJsModuleArtifactPath(testServices, module.name, mode).finalizePath(moduleKind))
                } else {
                    File(
                        JsEnvironmentConfigurator.getRecompiledJsModuleArtifactPath(
                            testServices,
                            module.name,
                            mode
                        ).finalizePath(moduleKind)
                    )
                }

                output.writeTo(outputFile, moduleId, moduleKind, mode.granularity)

                if (delegateTranspilationToExternalTool) {
                    SwcRunner.exec(outputFile.parentFile, moduleKind, mode, sourceMapsEnabled)
                }
            }
        }

        // Here we're checking only merged variant of TypeScript
        // After KT-48979 implemented it should be changed
        if (generateDts) {
            val tsFiles = compilerResult.outputs.entries.associate { it.value.tsDefinitions to it.key }
            val tsDefinitions = tsFiles.entries.singleOrNull()?.key
                ?: error("[${tsFiles.values.joinToString { it.name }}] make different TypeScript")

            outputFile
                .withReplacedExtensionOrNull("_v5${moduleKind.jsExtension}", ".d.ts")!!
                .write(tsDefinitions.body)
        }

        return this
    }

    private fun File.fixJsFile(rootDir: File, newJsTarget: File, moduleId: String, moduleKind: ModuleKind) {
        val newJsCode = wrapWithModuleEmulationMarkers(readText(), moduleKind, moduleId)
        val jsCodeWithCorrectImportPath = jsIrPathReplacer.replacePathTokensWithRealPath(newJsCode, newJsTarget, rootDir)

        val oldJsMap = File("$absolutePath.map")
        val jsCodeMap = (moduleKind == ModuleKind.PLAIN && oldJsMap.exists()).ifTrue { oldJsMap.readText() }

        this.delete()
        oldJsMap.delete()

        newJsTarget.write(jsCodeWithCorrectImportPath)
        jsCodeMap?.let { File("${newJsTarget.absolutePath}.map").write(it) }
    }

    private fun CompilationOutputs.writeTo(
        outputFile: File,
        moduleId: String,
        moduleKind: ModuleKind,
        granularity: JsGenerationGranularity,
    ) {
        val rootDir = outputFile.parentFile
        val tmpBuildDir = rootDir.resolve("tmp-build")
        // CompilationOutputs keeps the `outputDir` clean by removing all outdated JS and other unknown files.
        // To ensure that useful files around `outputFile`, such as irdump, are not removed, use `tmpBuildDir` instead.
        val artifactConfiguration = WebArtifactConfiguration(
            moduleKind,
            moduleId,
            tmpBuildDir,
            outputFile.nameWithoutExtension,
            granularity,
            TsCompilationStrategy.NONE
        )
        val allJsFiles = writeAll(artifactConfiguration).filter {
            it.extension == "js" || it.extension == "mjs"
        }

        val mainModuleFile = allJsFiles.last()
        mainModuleFile.fixJsFile(rootDir, outputFile, moduleId, moduleKind)

        dependencies.map { it.first }.zip(allJsFiles.dropLast(1)).forEach { (depModuleId, builtJsFilePath) ->
            val newFile = outputFile.augmentWithModuleName(depModuleId)
            builtJsFilePath.fixJsFile(rootDir, newFile, depModuleId, moduleKind)
        }
        tmpBuildDir.deleteRecursively()
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
        replaceAfterLast(File.separator, moduleName.minifyIfNeed().replace("./", "")).removeSuffix(suffix) + suffix
    } else {
        return removeSuffix("_v5$suffix") + "-${moduleName}_v5$suffix"
    }
}

fun String.finalizePath(moduleKind: ModuleKind): String {
    return plus(moduleKind.jsExtension).minifyIfNeed()
}

// D8 ignores Windows settings related to extending of maximum path symbols count
// The hack should be deleted when D8 fixes the bug.
// The issue is here: https://bugs.chromium.org/p/v8/issues/detail?id=13318
fun String.minifyIfNeed(): String {
    if (!isWindows) return this
    val delimiter = if (contains('\\')) '\\' else '/'
    val directoryPath = substringBeforeLast(delimiter)
    val fileFullName = substringAfterLast(delimiter)
    val fileName = fileFullName.substringBeforeLast('.')

    if (fileName.length <= 80) return this

    val fileExtension = fileFullName.substringAfterLast('.')
    val extensionPart = if (fileExtension.isEmpty()) "" else ".$fileExtension"

    return "$directoryPath$delimiter${fileName.cityHash64().toULong().toString(16)}$extensionPart"
}

fun File.augmentWithModuleName(moduleName: String): File = File(absolutePath.augmentWithModuleName(moduleName))
