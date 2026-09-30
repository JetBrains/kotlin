/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.impl

import kotlin.script.experimental.api.ReplScriptCompilationConfigurationKeys
import kotlin.script.experimental.util.PropertiesCollection

/**
 * The number of the snippet being compiled. A host may set it; `K2ReplCompiler` always stores the effective value
 * in the refined configuration, where the FIR configurators read it.
 */
val ReplScriptCompilationConfigurationKeys.currentSnippetNo by PropertiesCollection.key<Int>(isTransient = true)

/**
 * Marks the REPL snippet variant of a definition, matching `*.repl.<extension>`.
 * Set only by the compiler; transient, so a serialized configuration cannot turn another definition into a snippet one.
 */
val ReplScriptCompilationConfigurationKeys.isSnippetDefinition by PropertiesCollection.key<Boolean>(isTransient = true)
