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
import org.jetbrains.kotlin.backend.konan.objcexport.sx.SXClangModuleBuilder
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.konan.isNativeStdlib
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.konan.exec.Command
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.file.createTempFile
import org.jetbrains.kotlin.konan.file.use
import org.jetbrains.kotlin.konan.target.AppleConfigurables
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import java.util.*

internal class ObjCExport(val context: Context) {
    private val target get() = context.config.target

    private val resolver = object : CrossModuleResolver {
        override fun findModuleBuilder(declaration: DeclarationDescriptor): SXClangModuleBuilder =
                objcHeaderGenerators.first { declaration.module in it.moduleDescriptors }.moduleBuilder

        override fun findStdlibModuleBuilder(): SXClangModuleBuilder =
                objcHeaderGenerators.first { it.moduleDescriptors.find { it.isNativeStdlib() } != null }.moduleBuilder

        override fun findExportGenerator(moduleDescriptor: ModuleDescriptor): ObjCExportHeaderGenerator =
                objcHeaderGenerators.first { moduleDescriptor in it.moduleDescriptors }
    }

    private val objcHeaderGenerators: List<ObjCExportHeaderGenerator> = prepareObjCHeaderGenerators()

    private val exportedInterfaces = produceInterfaces()

    val namers: MutableList<ObjCExportNamer> = mutableListOf()
    private val codeSpecs: MutableMap<ObjCExportedInterface, ObjCExportCodeSpec> = mutableMapOf()

    fun buildCodeSpecs(symbolTable: SymbolTable) {
        exportedInterfaces.forEach {
            codeSpecs[it] = ObjCCodeSpecBuilder(it, symbolTable).build()
        }
    }

    private fun prepareObjCHeaderGenerators(): List<ObjCExportHeaderGenerator> {
        val unitSuspendFunctionExport = context.config.unitSuspendFunctionObjCExport
        val objcGenerics = context.configuration.getBoolean(KonanConfigKeys.OBJC_GENERICS)
        val mapper = ObjCExportMapper(context.frontendServices.deprecationResolver, unitSuspendFunctionExport = unitSuspendFunctionExport)

        val moduleDescriptors = listOf(context.moduleDescriptor) + context.getExportedDependencies()
        val stdlib = moduleDescriptors.first().allDependencyModules.first { it.isNativeStdlib() }
        val otherModules = (moduleDescriptors - stdlib).toSet()

        val stdlibNamer = ObjCExportNamerImpl(
                setOf(stdlib),
                stdlib.builtIns,
                mapper,
                "Kotlin",
                local = false,
                objcGenerics = objcGenerics,
        )

        val stdlibModuleBuilder = SXClangModuleBuilder(
                setOf(stdlib),
                headerPerModule = false,
                "Kotlin.h",
                containsStdlib = true,
                { TODO() }
        )

        val stdlibHeaderGenerator = ObjCExportHeaderGeneratorImpl(
                context, listOf(stdlib), mapper, stdlibNamer, objcGenerics, "Kotlin", stdlibModuleBuilder, this.resolver
        )

        return otherModules.map {
            val baseName = inferBaseName(it)
            val namer = ObjCExportNamerImpl(
                    setOf(it),
                    context.moduleDescriptor.builtIns,
                    mapper,
                    baseName,
                    local = false,
                    objcGenerics = objcGenerics
            )
            val theModuleBuilder = SXClangModuleBuilder(
                    setOf(it),
                    headerPerModule = false,
                    "${baseName}.h",
                    containsStdlib = false,
                    { stdlibModuleBuilder.findHeaderForStdlib() }
            )
            ObjCExportHeaderGeneratorImpl(
                    context, listOf(it), mapper, namer, objcGenerics, baseName, theModuleBuilder, this.resolver
            )
        } + stdlibHeaderGenerator
    }

    private fun inferBaseName(module: ModuleDescriptor): String {
        // TODO: Better normalization
        val normalized = module.name.asStringStripSpecialMarkers()
                .replace('.', '_')
                .replace(':', '_')
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        return normalized
    }

    private fun produceInterfaces(): List<ObjCExportedInterface> {
        if (!target.family.isAppleFamily) return emptyList()

        // TODO: emit RTTI to the same modules as classes belong to.
        //   Not possible yet, since ObjCExport translates the entire "world" API at once
        //   and can't do this per-module, e.g. due to global name conflict resolution.

        val produceFramework = context.config.produce == CompilerOutputKind.FRAMEWORK

        return if (produceFramework) {
            objcHeaderGenerators.forEach { builder ->
                builder.translateModule()
            }
            // We have to split these two loops because module translation might affect API of each module.
            objcHeaderGenerators.map { builder ->
                builder.buildInterface()
            }
        } else {
            emptyList()
        }
    }

    internal fun generate(codegen: CodeGenerator) {
        if (!target.family.isAppleFamily) return

        if (context.shouldDefineFunctionClasses) {
            ObjCExportBlockCodeGenerator(codegen).use { it.generate() }
        }

        if (!context.config.isFinalBinary) return // TODO: emit RTTI to the same modules as classes belong to.

        codeSpecs.forEach { (exportedInterface, codeSpec) ->
            namers += exportedInterface.namer
            ObjCExportCodeGenerator(codegen, exportedInterface.namer, exportedInterface.mapper).use { objCCodeGenerator ->
                exportedInterface.generateWorkaroundForSwiftSR10177()
                objCCodeGenerator.generate(codeSpec)
            }
        }
    }

    /**
     * Populate framework directory with headers, module and info.plist.
     */
    fun produceFrameworkInterface() {
        exportedInterfaces.forEach { exportedInterface ->
            val dir = File(context.config.outputFile).parentFile
            val name = if (!exportedInterface.frameworkName.endsWith(".framework")) {
                exportedInterface.frameworkName + ".framework"
            } else {
                exportedInterface.frameworkName
            }
            val frameworkDirectory = dir.child(name)
            val properties = context.config.platform.configurables as AppleConfigurables
            val mainPackageGuesser = MainPackageGuesser(
                    context.moduleDescriptor,
                    context.getIncludedLibraryDescriptors(),
                    context.getExportedDependencies()
            )
            val infoPListBuilder = InfoPListBuilder(target, properties, context.configuration, mainPackageGuesser)
            val moduleMapBuilder = ModuleMapBuilder(exportedInterface.frameworkName)
            FrameworkBuilder(
                    exportedInterface.clangModule,
                    target,
                    frameworkDirectory,
                    exportedInterface.frameworkName,
                    context.shouldExportKDoc()
            ).build(infoPListBuilder, moduleMapBuilder)
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
}
