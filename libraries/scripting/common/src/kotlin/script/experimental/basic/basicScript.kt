/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.basic

import kotlin.reflect.full.primaryConstructor
import kotlin.script.experimental.annotations.KotlinScriptDefaultCompilationConfiguration
import kotlin.script.experimental.api.*
import kotlin.script.experimental.util.TypedKey


private const val ILLEGAL_CONFIG_ANN_ARG =
    "Illegal argument to KotlinScriptDefaultCompilationConfiguration annotation: expecting List-derived object or default-constructed class of configuration parameters"

open class AnnotationsBasedCompilationConfigurator(val environment: ScriptingEnvironment) : ScriptCompilationConfigurator {

    override val defaultConfiguration = run {
        val base = environment[ScriptingEnvironmentProperties.baseClass]
        val cfg = base.annotations.filterIsInstance(KotlinScriptDefaultCompilationConfiguration::class.java).flatMap {
            val params = try {
                it.compilationConfiguration.objectInstance ?: it.compilationConfiguration.primaryConstructor!!.callBy(emptyMap())
            } catch (e: Throwable) {
                throw IllegalArgumentException(ILLEGAL_CONFIG_ANN_ARG, e)
            }
            params.forEach {
                if (it !is Pair<*, *> || it.first !is TypedKey<*>)
                    throw IllegalArgumentException("$ILLEGAL_CONFIG_ANN_ARG: invalid parameter $it")
            }
            params as List<Pair<TypedKey<*>, Any?>>
        }
        ScriptCompileConfiguration(environment, cfg)
    }
}

class DummyEvaluator<ScriptBase : Any>(val environment: ScriptingEnvironment) : ScriptEvaluator<ScriptBase> {
    override suspend fun eval(
        compiledScript: CompiledScript<ScriptBase>,
        scriptEvaluationEnvironment: ScriptEvaluationEnvironment
    ): ResultWithDiagnostics<EvaluationResult> =
        ResultWithDiagnostics.Failure("not implemented".asErrorDiagnostics())
}

