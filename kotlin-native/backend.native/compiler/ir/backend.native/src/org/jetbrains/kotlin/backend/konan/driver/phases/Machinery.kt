/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.common.ErrorReportingContext
import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.backend.common.phaser.*
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.driver.context.ConfigChecks
import org.jetbrains.kotlin.backend.konan.getCompilerMessageLocation
import org.jetbrains.kotlin.backend.konan.llvm.Llvm
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportCodeSpec
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportedInterface
import org.jetbrains.kotlin.backend.konan.serialization.KonanIdSignaturer
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerDesc
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.konan.TempFiles
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.resolve.CleanableBindingContext

// TODO: What is the difference between input and context?
//  Don't have a good answer yet.
internal interface PhaseContext : LoggingContext, ConfigChecks, ErrorReportingContext {
    val messageCollector: MessageCollector
}

internal open class BasicPhaseContext(
        override val config: KonanConfig,
) : PhaseContext {
    override var inVerbosePhase = false
    override fun log(message: () -> String) {
        if (inVerbosePhase) {
            println(message())
        }
    }

    override val messageCollector: MessageCollector
        get() = config.configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)

    override fun report(element: IrElement?, irFile: IrFile?, message: String, isError: Boolean) {
        val location = element?.getCompilerMessageLocation(irFile ?: error("irFile should be not null for $element"))
        this.messageCollector.report(
                if (isError) CompilerMessageSeverity.ERROR else CompilerMessageSeverity.WARNING,
                message, location
        )
    }
}



internal class PhaseEngine(
        private val phaseConfig: PhaseConfigService,
        private val phaserState: PhaserState<Any>,
) {
    companion object {
        fun startTopLevel(config: KonanConfig, body: (PhaseEngine) -> Unit) {
            val phaserState = PhaserState<Any>()
            val phaseConfig = config.dumbPhaseConfig
            val topLevelPhase = object: SimpleNamedCompilerPhase<PhaseContext, Any, Unit>(
                    "Compiler",
                    "The whole compilation process",
            ) {
                override fun phaseBody(context: PhaseContext, input: Any) {
                    val engine = PhaseEngine(phaseConfig, phaserState)
                    body(engine)
                }

                override fun outputIfNotEnabled(context: PhaseContext, input: Any) {
                    error("Compiler was disabled")
                }
            }
            topLevelPhase.invoke(phaseConfig, phaserState, BasicPhaseContext(config), Unit)
        }
    }

    internal fun <C : PhaseContext, Input, Output> runPhase(
            context: C,
            phase: NamedCompilerPhase<C, Input, Output>,
            input: Input
    ): Output {
        // TODO: sticky postconditions
        return phase.invoke(phaseConfig, phaserState.changeType(), context, input)
    }

    fun runFrontend(config: KonanConfig, environment: KotlinCoreEnvironment): FrontendPhaseResult {
        val frontendContext = FrontendContext(config)
        return this.runPhase(frontendContext, FrontendPhase, environment)
    }

    fun runPsiToIr(
            config: KonanConfig,
            frontendResult: FrontendPhaseResult.Full,
            symbolTable: SymbolTable,
            isProducingLibrary: Boolean
    ): PsiToIrResult {
        val psiToIrInput = PsiToIrInput(frontendResult, symbolTable, isProducingLibrary)
        val context = PsiToContextImpl(config, frontendResult.moduleDescriptor)
        val result = this.runPhase(context, PsiToIrPhase, psiToIrInput)
        val originalBindingContext = frontendResult.bindingContext as? CleanableBindingContext
                ?: error("BindingContext should be cleanable in K/N IR to avoid leaking memory: ${frontendResult.bindingContext}")
        originalBindingContext.clear()
        return result
    }

    fun runSerializer(
            config: KonanConfig,
            moduleDescriptor: ModuleDescriptor,
            psiToIrResult: PsiToIrResult.Full,
    ): SerializerResult {
        val context = BasicPhaseContext(config)
        val input = SerializerInput(moduleDescriptor, psiToIrResult.irModule, psiToIrResult.expectDescriptorToSymbol)
        return this.runPhase(context, SerializerPhase, input)
    }

    fun writeKlib(
            config: KonanConfig,
            serializationResult: SerializerResult,
    ) {
        this.runPhase(BasicPhaseContext(config), WriteKlibPhase, serializationResult)
    }

    fun produceObjCExportInterface(
            config: KonanConfig,
            frontendResult: FrontendPhaseResult.Full,
    ): ObjCExportedInterface {
        return this.runPhase(BasicPhaseContext(config), ProduceObjCInterfacePhase, frontendResult)
    }

    fun produceObjCCodeSpec(
            config: KonanConfig,
            objCExportedInterface: ObjCExportedInterface,
            symbolTable: SymbolTable,
    ): ObjCExportCodeSpec {
        val input = ObjCCodeSpecInput(symbolTable, objCExportedInterface)
        return this.runPhase(BasicPhaseContext(config), ProduceObjCCodeSpecPhase, input)
    }

    fun writeObjCFramework(
            config: KonanConfig,
            objCExportedInterface: ObjCExportedInterface,
            moduleDescriptor: ModuleDescriptor,
            frameworkFile: File,
    ) {
        val input = WriteObjCFrameworkInput(objCExportedInterface, moduleDescriptor, frameworkFile)
        return this.runPhase(BasicPhaseContext(config), WriteObjCFramework, input)
    }

    fun runBackendCodegen(
            context: Context,
            irModule: IrModuleFragment,
    ): IrModuleFragment {
        return this.runPhase(context, backendCodegen, irModule)
    }

    fun produceObjectFiles(
            config: KonanConfig,
            bitcodeFile: BitcodeFile,
            tempFiles: TempFiles,
    ): List<ObjectFile> {
        val input = ObjectFilesInput(bitcodeFile, tempFiles)
        return this.runPhase(BasicPhaseContext(config), ObjectFilesPhase, input)
    }

    fun linkObjectFiles(
            config: KonanConfig,
            objectFiles: List<ObjectFile>,
            llvm: Llvm,
            llvmModuleSpecification: LlvmModuleSpecification,
            needsProfileLibrary: Boolean,
            outputFile: String,
            outputFiles: OutputFiles,
            tempFiles: TempFiles,
    ) {
        val input = LinkerPhaseInput(objectFiles, llvm, llvmModuleSpecification, needsProfileLibrary, outputFile, outputFiles, tempFiles)
        return this.runPhase(BasicPhaseContext(config), LinkerPhase, input)
    }
}

interface Resource<T> {
    val value: T

    fun close()
}

inline fun <T, R> Resource<T>.use(block: (T) -> R): R {
    try {
        return block(value)
    } finally {
        close()
    }
}

class SymbolTableResource : Resource<SymbolTable> {
    override val value: SymbolTable by lazy {
        SymbolTable(KonanIdSignaturer(KonanManglerDesc), IrFactoryImpl)
    }

    override fun close() {
        // TODO: Invalidate
    }
}