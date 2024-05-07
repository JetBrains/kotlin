/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.utilities

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.phaser.*
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.ir.IrElement


/**
 * Adapter interface that can be implemented to enabled to access
 * IR facilities during phase pre- and postprocessing.
 */
interface BackendContextHolder<C : CommonBackendContext> {
    val backendContext: C
}

/**
 * Adapter interface that can be implemented by phase context, input or output
 * to enable IR validation, dumping and possibly other pre- and postprocessing.
 */
interface KotlinBackendIrHolder {
    val kotlinIr: IrElement
}

private fun <Context : PhaseContext> findBackendContext(context: Context): CommonBackendContext? = when {
    context is CommonBackendContext -> context
    context is BackendContextHolder<*> -> context.backendContext
    else -> null
}

private fun <Context : PhaseContext, Data> findKotlinBackendIr(context: Context, data: Data): IrElement? = when {
    data is IrElement -> data
    data is KotlinBackendIrHolder -> data.kotlinIr
    context is KotlinBackendIrHolder -> context.kotlinIr
    else -> null
}

private fun <Context : PhaseContext, Data> getIrValidator(): Action<Data, Context> =
        fun(state: ActionState, data: Data, context: Context) {
            if (!state.isValidationNeeded()) return

            val backendContext: CommonBackendContext? = findBackendContext(context)
            if (backendContext == null) {
                context.messageCollector.report(CompilerMessageSeverity.LOGGING,
                        "Cannot verify IR ${state.beforeOrAfter} ${state.phase}: insufficient context.")
                return
            }
            val element = findKotlinBackendIr(context, data)
            if (element == null) {
                context.messageCollector.report(CompilerMessageSeverity.LOGGING,
                        "Cannot verify IR ${state.beforeOrAfter} ${state.phase}: IR not found.")
                return
            }
            validationCallback(backendContext, element, checkTypes = true)
        }

private fun <Data, Context : PhaseContext> getIrDumper(): Action<Data, Context> =
        fun(state: ActionState, data: Data, context: Context) {
            if (!state.isDumpNeeded()) return
            val backendContext: CommonBackendContext? = findBackendContext(context)
            if (backendContext == null) {
                context.messageCollector.report(CompilerMessageSeverity.WARNING,
                        "Cannot dump IR ${state.beforeOrAfter} ${state.phase}: insufficient context.")
                return
            }
            val element = findKotlinBackendIr(context, data)
            if (element == null) {
                context.messageCollector.report(CompilerMessageSeverity.WARNING,
                        "Cannot dump IR ${state.beforeOrAfter} ${state.phase}: IR not found.")
                return
            }
            defaultDumper(state, element, backendContext)
        }

/**
 * IR dump and verify actions.
 */
internal fun <Data, Context : PhaseContext> getDefaultIrActions(): Set<Action<Data, Context>> = setOfNotNull(
        getIrDumper(),
        getIrValidator<Context, Data>()
)