/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.descriptors.isInterface
import org.jetbrains.kotlin.backend.konan.llvm.CodeGenerator
import org.jetbrains.kotlin.backend.konan.llvm.objcexport.ObjCExportBlockCodeGenerator
import org.jetbrains.kotlin.backend.konan.llvm.objcexport.ObjCExportCodeGenerator
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
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

// TODO: Replace Context with a more lightweight class.
internal fun produceObjCExportInterface(context: Context): ObjCExportedInterface {
    require(context.config.target.family.isAppleFamily)
    require(context.config.produce == CompilerOutputKind.FRAMEWORK)

    val topLevelNamePrefix = context.objCExportTopLevelNamePrefix

    // TODO: emit RTTI to the same modules as classes belong to.
    //   Not possible yet, since ObjCExport translates the entire "world" API at once
    //   and can't do this per-module, e.g. due to global name conflict resolution.

    val unitSuspendFunctionExport = context.config.unitSuspendFunctionObjCExport
    val mapper = ObjCExportMapper(context.frontendServices.deprecationResolver, unitSuspendFunctionExport = unitSuspendFunctionExport)
    val moduleDescriptors = listOf(context.moduleDescriptor) + context.getExportedDependencies()
    val objcGenerics = context.configuration.getBoolean(KonanConfigKeys.OBJC_GENERICS)
    val namer = ObjCExportNamerImpl(
            moduleDescriptors.toSet(),
            context.moduleDescriptor.builtIns,
            mapper,
            topLevelNamePrefix,
            local = false,
            objcGenerics = objcGenerics
    )
    val headerGenerator = ObjCExportHeaderGeneratorImpl(context, moduleDescriptors, mapper, namer, objcGenerics)
    headerGenerator.translateModule()
    return headerGenerator.buildInterface()
}

internal class ObjCExport(
        val context: Context,
        private val exportedInterface: ObjCExportedInterface?,
        private val codeSpec: ObjCExportCodeSpec?
) {
    private val target get() = context.config.target
    private val topLevelNamePrefix get() = context.objCExportTopLevelNamePrefix

    lateinit var namer: ObjCExportNamer

    internal fun generate(codegen: CodeGenerator) {
        if (!target.family.isAppleFamily) return

        if (context.shouldDefineFunctionClasses) {
            ObjCExportBlockCodeGenerator(codegen).generate()
        }

        if (!context.config.isFinalBinary) return // TODO: emit RTTI to the same modules as classes belong to.

        val mapper = exportedInterface?.mapper ?: ObjCExportMapper(unitSuspendFunctionExport = context.config.unitSuspendFunctionObjCExport)
        namer = exportedInterface?.namer ?: ObjCExportNamerImpl(
                setOf(codegen.context.moduleDescriptor),
                context.moduleDescriptor.builtIns,
                mapper,
                topLevelNamePrefix,
                local = false
        )

        val objCCodeGenerator = ObjCExportCodeGenerator(codegen, namer, mapper)

        exportedInterface?.generateWorkaroundForSwiftSR10177()

        objCCodeGenerator.generate(codeSpec)
        objCCodeGenerator.dispose()
    }

    /**
     * Populate framework directory with headers, module and info.plist.
     */
    fun produceFrameworkInterface() {
        if (exportedInterface != null) {
            produceFrameworkSpecific(exportedInterface.headerLines)
        }
    }

    private fun produceFrameworkSpecific(headerLines: List<String>) {
        val frameworkDirectory = File(context.generationState.outputFile)
        val frameworkName = frameworkDirectory.name.removeSuffix(".framework")
        val frameworkBuilder = FrameworkBuilder(
                context.config,
                infoPListBuilder = InfoPListBuilder(context.config),
                moduleMapBuilder = ModuleMapBuilder(),
                objCHeaderWriter = ObjCHeaderWriter(),
                mainPackageGuesser = MainPackageGuesser(),
        )
        frameworkBuilder.build(
                context.moduleDescriptor,
                frameworkDirectory,
                frameworkName,
                headerLines,
                moduleDependencies = emptySet()
        )
    }

    // See https://bugs.swift.org/browse/SR-10177
    private fun ObjCExportedInterface.generateWorkaroundForSwiftSR10177() {
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

        val clangCommand = context.config.clang.clangC(
                source.absolutePath,
                "-O2",
                "-emit-llvm",
                "-c", "-o", bitcode.absolutePath
        )

        val result = Command(clangCommand).getResult(withErrors = true)

        if (result.exitCode == 0) {
            context.generationState.llvm.additionalProducedBitcodeFiles += bitcode.absolutePath
        } else {
            // Note: ignoring compile errors intentionally.
            // In this case resulting framework will likely be unusable due to compile errors when importing it.
        }
    }
}
