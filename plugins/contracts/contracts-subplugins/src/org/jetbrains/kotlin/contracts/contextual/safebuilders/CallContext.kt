/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.contextual.safebuilders

import org.jetbrains.kotlin.contracts.contextual.cfg.ContextContracts
import org.jetbrains.kotlin.contracts.contextual.common.CallInfo
import org.jetbrains.kotlin.contracts.contextual.model.Context
import org.jetbrains.kotlin.contracts.contextual.util.FunctionAndThisInstanceHolder
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.diagnostics.Errors

internal data class CallContext(val calls: Map<FunctionAndThisInstanceHolder, CallInfo> = mapOf()) : Context {
    override val family = CallFamily

    override fun reportRemaining(sink: DiagnosticSink, declaredContracts: ContextContracts) {
        for ((instanceHolder, info) in calls) {
            val (sourceElement, kind) = info
            val message = "$instanceHolder had invoked $kind"
            sink.report(Errors.CONTEXTUAL_EFFECT_WARNING.on(sourceElement, message))
        }
    }
}