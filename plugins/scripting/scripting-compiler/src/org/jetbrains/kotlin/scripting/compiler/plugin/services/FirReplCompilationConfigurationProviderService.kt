/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.services

import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import kotlin.script.experimental.api.ScriptCompilationConfiguration

class FirReplCompilationConfigurationProviderService(session: FirSession) : FirExtensionSessionComponent(session) {
    @set:FirImplementationDetail
    lateinit var scriptConfiguration: ScriptCompilationConfiguration
}

val FirSession.replCompilationConfigurationProviderService: FirReplCompilationConfigurationProviderService by FirSession.sessionComponentAccessor()
