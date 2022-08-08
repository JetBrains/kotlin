/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.descriptors.isInterface
import org.jetbrains.kotlin.backend.konan.getExportedDependencies
import org.jetbrains.kotlin.backend.konan.llvm.CodeGenerator
import org.jetbrains.kotlin.backend.konan.llvm.objcexport.ObjCExportBlockCodeGenerator
import org.jetbrains.kotlin.backend.konan.llvm.objcexport.ObjCExportCodeGenerator
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.SourceFile
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.konan.exec.Command
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.file.createTempFile
import org.jetbrains.kotlin.konan.file.use
import org.jetbrains.kotlin.konan.target.*

internal class ObjCExportedInterface(
        val generatedClasses: Set<ClassDescriptor>,
        val categoryMembers: Map<ClassDescriptor, List<CallableMemberDescriptor>>,
        val topLevel: Map<SourceFile, List<CallableMemberDescriptor>>,
        val namer: ObjCExportNamer,
        val mapper: ObjCExportMapper,
        val clangModule: SXClangModule
)

internal class ObjCExport(val context: Context, symbolTable: SymbolTable) {
    private val target get() = context.config.target
    private val topLevelNamePrefix get() = context.objCExportTopLevelNamePrefix

    private val exportedInterface = produceInterface()

    private val codeSpec = exportedInterface?.let {
        ObjCCodeSpecBuilder(it, symbolTable).build()
    }

    private fun produceInterface(): ObjCExportedInterface? {
        if (!target.family.isAppleFamily) return null

        // TODO: emit RTTI to the same modules as classes belong to.
        //   Not possible yet, since ObjCExport translates the entire "world" API at once
        //   and can't do this per-module, e.g. due to global name conflict resolution.

        val produceFramework = context.config.produce == CompilerOutputKind.FRAMEWORK

        return if (produceFramework) {
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
            val headerGenerator = ObjCExportHeaderGeneratorImpl(
                    context, moduleDescriptors, mapper, namer, objcGenerics, getFrameworkName()
            )
            headerGenerator.translateModule()
            headerGenerator.buildInterface()
        } else {
            null
        }
    }

    lateinit var namer: ObjCExportNamer

    internal fun generate(codegen: CodeGenerator) {
        if (!target.family.isAppleFamily) return

        if (context.shouldDefineFunctionClasses) {
            ObjCExportBlockCodeGenerator(codegen).use { it.generate() }
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

        ObjCExportCodeGenerator(codegen, namer, mapper).use { objCCodeGenerator ->
            exportedInterface?.generateWorkaroundForSwiftSR10177()
            objCCodeGenerator.generate(codeSpec)
        }
    }

    /**
     * Populate framework directory with headers, module and info.plist.
     */
    fun produceFrameworkInterface() {
        if (exportedInterface != null) {
            val framework = File(context.config.outputFile)
            val properties = context.config.platform.configurables as AppleConfigurables
            val mainPackageGuesser = MainPackageGuesser(
                    context.moduleDescriptor, context.getIncludedLibraryDescriptors(), context.getExportedDependencies()
            )
            val infoPListBuilder = InfoPListBuilder(target, properties, context.configuration, mainPackageGuesser)
            val moduleMapBuilder = ModuleMapBuilder(getFrameworkName())
            FrameworkBuilder(exportedInterface.clangModule, target, framework, getFrameworkName(), context.shouldExportKDoc())
                    .build(infoPListBuilder, moduleMapBuilder)
        }
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
                    generatedClasses.filter { it.isInterface }.forEach {
                        val protocolName = namer.getClassOrProtocolName(it).objCName
                        append("@protocol($protocolName); ")
                    }
                },
                "}"
        )


        val source = createTempFile("protocols", ".m").deleteOnExit()
        source.writeLines(/*headerLines +*/ protocolsStub)

        val bitcode = createTempFile("protocols", ".bc").deleteOnExit()

        val clangCommand = context.config.clang.clangC(
                source.absolutePath,
                "-O2",
                "-emit-llvm",
                "-c", "-o", bitcode.absolutePath
        )

        val result = Command(clangCommand).getResult(withErrors = true)

        if (result.exitCode == 0) {
            context.llvm.additionalProducedBitcodeFiles += bitcode.absolutePath
        } else {
            // Note: ignoring compile errors intentionally.
            // In this case resulting framework will likely be unusable due to compile errors when importing it.
        }
    }

    private fun getFrameworkName(): String {
        val framework = File(context.config.outputFile)
        return framework.name.removeSuffix(".framework")
    }
}
