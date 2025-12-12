/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.fir

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.host.ScriptingHostConfiguration

open class FirScriptCompilationComponent(
    val hostConfiguration: ScriptingHostConfiguration,
    val getSessionForAnnotationResolution: (SourceCode, ScriptCompilationConfiguration) -> FirSession? = { _, _ -> null }
) : FirSessionComponent

val FirSession.scriptCompilationComponent: FirScriptCompilationComponent? by FirSession.nullableSessionComponentAccessor()
