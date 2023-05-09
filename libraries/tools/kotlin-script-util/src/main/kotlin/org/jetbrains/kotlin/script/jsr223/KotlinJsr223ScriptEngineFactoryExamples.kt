/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("unused", "DEPRECATION") // could be used externally in javax.script.ScriptEngineFactory META-INF file

package org.jetbrains.kotlin.script.jsr223

import org.jetbrains.kotlin.cli.common.repl.KotlinJsr223JvmScriptEngineFactoryBase
import org.jetbrains.kotlin.cli.common.repl.ScriptArgsWithTypes
import javax.script.Bindings
import javax.script.ScriptContext
import javax.script.ScriptEngine
import kotlin.script.experimental.jvm.util.KotlinJars
import kotlin.script.experimental.jvm.util.scriptCompilationClasspathFromContext

@Deprecated("Use kotlin-scripting-jsr223 instead")
class KotlinJsr223JvmLocalScriptEngineFactory : KotlinJsr223JvmScriptEngineFactoryBase() {

    override fun getScriptEngine(): ScriptEngine =
        KotlinJsr223JvmLocalScriptEngine(
            this,
            scriptCompilationClasspathFromContext("kotlin-script-util.jar", wholeClasspath = true),
            KotlinStandardJsr223ScriptTemplate::class.qualifiedName!!,
            { ctx, types -> ScriptArgsWithTypes(arrayOf(ctx.getBindings(ScriptContext.ENGINE_SCOPE)), types ?: emptyArray()) },
            arrayOf(Bindings::class)
        )
}

@Deprecated("Use kotlin-scripting-jsr223 instead")
class KotlinJsr223JvmDaemonLocalEvalScriptEngineFactory : KotlinJsr223JvmScriptEngineFactoryBase() {

    override fun getScriptEngine(): ScriptEngine =
        KotlinJsr223JvmDaemonCompileScriptEngine(
            this,
            KotlinJars.compilerWithScriptingClasspath,
            scriptCompilationClasspathFromContext("kotlin-script-util.jar", wholeClasspath = true),
            KotlinStandardJsr223ScriptTemplate::class.qualifiedName!!,
            { ctx, types -> ScriptArgsWithTypes(arrayOf(ctx.getBindings(ScriptContext.ENGINE_SCOPE)), types ?: emptyArray()) },
            arrayOf(Bindings::class)
        )
}

