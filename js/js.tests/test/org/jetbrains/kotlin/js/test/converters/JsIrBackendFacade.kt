/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.converters

import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.common.phaser.toPhaseMap
import org.jetbrains.kotlin.backend.common.serialization.cityHash64
import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureDescriptor
import org.jetbrains.kotlin.cli.common.isWindows
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.backend.js.*
import org.jetbrains.kotlin.ir.backend.js.ic.JsExecutableProducer
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsManglerDesc
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.*
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.backend.js.SourceMapsInfo
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImplForJsIC
import org.jetbrains.kotlin.ir.linkage.partial.PartialLinkageConfig
import org.jetbrains.kotlin.ir.linkage.partial.PartialLinkageLogLevel
import org.jetbrains.kotlin.ir.linkage.partial.PartialLinkageMode
import org.jetbrains.kotlin.ir.linkage.partial.setupPartialLinkageConfig
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.irMessageLogger
import org.jetbrains.kotlin.js.backend.ast.ESM_EXTENSION
import org.jetbrains.kotlin.js.backend.ast.REGULAR_EXTENSION
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.test.handlers.JsBoxRunner.Companion.TEST_FUNCTION
import org.jetbrains.kotlin.js.test.utils.extractTestPackage
import org.jetbrains.kotlin.js.test.utils.jsIrIncrementalDataProvider
import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.test.DebugMode
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.frontend.classic.moduleDescriptorProvider
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator.Companion.getJsArtifactSimpleName
import org.jetbrains.kotlin.test.services.configuration.getDependencies
import org.jetbrains.kotlin.test.services.libraryProvider
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.fileUtils.withReplacedExtensionOrNull
import java.io.File

