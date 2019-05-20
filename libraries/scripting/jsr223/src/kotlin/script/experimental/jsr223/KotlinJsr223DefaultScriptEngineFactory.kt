/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jsr223

import org.jetbrains.kotlin.cli.common.repl.KotlinJsr223JvmScriptEngineFactoryBase
import javax.script.ScriptEngine
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate
import kotlin.script.experimental.jvmhost.createJvmEvaluationConfigurationFromTemplate
import kotlin.script.experimental.jvmhost.jsr223.KotlinJsr223ScriptEngineImpl

class KotlinJsr223DefaultScriptEngineFactory : KotlinJsr223JvmScriptEngineFactoryBase() {

    private val compilationConfiguration = createJvmCompilationConfigurationFromTemplate<KotlinJsr223DefaultScript>()
    private val evaluationConfiguration = createJvmEvaluationConfigurationFromTemplate<KotlinJsr223DefaultScript>()

    override fun getScriptEngine(): ScriptEngine =
        KotlinJsr223ScriptEngineImpl(
            this,
            ScriptCompilationConfiguration(compilationConfiguration) {
                jvm {
                    dependenciesFromCurrentContext(wholeClasspath = true)
                }
            },
            evaluationConfiguration
        )
}

