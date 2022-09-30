/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.common.ErrorReportingContext
import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.backend.common.phaser.*
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.backendCodegen
import org.jetbrains.kotlin.backend.konan.driver.context.ConfigChecks
import org.jetbrains.kotlin.backend.konan.getCompilerMessageLocation
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

/**
 * Context is a set of resources that is shared between different phases.
 */
internal interface PhaseContext : LoggingContext, ConfigChecks, ErrorReportingContext {
    val messageCollector: MessageCollector

    /**
     * Called by [PhaseEngine.useContext] after action completion.
     */
    fun dispose()
}

/**
 * Minimal context.
 */
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

    override fun dispose() {

    }
}

/**
 * Engine is used to run compiler phases in a so-called "dynamic" driver,
 * i.e. compiler driver that does not know sequence of events upfront.
 */
internal class PhaseEngine<T : PhaseContext>(
        private val phaseConfig: PhaseConfigService,
        private val phaserState: PhaserState<Any>,
        val context: T
) {
    companion object {
        fun startTopLevel(config: KonanConfig, body: (PhaseEngine<PhaseContext>) -> Unit) {
            val phaserState = PhaserState<Any>()
            val phaseConfig = config.dumbPhaseConfig
            val context = BasicPhaseContext(config)
            // TODO: Get rid of when transition to the dynamic driver complete.
            phaseConfig.konanPhasesConfig(config)
            val topLevelPhase = object : SimpleNamedCompilerPhase<PhaseContext, Any, Unit>(
                    "Compiler",
                    "The whole compilation process",
            ) {
                override fun phaseBody(context: PhaseContext, input: Any) {
                    val engine = PhaseEngine(phaseConfig, phaserState, context)
                    body(engine)
                }

                override fun outputIfNotEnabled(context: PhaseContext, input: Any) {
                    error("Compiler was disabled")
                }
            }
            topLevelPhase.invoke(phaseConfig, phaserState, context, Unit)
        }
    }

    /**
     * Switch to a more specific phase engine.
     */
    inline fun <T : PhaseContext, R> useContext(newContext: T, action: (PhaseEngine<T>) -> R): R {
        val newEngine = PhaseEngine(phaseConfig, phaserState, newContext)
        try {
            return action(newEngine)
        } finally {
            newContext.dispose()
        }
    }

    fun <C : PhaseContext, Input, Output> runPhase(
            context: C,
            phase: NamedCompilerPhase<C, Input, Output>,
            input: Input
    ): Output {
        // TODO: sticky postconditions
        return phase.invoke(phaseConfig, phaserState.changeType(), context, input)
    }
}

internal fun PhaseEngine<Context>.runBackendCodegen(
        irModule: IrModuleFragment,
): IrModuleFragment {
    return this.runPhase(context, backendCodegen, irModule)
}