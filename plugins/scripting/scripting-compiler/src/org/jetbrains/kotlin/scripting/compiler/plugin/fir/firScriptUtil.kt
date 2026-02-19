/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.fir

import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataKey
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataRegistry
import org.jetbrains.kotlin.fir.declarations.FirScript
import kotlin.script.experimental.api.ScriptCompilationConfiguration

private object ScriptCompilationConfigurationKey : FirDeclarationDataKey()

var FirScript.scriptCompilationConfiguration: ScriptCompilationConfiguration? by FirDeclarationDataRegistry.data(ScriptCompilationConfigurationKey)
