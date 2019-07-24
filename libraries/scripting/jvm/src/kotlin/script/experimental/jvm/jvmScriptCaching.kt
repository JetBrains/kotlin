/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvm

import kotlin.script.experimental.api.CompiledScript
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.util.PropertiesCollection

interface CompiledJvmScriptsCache {
    fun get(script: SourceCode, scriptCompilationConfiguration: ScriptCompilationConfiguration): CompiledScript<*>?
    fun store(compiledScript: CompiledScript<*>, script: SourceCode, scriptCompilationConfiguration: ScriptCompilationConfiguration)

    object NoCache : CompiledJvmScriptsCache {
        override fun get(
            script: SourceCode, scriptCompilationConfiguration: ScriptCompilationConfiguration
        ): CompiledScript<*>? = null

        override fun store(
            compiledScript: CompiledScript<*>, script: SourceCode, scriptCompilationConfiguration: ScriptCompilationConfiguration
        ) {
        }
    }
}

val JvmScriptingHostConfigurationKeys.compilationCache by PropertiesCollection.key<CompiledJvmScriptsCache>(isTransient = true)

