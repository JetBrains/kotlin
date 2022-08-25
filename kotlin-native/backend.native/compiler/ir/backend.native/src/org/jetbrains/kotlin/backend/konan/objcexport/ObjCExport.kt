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
import org.jetbrains.kotlin.backend.konan.objcexport.sx.*
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageUtil
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithSource
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.konan.isNativeStdlib
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.konan.exec.Command
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.file.createTempFile
import org.jetbrains.kotlin.konan.file.use
import org.jetbrains.kotlin.konan.target.AppleConfigurables
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.source.getPsi
import java.util.*

private class SimpleEventQueue : EventQueue {

    private val uniqueEvents: MutableSet<Event> = mutableSetOf()
    private val queue: MutableList<Event> = mutableListOf()

    override fun add(event: Event) {
        if (event in uniqueEvents) return
        queue += event
        uniqueEvents += event
    }

    /**
     * Consume all events from the queue until it is empty
     */
    fun drain(eventProcessors: Collection<EventProcessor>) {
        // finalization of event processor might generate more events
        eventProcessors.forEach { it.begin() }
        while (queue.isNotEmpty()) {
            while (queue.isNotEmpty()) {
                val event = queue.removeFirst()
                eventProcessors.forEach { it.process(event) }
            }
            eventProcessors.forEach { it.finalize() }
        }
    }
}

internal class ObjCExport(val context: Context) {

    private val unitSuspendFunctionExport = context.config.unitSuspendFunctionObjCExport
    private val objcGenerics = context.configuration.getBoolean(KonanConfigKeys.OBJC_GENERICS)

    private val mapper = ObjCExportMapper(context.frontendServices.deprecationResolver, unitSuspendFunctionExport = unitSuspendFunctionExport)

    private val target get() = context.config.target

    private val resolver = object : CrossModuleResolver {
        override fun findModuleBuilder(declaration: DeclarationDescriptor): SXClangModuleBuilder =
                translationsConfigurations.first { it.modules.contains(declaration.module) }.moduleBuilder

        override fun findNamer(declaration: DeclarationDescriptor): ObjCExportNamer =
                translationsConfigurations.first { it.modules.contains(declaration.module) }.namer
    }

    data class TranslationConfiguration(
            val modules: Set<ModuleDescriptor>,
            val namer: ObjCExportNamer,
            val mapper: ObjCExportMapper,
            val moduleBuilder: SXClangModuleBuilder,
            val frameworkName: String,
            val createProducer: (ObjCExportProblemCollector) -> ObjCExportModuleTranslator
    )

    private val translationsConfigurations: List<TranslationConfiguration> by lazy { prepareTranslationConfigurations() }

    private val objcModuleIndexers: List<ObjCExportModulesIndexer> by lazy { prepareObjCHeaderIndexers() }

    private val exportedInterfaces by lazy { produceInterfaces() }

    private val eventQueue: SimpleEventQueue = SimpleEventQueue()

    lateinit var mainNamer: ObjCExportNamer
    private val codeSpecs: MutableMap<ObjCExportedInterface, ObjCExportCodeSpec> = mutableMapOf()

    fun buildCodeSpecs(symbolTable: SymbolTable) {
        exportedInterfaces.forEach {
            codeSpecs[it] = ObjCCodeSpecBuilder(it, symbolTable).build()
        }
    }

