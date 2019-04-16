/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

// could be used externally in javax.script.ScriptEngineFactory META-INF file

package kotlin.script.experimental.jsr223

import org.jetbrains.kotlin.cli.common.repl.KotlinJsr223JvmScriptEngineFactoryBase
import org.jetbrains.kotlin.cli.common.repl.ScriptArgsWithTypes
import org.jetbrains.kotlin.script.util.*
import javax.script.Bindings
import javax.script.ScriptContext
import javax.script.ScriptEngine

class KotlinJsr223JvmScriptEngineFactory : KotlinJsr223JvmScriptEngineFactoryBase() {

    override fun getScriptEngine(): ScriptEngine =
        KotlinJsr223ScriptEngine(
            this,
            { ctx, types -> ScriptArgsWithTypes(arrayOf(ctx.getBindings(ScriptContext.ENGINE_SCOPE)), types ?: emptyArray()) },
            arrayOf(Bindings::class)
        )
}