class JsIrBackendFacade(
    val testServices: TestServices,
    private val firstTimeCompilation: Boolean
) : AbstractTestFacade<BinaryArtifacts.KLib, BinaryArtifacts.Js>() {
    override val inputKind: ArtifactKinds.KLib get() = ArtifactKinds.KLib
    override val outputKind: ArtifactKinds.Js get() = ArtifactKinds.Js

    private val jsIrPathReplacer by lazy { JsIrPathReplacer(testServices) }

    constructor(testServices: TestServices) : this(testServices, firstTimeCompilation = true)

    override fun transform(module: TestModule, inputArtifact: BinaryArtifacts.KLib): BinaryArtifacts.Js? {
        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)

        // Enforce PL with the ERROR log level to fail any tests where PL detected any incompatibilities.
        configuration.setupPartialLinkageConfig(PartialLinkageConfig(PartialLinkageMode.ENABLE, PartialLinkageLogLevel.ERROR))

        val isMainModule = JsEnvironmentConfigurator.isMainModule(module, testServices)
        if (!isMainModule) return null

        val moduleInfo = loadIrFromKlib(module, configuration)
        return compileIrToJs(module, moduleInfo, configuration, inputArtifact)
    }

    private fun compileIrToJs(
        module: TestModule,
        moduleInfo: IrModuleInfo,
        configuration: CompilerConfiguration,
        inputArtifact: BinaryArtifacts.KLib,
    ): BinaryArtifacts.Js? {
        val (irModuleFragment, dependencyModules, _, symbolTable, deserializer) = moduleInfo

        val splitPerModule = JsEnvironmentConfigurationDirectives.SPLIT_PER_MODULE in module.directives
        val splitPerFile = JsEnvironmentConfigurationDirectives.SPLIT_PER_FILE in module.directives
        val perModule = JsEnvironmentConfigurationDirectives.PER_MODULE in module.directives
        val keep = module.directives[JsEnvironmentConfigurationDirectives.KEEP].toSet()

        val granularity = when {
            !firstTimeCompilation -> JsGenerationGranularity.WHOLE_PROGRAM
            splitPerFile || module.kind == ModuleKind.ES -> JsGenerationGranularity.PER_FILE
            splitPerModule || perModule -> JsGenerationGranularity.PER_MODULE
            else -> JsGenerationGranularity.WHOLE_PROGRAM
        }

        val testPackage = extractTestPackage(testServices, ignoreEsModules = false)
        val skipRegularMode = JsEnvironmentConfigurationDirectives.SKIP_REGULAR_MODE in module.directives

        if (skipRegularMode) return null

        if (JsEnvironmentConfigurator.incrementalEnabled(testServices)) {
            val outputFile = if (firstTimeCompilation) {
                File(JsEnvironmentConfigurator.getJsModuleArtifactPath(testServices, module.name) + module.kind.extension)
            } else {
                File(JsEnvironmentConfigurator.getRecompiledJsModuleArtifactPath(testServices, module.name) + module.kind.extension)
            }

            val compiledModule = CompilerResult(
                outputs = listOf(TranslationMode.FULL_DEV, TranslationMode.PER_MODULE_DEV).associateWith {
                    val jsExecutableProducer = JsExecutableProducer(
                        mainModuleName = configuration.getNotNull(CommonConfigurationKeys.MODULE_NAME),
                        moduleKind = configuration.get(JSConfigurationKeys.MODULE_KIND, ModuleKind.PLAIN),
                        sourceMapsInfo = SourceMapsInfo.from(configuration),
                        caches = testServices.jsIrIncrementalDataProvider.getCaches(),
                        relativeRequirePath = false
                    )
                    jsExecutableProducer.buildExecutable(it.granularity, true).compilationOut
                }
            )
            return BinaryArtifacts.Js.JsIrArtifact(
                outputFile, compiledModule, testServices.jsIrIncrementalDataProvider.getCacheForModule(module)
            ).dump(module, firstTimeCompilation)
        }

        val debugMode = DebugMode.fromSystemProperty("kotlin.js.debugMode")
        val phaseConfig = if (debugMode >= DebugMode.SUPER_DEBUG) {
            val dumpOutputDir = File(
                JsEnvironmentConfigurator.getJsArtifactsOutputDir(testServices),
                JsEnvironmentConfigurator.getJsArtifactSimpleName(testServices, module.name) + "-irdump"
            )
            PhaseConfig(
                jsPhases,
                dumpToDirectory = dumpOutputDir.path,
                toDumpStateAfter = jsPhases.toPhaseMap().values.toSet()
            )
        } else {
            PhaseConfig(jsPhases)
        }


        val loweredIr = compileIr(
            irModuleFragment.apply { resolveTestPaths() },
            MainModule.Klib(inputArtifact.outputFile.absolutePath),
            configuration,
            dependencyModules.onEach { it.resolveTestPaths() },
            emptyMap(),
            irModuleFragment.irBuiltins,
            symbolTable,
            deserializer,
            phaseConfig,
            exportedDeclarations = setOf(FqName.fromSegments(listOfNotNull(testPackage, TEST_FUNCTION))),
            keep = keep,
            dceRuntimeDiagnostic = null,
            safeExternalBoolean = JsEnvironmentConfigurationDirectives.SAFE_EXTERNAL_BOOLEAN in module.directives,
            safeExternalBooleanDiagnostic = module.directives[JsEnvironmentConfigurationDirectives.SAFE_EXTERNAL_BOOLEAN_DIAGNOSTIC].singleOrNull(),
            granularity = granularity,
        )

        return loweredIr2JsArtifact(module, loweredIr)
    }

    private fun loweredIr2JsArtifact(module: TestModule, loweredIr: LoweredIr): BinaryArtifacts.Js {
        val mainArguments = JsEnvironmentConfigurator.getMainCallParametersForModule(module)
            .run { if (shouldBeGenerated()) arguments() else null }
        val runIrDce = JsEnvironmentConfigurationDirectives.RUN_IR_DCE in module.directives
        val onlyIrDce = JsEnvironmentConfigurationDirectives.ONLY_IR_DCE in module.directives
        val perModuleOnly = JsEnvironmentConfigurationDirectives.SPLIT_PER_MODULE in module.directives
        val perFileOnly = JsEnvironmentConfigurationDirectives.SPLIT_PER_FILE in module.directives
        val isEsModules = JsEnvironmentConfigurationDirectives.ES_MODULES in module.directives ||
                module.directives[JsEnvironmentConfigurationDirectives.MODULE_KIND].contains(ModuleKind.ES)

        val outputFile =
            File(
                JsEnvironmentConfigurator.getJsModuleArtifactPath(testServices, module.name, TranslationMode.FULL_DEV)
                    .finalizePath(module.kind)
            )

        val transformer = IrModuleToJsTransformer(
            loweredIr.context,
            mainArguments,
            moduleToName = runIf(isEsModules) {
                loweredIr.allModules.associateWith {
                    "./${getJsArtifactSimpleName(testServices, it.safeName)}_v5".minifyIfNeed()
                }
            } ?: emptyMap()
        )
        // If runIrDce then include DCE results
        // If perModuleOnly then skip whole program
        // (it.dce => runIrDce) && (perModuleOnly => it.perModule)
        val translationModes = TranslationMode.values()
            .filter {
                (it.production || !onlyIrDce) &&
                        (!it.production || runIrDce) &&
                        (!perModuleOnly || it.granularity == JsGenerationGranularity.PER_MODULE) &&
                        (!perFileOnly || it.granularity == JsGenerationGranularity.PER_FILE)
            }
            .filter { it.production == it.minimizedMemberNames }
            .filter { isEsModules || it.granularity != JsGenerationGranularity.PER_FILE }
            .toSet()
        val compilationOut = transformer.generateModule(loweredIr.allModules, translationModes, isEsModules)
        return BinaryArtifacts.Js.JsIrArtifact(outputFile, compilationOut).dump(module)
    }

    private fun IrModuleFragment.resolveTestPaths() {
        files.forEach(jsIrPathReplacer::lower)
    }

    private fun loadIrFromKlib(module: TestModule, configuration: CompilerConfiguration): IrModuleInfo {
        val filesToLoad = module.files.takeIf { !firstTimeCompilation }?.map { "/${it.relativePath}" }?.toSet()

        val messageLogger = configuration.irMessageLogger
        val symbolTable = SymbolTable(IdSignatureDescriptor(JsManglerDesc), IrFactoryImplForJsIC(WholeWorldStageController()))

        val moduleDescriptor = testServices.moduleDescriptorProvider.getModuleDescriptor(module)
        val mainModuleLib = testServices.libraryProvider.getCompiledLibraryByDescriptor(moduleDescriptor)
        val friendLibraries = getDependencies(module, testServices, DependencyRelation.FriendDependency)
            .map { testServices.libraryProvider.getCompiledLibraryByDescriptor(it) }
        val friendModules = mapOf(mainModuleLib.uniqueName to friendLibraries.map { it.uniqueName })

        return getIrModuleInfoForKlib(
            moduleDescriptor,
            sortDependencies(JsEnvironmentConfigurator.getAllDependenciesMappingFor(module, testServices)) + mainModuleLib,
            friendModules,
            filesToLoad,
            configuration,
            symbolTable,
            messageLogger,
            loadFunctionInterfacesIntoStdlib = true,
        ) { if (it == mainModuleLib) moduleDescriptor else testServices.libraryProvider.getDescriptorByCompiledLibrary(it) }
    }

    private fun BinaryArtifacts.Js.JsIrArtifact.dump(
        module: TestModule,
        firstTimeCompilation: Boolean = true
    ): BinaryArtifacts.Js.JsIrArtifact {
        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
        val moduleId = configuration.getNotNull(CommonConfigurationKeys.MODULE_NAME)
        val moduleKind = configuration.get(JSConfigurationKeys.MODULE_KIND, ModuleKind.PLAIN)

        val generateDts = JsEnvironmentConfigurationDirectives.GENERATE_DTS in module.directives
        val dontSkipRegularMode = JsEnvironmentConfigurationDirectives.SKIP_REGULAR_MODE !in module.directives

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
                output.writeTo(outputFile, moduleId, moduleKind)
            }
        }

        if (generateDts) {
            val tsFiles = compilerResult.outputs.entries.associate { it.value.getFullTsDefinition(moduleId, moduleKind) to it.key }
            val tsDefinitions = tsFiles.entries.singleOrNull()?.key
                ?: error("[${tsFiles.values.joinToString { it.name }}] make different TypeScript")

            outputFile
                .withReplacedExtensionOrNull("_v5${moduleKind.extension}", ".d.ts")!!
                .write(tsDefinitions)
        }

        return this
    }

    fun File.fixJsFile(rootDir: File, newJsTarget: File, moduleId: String, moduleKind: ModuleKind) {
        val newJsCode = ClassicJsBackendFacade.wrapWithModuleEmulationMarkers(readText(), moduleKind, moduleId)
        val jsCodeWithCorrectImportPath = jsIrPathReplacer.replacePathTokensWithRealPath(newJsCode, newJsTarget, rootDir)

        val oldJsMap = File("$absolutePath.map")
        val jsCodeMap = (moduleKind == ModuleKind.PLAIN && oldJsMap.exists()).ifTrue { oldJsMap.readText() }

        this.delete()
        oldJsMap.delete()

        newJsTarget.write(jsCodeWithCorrectImportPath)
        jsCodeMap?.let { File("${newJsTarget.absolutePath}.map").write(it) }
    }

    private fun CompilationOutputs.writeTo(outputFile: File, moduleId: String, moduleKind: ModuleKind) {
        val rootDir = outputFile.parentFile
        val tmpBuildDir = rootDir.resolve("tmp-build")
        // CompilationOutputs keeps the `outputDir` clean by removing all outdated JS and other unknown files.
        // To ensure that useful files around `outputFile`, such as irdump, are not removed, use `tmpBuildDir` instead.
        val allJsFiles = writeAll(tmpBuildDir, outputFile.nameWithoutExtension, false, moduleId, moduleKind).filter {
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

    override fun shouldRunAnalysis(module: TestModule): Boolean {
        return JsEnvironmentConfigurator.isMainModule(module, testServices)
    }
}

val RegisteredDirectives.moduleKind: ModuleKind
    get() = get(JsEnvironmentConfigurationDirectives.MODULE_KIND).singleOrNull()
        ?: if (contains(JsEnvironmentConfigurationDirectives.ES_MODULES)) ModuleKind.ES else ModuleKind.PLAIN

val TestModule.kind: ModuleKind
    get() = directives.moduleKind

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
    return plus(moduleKind.extension).minifyIfNeed()
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
