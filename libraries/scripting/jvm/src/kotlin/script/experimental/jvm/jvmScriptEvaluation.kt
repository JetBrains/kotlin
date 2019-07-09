/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package kotlin.script.experimental.jvm

import kotlin.reflect.KClass
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.util.PropertiesCollection

interface JvmScriptEvaluationConfigurationKeys

open class JvmScriptEvaluationConfigurationBuilder : PropertiesCollection.Builder(), JvmScriptEvaluationConfigurationKeys {

    companion object : JvmScriptEvaluationConfigurationBuilder()
}

/**
 * The base classloader to use for script classes loading
 */
val JvmScriptEvaluationConfigurationKeys.baseClassLoader by PropertiesCollection.key<ClassLoader?> {
    get(ScriptEvaluationConfiguration.hostConfiguration)?.get(ScriptingHostConfiguration.jvm.baseClassLoader)
        ?: Thread.currentThread().contextClassLoader
}

/**
 * Load script dependencies before evaluation, true by default
 * If false, it is assumed that the all dependencies will be provided via baseClassLoader
 */
val JvmScriptEvaluationConfigurationKeys.loadDependencies by PropertiesCollection.key<Boolean>(true)

/**
 * Arguments of the main call, if script is executed via its main method
 */
val JvmScriptEvaluationConfigurationKeys.mainArguments by PropertiesCollection.key<Array<out String>>()

internal val JvmScriptEvaluationConfigurationKeys.actualClassLoader by PropertiesCollection.key<ClassLoader?>()

internal val JvmScriptEvaluationConfigurationKeys.scriptsInstancesSharingMap by PropertiesCollection.key<MutableMap<KClass<*>, EvaluationResult>>()

val ScriptEvaluationConfigurationKeys.jvm get() = JvmScriptEvaluationConfigurationBuilder()
