/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost

import java.io.File
import kotlin.script.experimental.api.CompiledScript
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.jvm.CompiledJvmScriptsCache
import kotlin.script.experimental.jvm.impl.KJvmCompiledScript

open class CompiledScriptJarsCache(val scriptToFile: (SourceCode, ScriptCompilationConfiguration) -> File?) :
    CompiledJvmScriptsCache {

    override fun get(script: SourceCode, scriptCompilationConfiguration: ScriptCompilationConfiguration): CompiledScript? {
        val file = scriptToFile(script, scriptCompilationConfiguration)
            ?: throw IllegalArgumentException("Unable to find a mapping to a file for the script $script")

        if (!file.exists()) return null

        return file.loadScriptFromJar() ?: run {
            // invalidate cache if the script cannot be loaded
            file.delete()
            null
        }
    }

    override fun store(
        compiledScript: CompiledScript,
        script: SourceCode,
        scriptCompilationConfiguration: ScriptCompilationConfiguration
    ) {
        val file = scriptToFile(script, scriptCompilationConfiguration)
            ?: throw IllegalArgumentException("Unable to find a mapping to a file for the script $script")

        val jvmScript = (compiledScript as? KJvmCompiledScript)
            ?: throw IllegalArgumentException("Unsupported script type ${compiledScript::class.java.name}")

        jvmScript.saveToJar(file)
    }
}

