/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.contextual.dslmarker

import org.jetbrains.kotlin.contracts.contextual.cfg.ContextContracts
import org.jetbrains.kotlin.contracts.contextual.model.Context
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue

internal class DslMarkerContext(val receivers: Set<ReceiverValue> = setOf()) : Context {
    override val family = DslMarkerFamily

    override fun reportRemaining(sink: DiagnosticSink, declaredContracts: ContextContracts) {}
}