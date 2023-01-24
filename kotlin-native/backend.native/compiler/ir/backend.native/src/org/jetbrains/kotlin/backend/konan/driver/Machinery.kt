/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.ErrorReportingContext
import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.backend.common.phaser.*
import org.jetbrains.kotlin.backend.konan.ConfigChecks
import org.jetbrains.kotlin.backend.konan.KonanConfig
import org.jetbrains.kotlin.backend.konan.getCompilerMessageLocation
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile

/**
 * Context is a set of resources that is shared between different phases. PhaseContext is a "minimal context",
 * effectively just a wrapper around [KonanConfig]. Still, it is more than enough in many cases.
 *
 * There is a fuzzy line between phase Input/Output and Context. We can consider them as a spectre:
 * * On the one end there is a [org.jetbrains.kotlin.backend.konan.Context] (circa 1.8.0). It has a lot of properties,
 * some even lateinit, which makes this object hard to construct and phases that depend on it are tightly coupled.
 * But we don't need to pass data between phases explicitly, which makes code easier to write.
 * * One the other end we can pass everything explicitly via I/O types. It will decouple code at the cost of boilerplate.
 *
 * So we have to find a point on this spectre for each phase.
 * We still don't have a rule of thumb for deciding whether object should be a part of context or not.
 * Some notes:
 * * Lifetime of context should be as small as possible: it reduces memory usage and forces a clean architecture.
 * * Frontend and backend are not really tied to IR and its friends, so we can pass more bytes via I/O.
 * * On the other hand, middle- and bitcode phases are hard to decouple due to the way the code was written many years ago.
 * It will take some time to rewrite it properly.
 */
internal interface PhaseContext : LoggingContext, ConfigChecks, ErrorReportingContext {
    val messageCollector: MessageCollector

    /**
     * Called by [PhaseEngine.useContext] after action completion to cleanup resources.
     */
    fun dispose()
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

    override fun dispose() {

    }
}

/**
 * PhaseEngine is a heart of dynamic compiler driver. Unlike old static compiler driver that relies on predefined list of phases,
 * dynamic one requires user to write a sequence of phases by hand (thus "dynamic"). PhaseEngine provides a framework for that by tracking
 * phase configuration and state under the hood and exposing two methods:
 * * [runPhase], well, executes a given phase.
 * * [useContext] creates a child engine with a more specific [PhaseContext] that will be cleanup at the end of the call.
 * This way, PhaseEngine forces user to create more specialized contexts that have a limited lifetime.
 */
internal class PhaseEngine<C : PhaseContext>(
        val phaseConfig: PhaseConfigurationService,
        val phaserState: PhaserState<Any>,
        val context: C
) {
    companion object {
        fun startTopLevel(config: KonanConfig, body: (PhaseEngine<PhaseContext>) -> Unit) {
            val phaserState = PhaserState<Any>()
            val phaseConfig = config.flexiblePhaseConfig
            val context = BasicPhaseContext(config)
            val topLevelPhase = object : SimpleNamedCompilerPhase<PhaseContext, Any, Unit>(
                    "Compiler",
                    "The whole compilation process",
            ) {
                override fun phaseBody(context: PhaseContext, input: Any) {
                    val engine = PhaseEngine(phaseConfig, phaserState, context)
                    body(engine)
                }

                override fun outputIfNotEnabled(phaseConfig: PhaseConfigurationService, phaserState: PhaserState<Any>, context: PhaseContext, input: Any) {
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

    fun <Input, Output, P : AbstractNamedCompilerPhase<C, Input, Output>> runPhase(
            phase: P,
            input: Input,
            disable: Boolean = false
    ): Output {
        if (disable) {
            return phase.outputIfNotEnabled(phaseConfig, phaserState.changePhaserStateType(), context, input)
        }
        // We lose sticky postconditions here, but it should be ok, since type is changed.
        return phase.invoke(phaseConfig, phaserState.changePhaserStateType(), context, input)
    }


    fun <Output, P : AbstractNamedCompilerPhase<C, Unit, Output>> runPhase(
            phase: P,
    ): Output = runPhase(phase, Unit)
}