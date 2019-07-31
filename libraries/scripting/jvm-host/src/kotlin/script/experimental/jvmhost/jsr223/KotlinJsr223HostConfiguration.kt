/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.jsr223

import javax.script.ScriptContext
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.ScriptingHostConfigurationKeys
import kotlin.script.experimental.util.PropertiesCollection

interface Jsr223HostConfigurationKeys

open class Jsr223HostConfigurationBuilder : PropertiesCollection.Builder(),
    Jsr223HostConfigurationKeys {
    companion object : Jsr223HostConfigurationBuilder()
}

val ScriptingHostConfigurationKeys.jsr223 get() = Jsr223HostConfigurationBuilder()

val Jsr223HostConfigurationKeys.getScriptContext by PropertiesCollection.key<() -> ScriptContext?>()


interface Jsr223CompilationConfigurationKeys

open class Jsr223CompilationConfigurationBuilder : PropertiesCollection.Builder(),
    Jsr223CompilationConfigurationKeys {
    companion object : Jsr223CompilationConfigurationBuilder()
}

val ScriptCompilationConfigurationKeys.jsr223 get() = Jsr223CompilationConfigurationBuilder()

val Jsr223CompilationConfigurationKeys.getScriptContext by PropertiesCollection.key<() -> ScriptContext?>(
    {
        get(ScriptCompilationConfiguration.hostConfiguration)?.get(ScriptingHostConfiguration.jsr223.getScriptContext)
    },
    isTransient = true
)

val Jsr223CompilationConfigurationKeys.importAllBindings by PropertiesCollection.key<Boolean>(false)

interface Jsr223EvaluationConfigurationKeys

open class Jsr223EvaluationConfigurationBuilder : PropertiesCollection.Builder(),
    Jsr223EvaluationConfigurationKeys {
    companion object : Jsr223EvaluationConfigurationBuilder()
}

val ScriptEvaluationConfigurationKeys.jsr223 get() = Jsr223EvaluationConfigurationBuilder()

val Jsr223EvaluationConfigurationKeys.getScriptContext by PropertiesCollection.key<() -> ScriptContext?>(
    {
        get(ScriptEvaluationConfiguration.hostConfiguration)?.get(ScriptingHostConfiguration.jsr223.getScriptContext)
    },
    isTransient = true
)


