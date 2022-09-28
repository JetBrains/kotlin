/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.common.ErrorReportingContext
import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.backend.common.phaser.NamedCompilerPhase
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.common.phaser.PhaserState
import org.jetbrains.kotlin.backend.common.phaser.changeType
import org.jetbrains.kotlin.backend.konan.KonanConfig
import org.jetbrains.kotlin.backend.konan.driver.context.ConfigChecks
import org.jetbrains.kotlin.backend.konan.getCompilerMessageLocation
import org.jetbrains.kotlin.backend.konan.serialization.KonanIdSignaturer
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerDesc
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.util.SymbolTable

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
        private val phaseConfig: PhaseConfig
) {
    private val phaserState = PhaserState<Any>()

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
        return this.runPhase(context, PsiToIrPhase, psiToIrInput)
    }

    fun runSerializer(
            config: KonanConfig,
            moduleDescriptor: ModuleDescriptor,
            psiToIrResult: PsiToIrResult.ForLibrary,
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