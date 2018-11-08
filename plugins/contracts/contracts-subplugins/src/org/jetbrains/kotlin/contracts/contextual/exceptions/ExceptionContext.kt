/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.contextual.exceptions

import org.jetbrains.kotlin.contracts.contextual.cfg.ContextContracts
import org.jetbrains.kotlin.contracts.contextual.model.Context
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.types.KotlinType

internal data class ExceptionContext(val cachedExceptions: Set<KotlinType> = setOf()) : Context {
    override val family = ExceptionFamily

    override fun reportRemaining(sink: DiagnosticSink, declaredContracts: ContextContracts) {}
}