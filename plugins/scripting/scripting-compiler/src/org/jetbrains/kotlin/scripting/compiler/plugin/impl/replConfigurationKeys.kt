/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.impl

import org.jetbrains.kotlin.cli.common.repl.LineId
import kotlin.script.experimental.api.ReplScriptCompilationConfigurationKeys
import kotlin.script.experimental.util.PropertiesCollection

/**
 * The id of the snippet being compiled.
 *
 * Hosts (e.g. JSR-223) may set it on the per-snippet compilation configuration; `K2ReplCompiler` always writes the
 * effective value into the refined configuration of the snippet, so that FIR-side configurators (which are parser-agnostic
 * and never see the snippet's PSI/`KtScript` user data) can derive the result field name (`<resultFieldPrefix><no>`) from it.
 */
val ReplScriptCompilationConfigurationKeys.currentLineId by PropertiesCollection.key<LineId>(isTransient = true)
