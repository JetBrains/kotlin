/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvm.jsr223

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

/**
 * Checks whether a class that is public in the bytecode is also public as declared in Kotlin, i.e. it is not, e.g., a Kotlin
 * `internal` class. Used to choose the type under which a JSR-223 binding value is exposed to snippets (see
 * [Jsr223CompilationConfigurationKeys.importAllBindings]): a class that is not public can't be referenced from a snippet,
 * so its nearest public supertype is used instead. The check should return true for non-Kotlin classes, and false if the
 * visibility can't be determined.
 *
 * The default implementation uses [kotlin.reflect.KClass.visibility] if `kotlin-reflect` is available at runtime, and
 * otherwise conservatively treats every Kotlin class as not public. A host without `kotlin-reflect` can provide a more
 * precise implementation, e.g. one reading the Kotlin metadata with the compiler's code.
 */
val Jsr223HostConfigurationKeys.hasPublicKotlinVisibility by PropertiesCollection.key<(Class<*>) -> Boolean>(
    { ::hasPublicKotlinVisibilityViaReflection },
    isTransient = true
)


interface Jsr223CompilationConfigurationKeys

open class Jsr223CompilationConfigurationBuilder : PropertiesCollection.Builder(),
    Jsr223CompilationConfigurationKeys {
    companion object : Jsr223CompilationConfigurationBuilder()
}

@Suppress("UnusedReceiverParameter")
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

@Suppress("UnusedReceiverParameter")
val ScriptEvaluationConfigurationKeys.jsr223 get() = Jsr223EvaluationConfigurationBuilder()

val Jsr223EvaluationConfigurationKeys.getScriptContext by PropertiesCollection.key<() -> ScriptContext?>(
    {
        get(ScriptEvaluationConfiguration.hostConfiguration)?.get(ScriptingHostConfiguration.jsr223.getScriptContext)
    },
    isTransient = true
)
