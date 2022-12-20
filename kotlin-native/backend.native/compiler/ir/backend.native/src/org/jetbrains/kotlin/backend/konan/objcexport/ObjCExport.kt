/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.backend.konan.KonanConfig
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.backend.konan.descriptors.isInterface
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.backend.konan.ir.konanLibrary
import org.jetbrains.kotlin.backend.konan.isFinalBinary
import org.jetbrains.kotlin.backend.konan.llvm.CodeGenerator
import org.jetbrains.kotlin.backend.konan.llvm.objcexport.ObjCExportBlockCodeGenerator
import org.jetbrains.kotlin.backend.konan.llvm.objcexport.ObjCExportCodeGenerator
import org.jetbrains.kotlin.backend.konan.shouldDefineFunctionClasses
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.konan.exec.Command
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.file.createTempFile
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.library.metadata.resolver.TopologicalLibraryOrder
import org.jetbrains.kotlin.resolve.descriptorUtil.module

internal class ObjCExportedInterface(
        val generatedClasses: Set<ClassDescriptor>,
        val categoryMembers: Map<ClassDescriptor, List<CallableMemberDescriptor>>,
        val topLevel: Map<SourceFile, List<CallableMemberDescriptor>>,
        val headerLines: List<String>,
        val namer: ModuleObjCExportNamer,
        val mapper: ObjCExportMapper
)

class FrameworkNaming(val topLevelPrefix: String, val moduleName: String, val headerName: String)

internal sealed class ModuleTranslationConfig(
        val module: ModuleDescriptor,
        val frameworkNaming: FrameworkNaming,
        val mapper: ObjCExportMapper,
        val objcGenerics: Boolean,
) {
    class Full(
            moduleDescriptor: ModuleDescriptor,
            frameworkNaming: FrameworkNaming,
            mapper: ObjCExportMapper,
            objcGenerics: Boolean,
    ): ModuleTranslationConfig(moduleDescriptor, frameworkNaming, mapper, objcGenerics)

    class Partial(
            moduleDescriptor: ModuleDescriptor,
            frameworkNaming: FrameworkNaming,
            mapper: ObjCExportMapper,
            objcGenerics: Boolean,
    ) : ModuleTranslationConfig(moduleDescriptor, frameworkNaming, mapper, objcGenerics) {
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

    val namer: ModuleObjCExportNamer by lazy {
        ObjCExportNamerImpl(
                moduleDescriptors = setOf(module),
                builtIns = module.builtIns,
                mapper = mapper,
                topLevelNamePrefix = frameworkNaming.topLevelPrefix,
                stdlibNamePrefix = "stdlib",
                local = false,
                objcGenerics = objcGenerics,
        )
    }
}

internal class ObjCExportSharedState(
        exportedLibraries: List<ModuleDescriptor>,
        private val stdlibModule: ModuleDescriptor,
        private val moduleNamer: (ModuleDescriptor) -> String = { it.name.asStringStripSpecialMarkers() },
        private val createMapper: (ModuleDescriptor) -> ObjCExportMapper,
        private val objcGenerics: Boolean,
) {
    private val modulesToTranslate: MutableMap<ModuleDescriptor, ModuleTranslationConfig> by lazy {
        exportedLibraries.associateWith {
            ModuleTranslationConfig.Full(it, createHeaders(it), createMapper(it), objcGenerics)
        }.toMutableMap()
    }

    fun addDependency(descriptor: DeclarationDescriptor): HeaderDependency? {
        val module = descriptor.module
        val translationConfig = getTranslationConfigOrCreate(module)
        return when (translationConfig) {
            // Nothing to do, everything is exported anyway
            is ModuleTranslationConfig.Full -> null
            is ModuleTranslationConfig.Partial -> {
                translationConfig.reference(descriptor)
            }
        }
    }

    fun addStdlibDependency(): HeaderDependency {
        val stdlibTranslationConfig = getTranslationConfigOrCreate(stdlibModule)
        return HeaderDependency(
                stdlibTranslationConfig.frameworkNaming.moduleName,
                stdlibTranslationConfig.frameworkNaming.headerName,
                stdlibModule
        )
    }

    fun findExportNamerFor(module: ModuleDescriptor): ObjCExportNamer {
        val translationConfig = getTranslationConfigOrCreate(module)
        return translationConfig.namer
    }

    fun findExportNamerForStdlib(): ObjCExportNamer =
            findExportNamerFor(stdlibModule)

    val globalNamer: ObjCExportNamer by lazy {
        SharedObjCExportNamer(this)
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

    private fun getTranslationConfigOrCreate(module: ModuleDescriptor): ModuleTranslationConfig =
            modulesToTranslate.getOrPut(module) {
                ModuleTranslationConfig.Partial(module, createHeaders(module), createMapper(module), objcGenerics)
            }
}

internal fun produceObjCExportInterface(
        context: PhaseContext,
        moduleTranslationConfig: ModuleTranslationConfig,
        sharedState: ObjCExportSharedState,
): ObjCExportedInterface {
    // TODO: emit RTTI to the same modules as classes belong to.
    //   Not possible yet, since ObjCExport translates the entire "world" API at once
    //   and can't do this per-module, e.g. due to global name conflict resolution.

    val mapper = moduleTranslationConfig.mapper
    val moduleDescriptors = listOf(moduleTranslationConfig.module)
    val objcGenerics = moduleTranslationConfig.objcGenerics
    val namer = sharedState.globalNamer
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
                stdlibNamePrefix = "stdlib",
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
