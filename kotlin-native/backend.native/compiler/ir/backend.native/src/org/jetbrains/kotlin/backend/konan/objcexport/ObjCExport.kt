/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.descriptors.isInterface
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.backend.konan.ir.konanLibrary
import org.jetbrains.kotlin.backend.konan.llvm.CodeGenerator
import org.jetbrains.kotlin.backend.konan.llvm.objcexport.ObjCExportBlockCodeGenerator
import org.jetbrains.kotlin.backend.konan.llvm.objcexport.ObjCExportCodeGenerator
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.konan.exec.Command
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.file.createTempFile
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.library.metadata.resolver.TopologicalLibraryOrder
import org.jetbrains.kotlin.resolve.descriptorUtil.module

internal class ObjCExportedInterface(
        val generatedClasses: Set<ClassDescriptor>,
        val categoryMembers: Map<ClassDescriptor, List<CallableMemberDescriptor>>,
        val topLevel: Map<SourceFile, List<CallableMemberDescriptor>>,
        val headerLines: List<String>,
        val namer: ObjCExportNamer,
        val mapper: ObjCExportMapper
)

class FrameworkNaming(val topLevelPrefix: String, val moduleName: String, val headerName: String)

sealed class ModuleTranslationConfig(
        val module: ModuleDescriptor,
        val frameworkNaming: FrameworkNaming
) {
    class Full(moduleDescriptor: ModuleDescriptor, frameworkNaming: FrameworkNaming): ModuleTranslationConfig(moduleDescriptor, frameworkNaming)

    class Partial(moduleDescriptor: ModuleDescriptor, frameworkNaming: FrameworkNaming) : ModuleTranslationConfig(moduleDescriptor, frameworkNaming) {
        private val referencedDeclarations = mutableSetOf<DeclarationDescriptor>()
        fun reference(declaration: DeclarationDescriptor): HeaderDependency {
            assert(declaration.module == module)
            referencedDeclarations += declaration
            return HeaderDependency(frameworkNaming.moduleName, frameworkNaming.headerName, module)
        }

        fun referencedDeclarations(): List<DeclarationDescriptor> {
            return referencedDeclarations.toList()
        }
    }
}

internal class ObjCExportSharedState(
        exportedLibraries: List<ModuleDescriptor>,
        private val moduleNamer: (ModuleDescriptor) -> String = { it.name.asStringStripSpecialMarkers() }
) {
    private val modulesToTranslate: MutableMap<ModuleDescriptor, ModuleTranslationConfig> by lazy {
        exportedLibraries.associateWith { ModuleTranslationConfig.Full(it, createHeaders(it)) }.toMutableMap()
    }

    fun addDependency(descriptor: DeclarationDescriptor): HeaderDependency? {
        val module = descriptor.module
        val translationConfig = modulesToTranslate.getOrPut(module) { ModuleTranslationConfig.Partial(module, createHeaders(module)) }
        return when (translationConfig) {
            // Nothing to do, everything is exported anyway
            is ModuleTranslationConfig.Full -> null
            is ModuleTranslationConfig.Partial -> {
                translationConfig.reference(descriptor)
            }
        }
    }

    fun yieldAll(config: KonanConfig): Sequence<ModuleTranslationConfig> = sequence {
        val libraries = sortedLibraries(config).reversed()
        libraries.forEach { library ->
            val translationConfig = modulesToTranslate.values
                    .firstOrNull { it.module.konanLibrary == library }
                    ?: return@forEach
            yield(translationConfig)
        }
    }

    private fun sortedLibraries(config: KonanConfig) = config.resolvedLibraries
            .filterRoots { (!it.isDefault && !config.purgeUserLibs) || it.isNeededForLink }
            .getFullList(TopologicalLibraryOrder)
            .map { it as KonanLibrary }

    private fun createHeaders(moduleDescriptor: ModuleDescriptor): FrameworkNaming {
        val name = moduleNamer(moduleDescriptor)
        return FrameworkNaming(name, name, "$name.h")
    }
}

internal fun produceObjCExportInterface(
        context: PhaseContext,
        moduleTranslationConfig: ModuleTranslationConfig,
        frontendServices: FrontendServices,
        sharedState: ObjCExportSharedState,
): ObjCExportedInterface {
    val config = context.config
    require(config.target.family.isAppleFamily)
    require(config.produce == CompilerOutputKind.FRAMEWORK)

    val topLevelNamePrefix: String = moduleTranslationConfig.frameworkNaming.topLevelPrefix

    // TODO: emit RTTI to the same modules as classes belong to.
    //   Not possible yet, since ObjCExport translates the entire "world" API at once
    //   and can't do this per-module, e.g. due to global name conflict resolution.

    val unitSuspendFunctionExport = config.unitSuspendFunctionObjCExport
    val mapper = ObjCExportMapper(frontendServices.deprecationResolver, unitSuspendFunctionExport = unitSuspendFunctionExport)
    val moduleDescriptors = listOf(moduleTranslationConfig.module)
    val objcGenerics = config.configuration.getBoolean(KonanConfigKeys.OBJC_GENERICS)
    val namer = ObjCExportNamerImpl(
            moduleDescriptors.toSet(),
            moduleTranslationConfig.module.builtIns,
            mapper,
            topLevelNamePrefix,
            local = false,
            objcGenerics = objcGenerics
    )
    val headerGenerator = ObjCExportHeaderGeneratorImpl(context, moduleTranslationConfig, moduleDescriptors, mapper, namer, objcGenerics, sharedState)
    headerGenerator.translateModule()
    return headerGenerator.buildInterface()
}

