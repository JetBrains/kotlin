/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.descriptors.isInterface
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.backend.konan.llvm.CodeGenerator
import org.jetbrains.kotlin.backend.konan.llvm.objcexport.ObjCExportBlockCodeGenerator
import org.jetbrains.kotlin.backend.konan.llvm.objcexport.ObjCExportCodeGenerator
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.SourceFile
import org.jetbrains.kotlin.konan.exec.Command
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.file.createTempFile
import org.jetbrains.kotlin.konan.target.CompilerOutputKind

internal class ObjCExportedInterface(
        val generatedClasses: Set<ClassDescriptor>,
        val categoryMembers: Map<ClassDescriptor, List<CallableMemberDescriptor>>,
        val topLevel: Map<SourceFile, List<CallableMemberDescriptor>>,
        val headerLines: List<String>,
        val namer: ObjCExportNamer,
        val mapper: ObjCExportMapper
)

internal fun produceObjCExportInterface(
        context: PhaseContext,
        moduleDescriptor: ModuleDescriptor,
        frontendServices: FrontendServices,
): ObjCExportedInterface {
    val config = context.config
    require(config.target.family.isAppleFamily)
    require(config.produce == CompilerOutputKind.FRAMEWORK)

    val topLevelNamePrefix = context.objCExportTopLevelNamePrefix

    // TODO: emit RTTI to the same modules as classes belong to.
    //   Not possible yet, since ObjCExport translates the entire "world" API at once
    //   and can't do this per-module, e.g. due to global name conflict resolution.

    val unitSuspendFunctionExport = config.unitSuspendFunctionObjCExport
    val mapper = ObjCExportMapper(frontendServices.deprecationResolver, unitSuspendFunctionExport = unitSuspendFunctionExport)
    val moduleDescriptors = listOf(moduleDescriptor) + moduleDescriptor.getExportedDependencies(config)
    val objcGenerics = config.configuration.getBoolean(KonanConfigKeys.OBJC_GENERICS)
    val disableSwiftMemberNameMangling = config.configuration.getBoolean(BinaryOptions.objcExportDisableSwiftMemberNameMangling)
    val ignoreInterfaceMethodCollisions = config.configuration.getBoolean(BinaryOptions.objcExportIgnoreInterfaceMethodCollisions)
    val namer = ObjCExportNamerImpl(
            moduleDescriptors.toSet(),
            moduleDescriptor.builtIns,
            mapper,
            topLevelNamePrefix,
            local = false,
            objcGenerics = objcGenerics,
            disableSwiftMemberNameMangling = disableSwiftMemberNameMangling,
            ignoreInterfaceMethodCollisions = ignoreInterfaceMethodCollisions,
    )
    val headerGenerator = ObjCExportHeaderGeneratorImpl(context, moduleDescriptors, mapper, namer, objcGenerics)
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
    val frameworkName = frameworkDirectory.name.removeSuffix(CompilerOutputKind.FRAMEWORK.suffix())
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
            moduleDependencies = setOf("Foundation")
    )
}

internal fun createTestBundle(
        config: KonanConfig,
        moduleDescriptor: ModuleDescriptor,
        bundleDirectory: File
) {
    val name = bundleDirectory.name.removeSuffix(CompilerOutputKind.TEST_BUNDLE.suffix())
    BundleBuilder(config, infoPListBuilder = InfoPListBuilder(config), mainPackageGuesser = MainPackageGuesser())
            .build(moduleDescriptor, bundleDirectory, name)
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
