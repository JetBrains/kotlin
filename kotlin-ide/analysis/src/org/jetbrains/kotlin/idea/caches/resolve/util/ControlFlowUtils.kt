/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.resolve.util

import org.jetbrains.kotlin.cfg.ControlFlowInformationProviderImpl
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace
import org.jetbrains.kotlin.resolve.lazy.ResolveSession

fun analyzeControlFlow(resolveSession: ResolveSession, resolveElement: KtElement, trace: BindingTrace) {
    val controlFlowTrace = DelegatingBindingTrace(
        trace.bindingContext, "Element control flow resolve", resolveElement, allowSliceRewrite = true
    )
    ControlFlowInformationProviderImpl(
        resolveElement, controlFlowTrace, resolveElement.languageVersionSettings, resolveSession.platformDiagnosticSuppressor
    ).checkDeclaration()
    controlFlowTrace.addOwnDataTo(trace, null, false)
}