/**
 * Populate framework directory with headers, module and info.plist.
 */
internal fun createObjCFramework(
        config: KonanConfig,
        moduleDescriptor: ModuleDescriptor,
        exportedInterface: ObjCExportedInterface,
        frameworkDirectory: File
) {
    val frameworkName = frameworkDirectory.name.removeSuffix(".framework")
    val frameworkBuilder = FrameworkBuilder(
            config,
            infoPListBuilder = InfoPListBuilder(config),
            moduleMapBuilder = ModuleMapBuilder(),
            objCHeaderWriter = ObjCHeaderWriter(),
            mainPackageGuesser = MainPackageGuesser(),
    )
    frameworkBuilder.build(
            moduleDescriptor,
            frameworkDirectory,
            frameworkName,
            exportedInterface.headerLines,
            moduleDependencies = emptySet()
    )
}

// TODO: No need for such class in dynamic driver.
internal class ObjCExport(
        private val generationState: NativeGenerationState,
        private val moduleDescriptor: ModuleDescriptor,
        private val exportedInterface: ObjCExportedInterface?,
        private val codeSpec: ObjCExportCodeSpec?
) {
    private val config = generationState.config
    private val target get() = config.target
    private val topLevelNamePrefix get() = generationState.objCExportTopLevelNamePrefix

    lateinit var namer: ObjCExportNamer

    internal fun generate(codegen: CodeGenerator) {
        if (!target.family.isAppleFamily) return

        if (generationState.shouldDefineFunctionClasses) {
            ObjCExportBlockCodeGenerator(codegen).generate()
        }

        if (!config.isFinalBinary) return // TODO: emit RTTI to the same modules as classes belong to.

        val mapper = exportedInterface?.mapper ?: ObjCExportMapper(unitSuspendFunctionExport = config.unitSuspendFunctionObjCExport)
        namer = exportedInterface?.namer ?: ObjCExportNamerImpl(
                setOf(moduleDescriptor),
                moduleDescriptor.builtIns,
                mapper,
                topLevelNamePrefix,
                local = false
        )

        val objCCodeGenerator = ObjCExportCodeGenerator(codegen, namer, mapper)

        exportedInterface?.generateWorkaroundForSwiftSR10177(generationState)

        objCCodeGenerator.generate(codeSpec)
        objCCodeGenerator.dispose()
    }

    /**
     * Populate framework directory with headers, module and info.plist.
     */
    fun produceFrameworkInterface() {
        if (exportedInterface != null) {
            createObjCFramework(generationState.config, moduleDescriptor, exportedInterface, File(generationState.outputFile))
        }
    }
}

// See https://bugs.swift.org/browse/SR-10177
private fun ObjCExportedInterface.generateWorkaroundForSwiftSR10177(generationState: NativeGenerationState) {
    // Code for all protocols from the header should get into the binary.
    // Objective-C protocols ABI is complicated (consider e.g. undocumented extended type encoding),
    // so the easiest way to achieve this (quickly) is to compile a stub by clang.

    val protocolsStub = listOf(
            "__attribute__((used)) static void __workaroundSwiftSR10177() {",
            buildString {
                append("    ")
                generatedClasses.forEach {
                    if (it.isInterface) {
                        val protocolName = namer.getClassOrProtocolName(it).objCName
                        append("@protocol($protocolName); ")
                    }
                }
            },
            "}"
    )

    val source = createTempFile("protocols", ".m").deleteOnExit()
    source.writeLines(headerLines + protocolsStub)

    val bitcode = createTempFile("protocols", ".bc").deleteOnExit()

    val clangCommand = generationState.config.clang.clangC(
            source.absolutePath,
            "-O2",
            "-emit-llvm",
            "-c", "-o", bitcode.absolutePath
    )

    val result = Command(clangCommand).getResult(withErrors = true)

    if (result.exitCode == 0) {
        generationState.llvm.additionalProducedBitcodeFiles += bitcode.absolutePath
    } else {
        // Note: ignoring compile errors intentionally.
        // In this case resulting framework will likely be unusable due to compile errors when importing it.
    }
}