    private fun prepareTranslationConfigurations(): List<TranslationConfiguration> {
        val translationsConfigurations: MutableList<TranslationConfiguration> = mutableListOf()
        val moduleDescriptors: List<ModuleDescriptor> = listOf(context.moduleDescriptor) + context.getExportedDependencies()
        val stdlib: ModuleDescriptor = moduleDescriptors.first().allDependencyModules.first { it.isNativeStdlib() }
        val otherModules = (moduleDescriptors - stdlib).toSet()
        val coreFrameworkPrefix = "KotlinCore"
        val stdlibNamer = ObjCExportNamerImpl(
                setOf(stdlib),
                stdlib.builtIns,
                mapper,
                coreFrameworkPrefix,
                local = false,
                objcGenerics = objcGenerics,
        )
        val stdlibModuleBuilder = StdlibClangModuleBuilder(
                stdlib,
                coreFrameworkPrefix,
        )
        translationsConfigurations += TranslationConfiguration(
                setOf(stdlib),
                stdlibNamer,
                mapper,
                stdlibModuleBuilder,
                coreFrameworkPrefix, { problemCollector ->
            ObjCExportStdlibTranslator(
                    stdlibModuleBuilder,
                    objcGenerics,
                    problemCollector,
                    mapper,
                    stdlibNamer,
                    resolver,
                    eventQueue,
                    coreFrameworkPrefix
            )
        })

        otherModules.map {
            val baseName = inferBaseName(it)
            val namer = ObjCExportNamerImpl(
                    setOf(it),
                    context.moduleDescriptor.builtIns,
                    mapper,
                    baseName,
                    local = false,
                    objcGenerics = objcGenerics
            )
            val theModuleBuilder = SimpleClangModuleBuilder(
                    setOf(it),
                    baseName,
                    { stdlibModuleBuilder }
            )

            translationsConfigurations += TranslationConfiguration(
                    setOf(it),
                    namer,
                    mapper,
                    theModuleBuilder,
                    baseName,
                    { problemCollector ->
                        ObjCExportModuleTranslator(
                                theModuleBuilder,
                                objcGenerics,
                                problemCollector,
                                mapper,
                                namer,
                                resolver,
                                eventQueue,
                                baseName,
                        )
                    })
        }
        return translationsConfigurations
    }

    private fun prepareObjCHeaderIndexers(): List<ObjCExportModulesIndexer> {
        val moduleDescriptors: List<ModuleDescriptor> = listOf(context.moduleDescriptor) + context.getExportedDependencies()
        val stdlib: ModuleDescriptor = moduleDescriptors.first().allDependencyModules.first { it.isNativeStdlib() }
        val otherModules = (moduleDescriptors - stdlib).toSet()

        val stdlibHeaderGenerator = ObjCExporModulesIndexerImpl(
                context, listOf(stdlib), mapper, eventQueue
        )
        return otherModules.map {
            ObjCExporModulesIndexerImpl(
                    context, listOf(it), mapper, eventQueue
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
            val problemCollector = ProblemCollector(context)
            objcModuleIndexers.forEach { builder ->
                builder.indexModule()
            }

            // We have to split these two loops because module translation might affect API of each module.
            val translators = translationsConfigurations.toList().map {
                it.createProducer(problemCollector)
            }

            // Smells bad.
            eventQueue.drain(translators)

            translators.map { it.buildInterface() }
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


        val codespec = codeSpecs.values.reduce { acc, spec -> acc.merge(spec) }
        val exportedInterface = codeSpecs.keys.first { it.containsStdlib }
        mainNamer = exportedInterface.namer

        ObjCExportCodeGenerator(codegen, exportedInterface.namer, exportedInterface.mapper).use { objCCodeGenerator ->
            exportedInterface.generateWorkaroundForSwiftSR10177()
            objCCodeGenerator.generate(codespec)
        }
    }

    /**
     * Populate framework directory with headers, module and info.plist.
     */
    fun produceFrameworkInterface() {
        val outputs = context.config.outputFiles as FrameworkOutputs
        exportedInterfaces.forEach { exportedInterface ->
            val frameworkDirectory = outputs.directoryForFramework(exportedInterface.frameworkName)
            val properties = context.config.platform.configurables as AppleConfigurables
            // TODO: per-module.
            val mainPackageGuesser = MainPackageGuesser(
                    context.moduleDescriptor,
                    context.getIncludedLibraryDescriptors(),
                    context.getExportedDependencies()
            )
            val infoPListBuilder = InfoPListBuilder(target, properties, context.configuration, mainPackageGuesser)
            val moduleMapBuilder = ModuleMapBuilder(
                    exportedInterface.frameworkName,
                    exportedInterface.clangModule.moduleDependencies,
            )
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

private class ProblemCollector(val context: Context) : ObjCExportProblemCollector {
    override fun reportWarning(text: String) {
        context.reportCompilationWarning(text)
    }

    override fun reportWarning(method: FunctionDescriptor, text: String) {
        val psi = (method as? DeclarationDescriptorWithSource)?.source?.getPsi()
                ?: return reportWarning(
                        "$text\n    (at ${DescriptorRenderer.COMPACT_WITH_SHORT_TYPES.render(method)})"
                )

        val location = MessageUtil.psiElementToMessageLocation(psi)

        context.messageCollector.report(CompilerMessageSeverity.WARNING, text, location)
    }

    override fun reportException(throwable: Throwable) {
        throw throwable
    }
}

