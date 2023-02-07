/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.converters

import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.common.phaser.toPhaseMap
import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureDescriptor
import org.jetbrains.kotlin.cli.common.isWindows
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.backend.js.*
import org.jetbrains.kotlin.ir.backend.js.codegen.JsGenerationGranularity
import org.jetbrains.kotlin.ir.backend.js.ic.JsExecutableProducer
import org.jetbrains.kotlin.ir.backend.web.lower.serialization.ir.JsManglerDesc
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.IrModuleToJsTransformer
import org.jetbrains.kotlin.ir.backend.js.SourceMapsInfo
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.CompilationOutputs
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.TranslationMode
import org.jetbrains.kotlin.ir.backend.web.IrModuleInfo
import org.jetbrains.kotlin.ir.backend.web.MainModule
import org.jetbrains.kotlin.ir.backend.web.getIrModuleInfoForKlib
import org.jetbrains.kotlin.ir.backend.web.sortDependencies
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImplForJsIC
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.irMessageLogger
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
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue
import org.jetbrains.kotlin.utils.fileUtils.withReplacedExtensionOrNull
import java.io.File

const val REGULAR_EXTENSION = ".js"
const val ESM_EXTENSION = ".mjs"

class JsIrBackendFacade(
    val testServices: TestServices,
    private val firstTimeCompilation: Boolean
) : AbstractTestFacade<BinaryArtifacts.KLib, BinaryArtifacts.Js>() {
    override val inputKind: ArtifactKinds.KLib
        get() = ArtifactKinds.KLib
    override val outputKind: ArtifactKinds.Js
        get() = ArtifactKinds.Js

    constructor(testServices: TestServices) : this(testServices, firstTimeCompilation = true)

    override fun transform(module: TestModule, inputArtifact: BinaryArtifacts.KLib): BinaryArtifacts.Js? {
        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)

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
        val es6Mode = JsEnvironmentConfigurationDirectives.ES6_MODE in module.directives

        val granularity = when {
            !firstTimeCompilation -> JsGenerationGranularity.WHOLE_PROGRAM
            splitPerModule || perModule -> JsGenerationGranularity.PER_MODULE
            splitPerFile -> JsGenerationGranularity.PER_FILE
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
                    jsExecutableProducer.buildExecutable(it.perModule, true).compilationOut
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
            irModuleFragment.apply { resolveTestPathes() },
            MainModule.Klib(inputArtifact.outputFile.absolutePath),
            configuration,
            dependencyModules.apply { forEach { it.resolveTestPathes() } },
            emptyMap(),
            irModuleFragment.irBuiltins,
            symbolTable,
            deserializer,
            phaseConfig,
            exportedDeclarations = setOf(FqName.fromSegments(listOfNotNull(testPackage, TEST_FUNCTION))),
            keep = keep,
            dceRuntimeDiagnostic = null,
            es6mode = es6Mode,
            safeExternalBoolean = JsEnvironmentConfigurationDirectives.SAFE_EXTERNAL_BOOLEAN in module.directives,
            safeExternalBooleanDiagnostic = module.directives[JsEnvironmentConfigurationDirectives.SAFE_EXTERNAL_BOOLEAN_DIAGNOSTIC].singleOrNull(),
            granularity = granularity,
        )

        return loweredIr2JsArtifact(module, loweredIr, granularity)
    }

    private fun loweredIr2JsArtifact(
        module: TestModule,
        loweredIr: LoweredIr,
        granularity: JsGenerationGranularity,
    ): BinaryArtifacts.Js {
        val mainArguments = JsEnvironmentConfigurator.getMainCallParametersForModule(module)
            .run { if (shouldBeGenerated()) arguments() else null }
        val runIrDce = JsEnvironmentConfigurationDirectives.RUN_IR_DCE in module.directives
        val onlyIrDce = JsEnvironmentConfigurationDirectives.ONLY_IR_DCE in module.directives
        val perModuleOnly = JsEnvironmentConfigurationDirectives.SPLIT_PER_MODULE in module.directives
        val isEsModules = JsEnvironmentConfigurationDirectives.ES_MODULES in module.directives ||
                module.directives[JsEnvironmentConfigurationDirectives.MODULE_KIND].contains(ModuleKind.ES)

        val outputFile =
            File(JsEnvironmentConfigurator.getJsModuleArtifactPath(testServices, module.name, TranslationMode.FULL_DEV) + module.kind.extension)

        val transformer = IrModuleToJsTransformer(
            loweredIr.context,
            mainArguments,
            moduleToName = JsIrModuleToPath(
                testServices,
                isEsModules && granularity != JsGenerationGranularity.WHOLE_PROGRAM
            )
        )
        // If runIrDce then include DCE results
        // If perModuleOnly then skip whole program
        // (it.dce => runIrDce) && (perModuleOnly => it.perModule)
        val translationModes = TranslationMode.values()
            .filter { (it.production || !onlyIrDce) && (!it.production || runIrDce) && (!perModuleOnly || it.perModule) }
            .filter { it.production == it.minimizedMemberNames }
            .toSet()
        val compilationOut = transformer.generateModule(loweredIr.allModules, translationModes, false)
        return BinaryArtifacts.Js.JsIrArtifact(outputFile, compilationOut).dump(module)
    }

    private fun IrModuleFragment.resolveTestPathes() {
        JsIrPathReplacer(testServices).let {
            files.forEach(it::lower)
        }
    }

    private fun loadIrFromKlib(module: TestModule, configuration: CompilerConfiguration): IrModuleInfo {
        val filesToLoad = module.files.takeIf { !firstTimeCompilation }?.map { "/${it.relativePath}" }?.toSet()

        val messageLogger = configuration.irMessageLogger
        val symbolTable = SymbolTable(IdSignatureDescriptor(JsManglerDesc), IrFactoryImplForJsIC(WholeWorldStageController()))

        val moduleDescriptor = testServices.moduleDescriptorProvider.getModuleDescriptor(module)
        val mainModuleLib = testServices.jsLibraryProvider.getCompiledLibraryByDescriptor(moduleDescriptor)
        val friendLibraries = JsEnvironmentConfigurator.getDependencies(module, testServices, DependencyRelation.FriendDependency)
            .map { testServices.jsLibraryProvider.getCompiledLibraryByDescriptor(it) }
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
        ) { if (it == mainModuleLib) moduleDescriptor else testServices.jsLibraryProvider.getDescriptorByCompiledLibrary(it) }
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
                    File(JsEnvironmentConfigurator.getJsModuleArtifactPath(testServices, module.name, mode) + moduleKind.extension)
                } else {
                    File(
                        JsEnvironmentConfigurator.getRecompiledJsModuleArtifactPath(
                            testServices,
                            module.name,
                            mode
                        ) + moduleKind.extension
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

    fun File.fixJsFile(newJsTarget: File, moduleId: String, moduleKind: ModuleKind) {
        val newJsCode = ClassicJsBackendFacade.wrapWithModuleEmulationMarkers(readText(), moduleKind, moduleId)

        val oldJsMap = File("$absolutePath.map")
        val jsCodeMap = (moduleKind == ModuleKind.PLAIN && oldJsMap.exists()).ifTrue { oldJsMap.readText() }

        this.delete()
        oldJsMap.delete()

        newJsTarget.write(newJsCode)
        jsCodeMap?.let { File("${newJsTarget.absolutePath}.map").write(it) }
    }

    private fun CompilationOutputs.writeTo(outputFile: File, moduleId: String, moduleKind: ModuleKind) {
        val allJsFiles = writeAll(outputFile.parentFile, outputFile.nameWithoutExtension, false, moduleId, moduleKind).filter {
            it.extension == "js"
        }

        val mainModuleFile = allJsFiles.last()
        mainModuleFile.fixJsFile(outputFile, moduleId, moduleKind)

        dependencies.map { it.first }.zip(allJsFiles.dropLast(1)).forEach { (depModuleId, builtJsFilePath) ->
            val newFile = outputFile.augmentWithModuleName(depModuleId)
            builtJsFilePath.fixJsFile(newFile, depModuleId, moduleKind)
        }
    }

    private fun File.write(text: String) {
        parentFile.mkdirs()
        writeText(text)
    }

    override fun shouldRunAnalysis(module: TestModule): Boolean {
        return JsEnvironmentConfigurator.isMainModule(module, testServices)
    }
}

val ModuleKind.extension: String
    get() = when (this) {
        ModuleKind.ES -> ESM_EXTENSION
        else -> REGULAR_EXTENSION
    }

val RegisteredDirectives.moduleKind: ModuleKind
    get() = get(JsEnvironmentConfigurationDirectives.MODULE_KIND).singleOrNull()
        ?: if (contains(JsEnvironmentConfigurationDirectives.ES_MODULES)) ModuleKind.ES else ModuleKind.PLAIN

val TestModule.kind: ModuleKind
    get() = directives.moduleKind

fun String.augmentWithModuleName(moduleName: String): String {
    val normalizedName = moduleName.run { if (isWindows) minify() else this }

    return if (normalizedName.isPath()) {
        replaceAfterLast(File.separator, normalizedName.replace("./", ""))
    } else {
        val suffix = when {
            endsWith(ESM_EXTENSION) -> ESM_EXTENSION
            endsWith(REGULAR_EXTENSION) -> REGULAR_EXTENSION
            else -> error("Unexpected file '$this' extension")
        }
        return removeSuffix("_v5$suffix") + "-${normalizedName}_v5$suffix"
    }
}

// D8 ignores Windows settings related to extending of maximum path symbols count
// The hack should be deleted when D8 fixes the bug.
// The issue is here: https://bugs.chromium.org/p/v8/issues/detail?id=13318
fun String.minify(): String {
    return replace("kotlin_org_jetbrains_kotlin_kotlin_", "")
        .replace("_minimal_for_test", "_min")
}

private fun String.isPath(): Boolean = contains("/")

fun File.augmentWithModuleName(moduleName: String): File = File(absolutePath.augmentWithModuleName(moduleName))
